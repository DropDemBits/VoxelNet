package ddb.io.voxelnet.network.packet;

import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.ChunkColumn;
import ddb.io.voxelnet.world.World;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

public class PSChunkData extends Packet
{
	private static final int UNCOMPRESSED_CHUNK_SIZE = Chunk.BLOCK_DATA_SIZE + Chunk.LIGHT_DATA_SIZE + Chunk.META_DATA_SIZE + Chunk.LAYER_DATA_SIZE * 2;
	
	public int chunkX;
	public int chunkZ;
	public ChunkColumn column;
	public final List<Chunk> chunkList = new ArrayList<>();
	
	public PSChunkData() {}
	
	public PSChunkData(int chunkX, int chunkZ, ChunkColumn column)
	{
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
		this.column = column;
	}
	
	/**
	 * Adds a chunk to be set with this packet
	 * Must be within the same chunk column as the packet
	 * @param chunk The chunk to be sent with this packet
	 */
	public void addChunk(Chunk chunk)
	{
		chunkList.add(chunk);
	}
	
	/**
	 * Sets the world for all of the chunks
	 * Must be done before the chunks can be processed
	 * @param world The new world for all of the chunks
	 */
	public void setWorld(World world)
	{
		chunkList.forEach((chunk) -> chunk.world = world);
	}
	
	@Override
	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void decodePayload(ByteBuf data) throws IOException, DataFormatException
	{
		// Decode the chunk data
		//ByteArrayInputStream input;
		//GZIPInputStream inflator;
		
		// ub = unsigned byte
		// us = unsigned short
		// i = int
		// Number is size in bytes
		
		// Header:
		// ChunkX (4, i) | ChunkZ (4, i) | ChunkCount (1, ub) | CompressedLen (2, us) |
		// CompressedData (1*size, b) |
		
		// Compressed Column Data:
		// OpaqueColumns (1*size, b) |
		
		int chunkCount;
		int decompressedSize;
		byte[] compressedColData;
		byte[] deflatedData;
		
		chunkX = data.readInt();
		chunkZ = data.readInt();
		chunkCount = data.readUnsignedByte();
		int compressedColumnLen = data.readUnsignedShort();
		compressedColData = new byte[compressedColumnLen];
		
		// Deflate the column
		data.readBytes(compressedColData);
		ByteArrayInputStream columnInput = new ByteArrayInputStream(compressedColData);
		GZIPInputStream columnInflator = new GZIPInputStream(columnInput);
		
		deflatedData = new byte[ChunkColumn.COLUMNS_SIZE];
		decompressedSize = columnInflator.read(deflatedData);
		columnInflator.close();
		columnInput.close();
		
		assert decompressedSize == ChunkColumn.COLUMNS_SIZE : "Mismatch in sizes (" + decompressedSize + " != " + ChunkColumn.COLUMNS_SIZE + ")";
		
		column = new ChunkColumn(chunkX, chunkZ, deflatedData);
		
		// Variable Block:
		// 1 Chunk segment:
		// ChunkY     (4, i)       | BlockCount  (2, us)      | Tick-able Count (2, us)   |
		// Tick-ables (4*size, i)  | CompressedLength (2, us) | CompressedData (1*size,b) |
		
		// Compressed Chunk Data:
		// BlockData[] (1*size, b) | Lighting[] (1*size, b)  | BlockMeta[] (1*size, b) |
		// LayerData[] (2*size, s) |
		
		// Decompressed chunk data
		final byte[] decompressedData = new byte[UNCOMPRESSED_CHUNK_SIZE];
		
		// "Chunk.deserialize" clones the data, so buffers can be shared
		final byte[] blockData = new byte[Chunk.BLOCK_DATA_SIZE];
		final byte[] lightData  = new byte[Chunk.LIGHT_DATA_SIZE];
		final byte[] metaData= new byte[Chunk.META_DATA_SIZE];
		final byte[] layerIntermediate = new byte[Chunk.LAYER_DATA_SIZE * 2];
		final short[] layerData = new short[Chunk.LAYER_DATA_SIZE];
		
		Inflater chunkInflater = new Inflater();
		ByteArrayInputStream chunkData = new ByteArrayInputStream(decompressedData);
		
		for (int i = 0; i < chunkCount; i++)
		{
			// Decode chunk data
			int chunkY;
			int blockCount;
			int tickableCount;
			int compressedLen;
			int[] tickables;
			byte[] compressedData;
			
			Chunk chunk;
			
			// Read in chunk position & info
			chunkY = data.readInt();
			blockCount = data.readUnsignedShort();
			
			// Setup tickables
			tickableCount = data.readUnsignedShort();
			tickables = new int[tickableCount];
			
			// Load the tickables
			for (int j = 0; j < tickableCount; j++)
				tickables[j] = data.readInt();
			
			// Read in the compressed data
			compressedLen = data.readUnsignedShort();
			compressedData = new byte[compressedLen];
			data.readBytes(compressedData, 0, compressedLen);
			
			// Uncompress the data
			chunkInflater.setInput(compressedData);
			decompressedSize = chunkInflater.inflate(decompressedData);
			chunkInflater.reset();
			
			// Done working with the compressed data, now de-aggregating chunk data
			chunkData.read(blockData);
			chunkData.read(lightData);
			chunkData.read(metaData);
			chunkData.read(layerIntermediate);
			chunkData.reset();
			
			assert decompressedSize == UNCOMPRESSED_CHUNK_SIZE
					: "Mismatch in chunk decompression count! (" + chunkX + "," + chunkY + ", " + chunkZ + ")"
					+ "[" + decompressedSize + " != " + UNCOMPRESSED_CHUNK_SIZE + "]";
			
			// Convert layer data into short array
			ByteBuffer.wrap(layerIntermediate).asShortBuffer().get(layerData);
			
			chunk = new Chunk(null, chunkX, chunkY, chunkZ);
			chunk.deserialize(blockData, lightData, metaData, layerData, tickables, blockCount);
			chunkList.add(chunk);
		}
		
		chunkInflater.end();
		chunkData.close();
	}
	
