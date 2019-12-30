package ddb.io.voxelnet.network;

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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PSChunkData extends Packet
{
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
	public void decodePayload(ByteBuf data) throws IOException
	{
		// Decode the chunk data
		ByteArrayInputStream input;
		GZIPInputStream inflator;
		
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
		int compressedColumnLen;
		int decompressedCount;
		byte[] colData;
		byte[] deflatedData;
		
		chunkX = data.readInt();
		chunkZ = data.readInt();
		chunkCount = data.readUnsignedByte();
		compressedColumnLen = data.readUnsignedShort();
		colData = new byte[compressedColumnLen];
		
		// Deflate the column
		data.readBytes(colData);
		input = new ByteArrayInputStream(colData);
		inflator = new GZIPInputStream(input);
		
		deflatedData = new byte[ChunkColumn.COLUMNS_SIZE];
		decompressedCount = inflator.read(deflatedData);
		inflator.close();
		
		assert decompressedCount == ChunkColumn.COLUMNS_SIZE : "Mismatch in sizes (" + decompressedCount + " != " + ChunkColumn.COLUMNS_SIZE + ")";
		
		column = new ChunkColumn(chunkX, chunkZ, deflatedData);
		
		// Variable Block:
		// 1 Chunk segment:
		// ChunkY     (4, i)       | BlockCount  (2, us)      | Tick-able Count (2, us)   |
		// Tick-ables (4*size, i)  | CompressedLength (2, us) | CompressedData (1*size,b) |
		
		// Compressed Chunk Data:
		// BlockData[] (1*size, b) | Lighting[] (1*size, b)  | BlockMeta[] (1*size, b) |
		// LayerData[] (2*size, s) |
		
		for (int i = 0; i < chunkCount; i++)
		{
			// Decode chunk data
			int chunkY;
			int blockCount;
			int tickableCount;
			int compressedLen;
			int[] tickables;
			byte[] compressedData;
			
			byte[] blockData;
			byte[] lightData;
			byte[] metaData;
			byte[] layerIntermediate;
			short[] layerData;
			
			Chunk chunk;
			
			chunkY = data.readInt();
			blockCount = data.readUnsignedShort();
			tickableCount = data.readUnsignedShort();
			tickables = new int[tickableCount];
			
			// Load the tickables
			for (int j = 0; j < tickableCount; j++)
				tickables[j] = data.readInt();
			
			compressedLen = data.readUnsignedShort();
			compressedData = new byte[compressedLen];
			
			data.readBytes(compressedData);
			
			// Done working with the ByteBuf, now decoding compressed data
			decompressedCount = 0;
			
			blockData = new byte[Chunk.BLOCK_DATA_SIZE];
			lightData = new byte[Chunk.LIGHT_DATA_SIZE];
			metaData  = new byte[Chunk.META_DATA_SIZE];
			layerIntermediate = new byte[Chunk.LAYER_DATA_SIZE * 2];
			layerData = new short[Chunk.LAYER_DATA_SIZE];
			
			input = new ByteArrayInputStream(compressedData);
			inflator = new GZIPInputStream(input);
			
			decompressedCount += inflator.read(blockData);
			decompressedCount += inflator.read(lightData);
			decompressedCount += inflator.read(metaData);
			decompressedCount += inflator.read(layerIntermediate);
			
			inflator.close();
			
			assert decompressedCount == (Chunk.BLOCK_DATA_SIZE + Chunk.LIGHT_DATA_SIZE + Chunk.META_DATA_SIZE + Chunk.LAYER_DATA_SIZE * 2)
					: "Mismatch in chunk decompression count! (" + chunkY + ")";
			
			// Convert layer data into short array
			ByteBuffer.wrap(layerIntermediate).asShortBuffer().get(layerData);
			
			chunk = new Chunk(null, chunkX, chunkY, chunkZ);
			chunk.deserialize(blockData, lightData, metaData, layerData, tickables, blockCount);
			chunkList.add(chunk);
		}
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
		
		// May throw IOExceptions
		ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
		GZIPOutputStream compressor = new GZIPOutputStream(output);
		compressor.write(column.opaqueColumns);
		
		compressor.flush();
		compressor.finish();
		
		// Chunk column data
		byte[] colData = output.toByteArray();
		data.writeShort(colData.length);
		data.writeBytes(colData);
		output.reset();
		
		ByteBuf layerHolder = data.alloc().buffer(Chunk.LAYER_DATA_SIZE * 2);
		
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
			
			// Compress the other arrays
			compressor = new GZIPOutputStream(output);
			compressor.write(chunk.getData());
			compressor.write(chunk.getLightData());
			compressor.write(chunk.getMetaData());
			
			// Copy the layer data
			for (int i = 0; i < Chunk.LAYER_DATA_SIZE; i++)
				layerHolder.writeShort(chunk.getLayerData()[i]);
			
			// Write out the data
			layerHolder.getBytes(0, compressor, layerHolder.capacity());
			layerHolder.resetWriterIndex();
			layerHolder.resetReaderIndex();
			
			// Submit the data to the array
			compressor.flush();
			compressor.finish();
			
			output.flush();
			byte[] chunkData = output.toByteArray();
			data.writeShort(chunkData.length);
			data.writeBytes(chunkData);
			output.reset();
		}
	}
	
	@Override
	public int getPacketID()
	{
		return 4;
	}
}