	@Override
	public void encodePayload(ByteBuf data) throws IOException
	{
		// Encode the chunk data
		// Compress all of the arrays
		
		// ub = unsigned byte
		// us = unsigned short
		// i = int
		// Number is size in bytes
		
		// Header:
		// ChunkX (4, i) | ChunkZ (4, i) | ChunkCount (1, ub) | CompressedLen (2, us) |
		// CompressedData (1*size,b) |
		
		// Compressed Column Data:
		// OpaqueColumns (1*size, b) |
		
		// Variable Block:
		// 1 Chunk segment:
		// ChunkY     (4, i)       | BlockCount  (2, us)      | Tick-able Count (2, us)   |
		// Tick-ables (4*size, i)  | CompressedLength (2, us) | CompressedData (1*size,b) |
		
		// Compressed Chunk Data:
		// BlockData[] (1*size, b) | Lighting[] (1*size, b)  | BlockMeta[] (1*size, b) |
		// LayerData[] (2*size, s) |
		
		data.writeInt(chunkX);
		data.writeInt(chunkZ);
		data.writeByte(chunkList.size());
		
		// Compress the column data
		// May throw IOExceptions
		ByteArrayOutputStream aggregator = new ByteArrayOutputStream(UNCOMPRESSED_CHUNK_SIZE);
		GZIPOutputStream columnCompressor = new GZIPOutputStream(aggregator);
		columnCompressor.write(column.opaqueColumns);
		columnCompressor.flush();
		columnCompressor.close();
		
		// Chunk column data
		byte[] colData = aggregator.toByteArray();
		data.writeShort(colData.length);
		data.writeBytes(colData);
		aggregator.reset();
		
		// Working buffers
		final byte[] layerIntermediate = new byte[Chunk.LAYER_DATA_SIZE * 2];
		final byte[] deflateBuffer = new byte[UNCOMPRESSED_CHUNK_SIZE * 2];
		
		// Compressor
		Deflater chunkCompressor = new Deflater(Deflater.BEST_SPEED);
		
		// Variable block start:
		for (Chunk chunk : chunkList)
		{
			data.writeInt(chunk.chunkY);
			data.writeShort(chunk.getBlockCount());
			
			// Do the tickables
			int tickableCount = chunk.tickables.size();
			data.writeShort(tickableCount);
			for (int tickable : chunk.tickables)
				data.writeInt(tickable);
			
			// Copy the layer data to the holding area
			ByteBuffer.wrap(layerIntermediate).asShortBuffer().put(chunk.getLayerData());
			
			// Aggregate the arrays
			aggregator.write(chunk.getData());
			aggregator.write(chunk.getLightData());
			aggregator.write(chunk.getMetaData());
			aggregator.write(layerIntermediate);
			aggregator.flush();
			byte[] aggregateData = aggregator.toByteArray();
			
			// Compress the chunk data
			chunkCompressor.setInput(aggregateData);
			chunkCompressor.finish();
			int compressSize = chunkCompressor.deflate(deflateBuffer);
			chunkCompressor.reset();
			
			// Add the compressed data to the packet
			data.writeShort(compressSize);
			data.writeBytes(deflateBuffer, 0, compressSize);
			
			// Reset for next chunk
			aggregator.reset();
		}
		
		aggregator.close();
		chunkCompressor.end();
	}
	
	@Override
	public int getPacketID()
	{
		return 4;
	}
}
