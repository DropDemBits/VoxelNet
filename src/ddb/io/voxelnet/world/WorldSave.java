package ddb.io.voxelnet.world;

import ddb.io.voxelnet.util.Vec3i;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Class handling the world saving and loading process
 */
public class WorldSave
{
	// TODO: Use ChunkManager's chunk list instead of World's loaded chunks
	private static final int SAVE_VERSION = 1;
	private static final byte[] SAVE_MAGIC = "VXNT".getBytes();
	private static final int CHUNK_ENTRY_SIZE0 = 4*3 + 2 + 512 + 4096;
	
	// Save Format:
	// cX | cY | cZ | blockCount | blockLayers | blockLighting | blockData | blockMeta  ~ tickablesCount | tickables
	private static final int CHUNK_FIXED_ENTRY_SIZE_V1 = Chunk.FIXED_SIZE;
	private static final int COLUMN_ENTRY_SIZE = 4 * 2 + 256;
	
	// The world to save / load
	private final World world;
	// The file to save / load the world in
	private final String saveFile;
	
	public WorldSave(World world, String saveFile)
	{
		this.world = world;
		this.saveFile = saveFile;
	}
	
	/**
	 * Triggers a world save
	 */
	public void save()
	{
		if (canLoad())
		{
			// Backup the old world if it can be loaded
			System.out.println("Backing up " + saveFile);
			try
			{
				Files.copy(Paths.get(saveFile), Paths.get(saveFile + ".bak"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e)
			{
				System.err.println("Unable to backup old world! Overwriting without a backup copy");
				e.printStackTrace();
			}
		}
		
		System.out.println("Saving world to " + saveFile);
		try (FileOutputStream stream = new FileOutputStream(saveFile))
		{
			// All data is saved as big endian (because network order)
			// Format:
			// Header | ColumnCount | ColumnData | ChunkCount | ChunkData
			// Header: Defines metainfo on the file
			// ColumnCount (int): Number of column save entries
			// ColumnData: Variable length array holding the column data
			// ChunkCount (int): Number of chunk save entries
			// ChunkData: Variable length array holding the chunk data
			
			// Header:
			// magic | saveVersion | worldSeed
			// Magic (char[4]): "VXNT" in bytes
			// saveVersion (int): Save version (initially 0)
			// worldSeed (long): Seed used in world generation
			
			stream.write(SAVE_MAGIC);                          // Magic
			stream.write(serializeInt(SAVE_VERSION));               // Save Version
			stream.write(serializeLong(world.getWorldSeed())); // Seed
			
			///////////////////////////////////////////////////////////////////
			// Write out the column entry count
			int columnEntries = world.chunkManager.chunkColumns.size();
			stream.write(serializeInt(columnEntries)); // Column Entry Count
			
			// Iterate through all of the ChunkColumns
			for (ChunkColumn column : world.chunkManager.chunkColumns.values())
			{
				// Need to save
				// Position (Cx, Cz)
				// Opaque heightmap (opaqueColumns)
				
				// Each column costs 264 bytes to store
				// cX, cZ: 4+4 bytes
				// opaqueColumns: 256 bytes
				
				// Save format
				// cX | cZ | opaqueColumns
				byte[] columnBytes = serializeColumn(column);
				stream.write(columnBytes);
			}
			
			///////////////////////////////////////////////////////////////////
			// Iterate through all of the loaded chunks in the world to
			// determine the save count
			int chunkEntries = 0;
			for (Chunk chunk : world.chunkManager.loadedChunks.values())
			{
				if (!chunk.isEmpty())
					++chunkEntries;
			}
			
			stream.write(serializeInt(chunkEntries)); // Chunk Entry Count
			
			// Iterate through all of the loaded chunks in the world again to
			// actually save them
			for (Chunk chunk : world.chunkManager.loadedChunks.values())
			{
				// Skip empty chunks
				if (chunk.isEmpty())
					continue;
				
				// Need to save
				// Position (Cx, Cy, Cz)
				// Block Lighting (blockLights)
				// Block count (blockCount)
				// Block Data (blockData)
				// blockLayers (blockLayers)
				
				// Don't need to save
				// isEmpty (implied in block count)
				// needsRebuild & isDirty (only used during runtime)
				// recentlyGenerated (only used to generate the chunk's ChunkModel)
				
				// Each chunk costs 4622 Bytes / ~4KiB to store
				// blockData: 4096 bytes
				// blockLighting: 512 bytes
				// chunkX, chunkY, chunkZ: 4+4+4 bytes
				// blockCount: 2 bytes
				
				// Save Format:
				// cX | cY | cZ | blockCount | blockLayers | blockLighting | blockData | blockMeta  ~ tickablesCount | tickables
				// Always stored in big endian
				byte[] chunkBytes = serializeChunk(chunk);
				stream.write(chunkBytes);
			}
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("Finished saving world");
	}
	
	/**
	 * Loads a world from a file
	 * @return True if the world was successfully loaded
	 */
	public boolean load()
	{
		System.out.println("Loading world from " + saveFile);
		// Build the chunk data
		try (FileInputStream fis = new FileInputStream(saveFile))
		{
			// Temporary holder
			byte[] intBytes = new byte[4];
			
			// Skip the header magic
			long skipped = fis.skip(SAVE_MAGIC.length);
			
			// Sanity check
			if (skipped != SAVE_MAGIC.length)
			{
				System.err.println("Error: save file is too small!");
				return false;
			}
			
			// saveVersion (int): World's save version
			fis.read(intBytes);
			int saveVersion = deserializeInt(intBytes);
			
			// worldSeed (long): Seed used in world generation
			
			// Fetch the seed
			byte[] seedBytes = new byte[8];
			fis.read(seedBytes);
			long seed = deserializeLong(seedBytes);
			world.setWorldSeed(seed);
			
			// Build the column data
			fis.read(intBytes);
			int columnEntries = deserializeInt(intBytes);
			
			byte[] columnData = new byte[COLUMN_ENTRY_SIZE];
			for (int i = 0; i < columnEntries; i++)
			{
				fis.read(columnData);
				ChunkColumn column = deserializeColumn(columnData);
				world.chunkManager.chunkColumns.put(new Vec3i(column.columnX, 0, column.columnZ), column);
			}
			
			if (saveVersion == 0)
				loadChunksV0(fis);
			else
				loadChunksV1(fis);
		} catch (IOException e)
		{
			System.out.println("Failed to load a world from " + saveFile + ", generating a new one");
			e.printStackTrace();
			return false;
		}
		
		System.out.println("Successfully loaded world");
		return true;
	}
	
	private void loadChunksV0(FileInputStream fis) throws IOException
	{
		// Temporary holder
		byte[] intBytes = new byte[4];
		
		// Build the chunk data
		fis.read(intBytes);
		int chunkEntries = deserializeInt(intBytes);
		
		byte[] chunkData = new byte[CHUNK_ENTRY_SIZE0];
		for (int i = 0; i < chunkEntries; i++)
		{
			fis.read(chunkData);
			Chunk chunk = deserializeChunkV0(chunkData);
			world.chunkManager.loadedChunks.put(new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ), chunk);
		}
	}
	
	private void loadChunksV1(FileInputStream fis) throws IOException
	{
		byte[] intBytes = new byte[4];
		
		// Build the chunk data
		fis.read(intBytes);
		int chunkEntries = deserializeInt(intBytes);
		
		byte[] chunkData = new byte[CHUNK_FIXED_ENTRY_SIZE_V1];
		for (int i = 0; i < chunkEntries; i++)
		{
			// Read in fixed area (deserializeChunkV1 takes care of variable ended data)
			fis.read(chunkData);
			Chunk chunk = deserializeChunkV1(chunkData, fis);
			world.chunkManager.loadedChunks.put(new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ), chunk);
		}
	}
	
	/**
	 * Checks if the world can be loaded from a file
	 * @return True if the world can be loaded, false for generation
	 */
	public boolean canLoad()
	{
		// Check if the file exists
		// TODO: Verify the magic & save version here
		return new File(saveFile).exists();
	}
	
	/**
	 * Serializes the chunk into a series of bytes
	 * The byte order is always big endian
	 * @param chunk The chunk to serialize
	 * @return The serialized version of the chunk
	 */
	private byte[] serializeChunk(Chunk chunk)
	{
		// Save Format:
		// cX | cY | cZ | blockCount | blockLayers | blockLighting | blockData | blockMeta  ~ tickablesCount | tickables
		int chunkSaveSize = CHUNK_FIXED_ENTRY_SIZE_V1 + chunk.tickables.size() * Chunk.TICKPOS_BYTES;
		
		byte[] data = new byte[chunkSaveSize];
		ByteBuffer buf = ByteBuffer.allocate(data.length).order(ByteOrder.BIG_ENDIAN);
		
		// Serialize the chunk in the specified format
		buf.putInt(chunk.chunkX);
		buf.putInt(chunk.chunkY);
		buf.putInt(chunk.chunkZ);
		
		// Fixed area
		buf.putShort(chunk.getBlockCount());
		
		for (short s : chunk.getLayerData())
			buf.putShort(s);
		buf.put(chunk.getLightData());
		buf.put(chunk.getData());
		buf.put(chunk.getMetaData());
		
		buf.putShort((short)chunk.tickables.size());
		
		// Variable area
		for (int tickerPos : chunk.tickables)
			buf.put(serializeMedium(tickerPos));
		buf.flip();
		
		buf.get(data);
		buf.flip();
		
		return data;
	}
	
	/**
	 * Serializes the chunk column into a series of bytes
	 * The byte order is always big endian
	 * @param column The chunk colimn to serialize
	 * @return The serialized version of the chunk column
	 */
	private byte[] serializeColumn(ChunkColumn column)
	{
		// Save format
		// cX | cZ | opaqueColumns
		byte[] data = new byte[COLUMN_ENTRY_SIZE];
		ByteBuffer buf = ByteBuffer.allocate(data.length).order(ByteOrder.BIG_ENDIAN);
		
		// Serialize the chunk in the specified format
		buf.putInt(column.columnX);
		buf.putInt(column.columnZ);
		buf.put(column.opaqueColumns);
		buf.flip();
		
		buf.get(data);
		buf.flip();
		
		return data;
	}
	
	private byte[] serializeInt(int value)
	{
		return new byte[] {
				(byte)((value >> 24) & 0xFF),
				(byte)((value >> 16) & 0xFF),
				(byte)((value >>  8) & 0xFF),
				(byte)((value >>  0) & 0xFF),
		};
	}
	
	// 3 byte num
	private byte[] serializeMedium(int value)
	{
		return new byte[] {
				(byte)((value >> 16) & 0xFF),
				(byte)((value >>  8) & 0xFF),
				(byte)((value >>  0) & 0xFF),
		};
	}
	
	private byte[] serializeLong(long value)
	{
		return new byte[] {
				(byte)((value >> 56) & 0xFF),
				(byte)((value >> 48) & 0xFF),
				(byte)((value >> 40) & 0xFF),
				(byte)((value >> 32) & 0xFF),
				(byte)((value >> 24) & 0xFF),
				(byte)((value >> 16) & 0xFF),
				(byte)((value >>  8) & 0xFF),
				(byte)((value >>  0) & 0xFF),
		};
	}
	
	private int deserializeInt(byte[] bytes)
	{
		return    (Byte.toUnsignedInt(bytes[0]) << 24)
				| (Byte.toUnsignedInt(bytes[1]) << 16)
				| (Byte.toUnsignedInt(bytes[2]) << 8)
				| (Byte.toUnsignedInt(bytes[3]) << 0);
	}
	
	// 3 byte num
	private int deserializeMedium(byte[] bytes)
	{
		return    (Byte.toUnsignedInt(bytes[1]) << 16)
				| (Byte.toUnsignedInt(bytes[2]) << 8)
				| (Byte.toUnsignedInt(bytes[3]) << 0);
	}
	
	private long deserializeLong(byte[] bytes)
	{
		return    (Byte.toUnsignedLong(bytes[0]) << 56)
				| (Byte.toUnsignedLong(bytes[1]) << 48)
				| (Byte.toUnsignedLong(bytes[2]) << 40)
				| (Byte.toUnsignedLong(bytes[3]) << 32)
				| (Byte.toUnsignedLong(bytes[4]) << 24)
				| (Byte.toUnsignedLong(bytes[5]) << 16)
				| (Byte.toUnsignedLong(bytes[6]) << 8)
				| (Byte.toUnsignedLong(bytes[7]) << 0);
	}
	
	private Chunk deserializeChunkV0(byte[] data)
	{
		final ByteBuffer buf = ByteBuffer.allocate(CHUNK_ENTRY_SIZE0);
		buf.put(data);
		buf.flip();
		
		// Save Format:
		// cX | cY | cZ | blockCount | blockLighting | blockData
		
		// Fetch the chunk position
		int cx = buf.getInt();
		int cy = buf.getInt();
		int cz = buf.getInt();
		
		// Fetch the block count
		short blockCount = buf.getShort();
		
		// TODO: Change this size when adding multiple levels of lighting
		byte[] blockLighting = new byte[2 * 16 * 16];
		byte[] blockData = new byte[16 * 16 * 16];
		
		buf.get(blockLighting);
		buf.get(blockData);
		
		Chunk chunk = new Chunk(world, cx, cy, cz);
		// Workaround to stop errors
		chunk.deserialize(blockData, blockLighting, new byte[0], new short[0], new int[0], blockCount);
		return chunk;
	}
	
	private Chunk deserializeChunkV1(byte[] data, FileInputStream fis) throws IOException
	{
		ByteBuffer buf = ByteBuffer.allocate(data.length).order(ByteOrder.BIG_ENDIAN);
		buf.put(data);
		buf.flip();
		
		// Save Format:
		// cX | cY | cZ | blockCount | blockLayers | blockLighting | blockData | blockMeta ~ tickablesCount | tickables
		
		// Fetch the chunk position
		int cx = buf.getInt();
		int cy = buf.getInt();
		int cz = buf.getInt();
		
		// Fetch the block count
		short blockCount = buf.getShort();
		
		short[] blockLayers = new short[Chunk.LAYER_DATA_SIZE];
		byte[] blockLighting = new byte[Chunk.LIGHT_DATA_SIZE];
		byte[] blockData = new byte[Chunk.BLOCK_DATA_SIZE];
		byte[] blockMeta = new byte[Chunk.META_DATA_SIZE];
		
		for(int i = 0; i < blockLayers.length; i++)
			blockLayers[i] = buf.getShort();
		buf.get(blockLighting);
		buf.get(blockData);
		buf.get(blockMeta);
		
		// Fetch the tickables count
		int tickerCount = buf.getShort();
		int[] tickables = new int[tickerCount];
		
		byte[] tickPos = new byte[Chunk.TICKPOS_BYTES];
		
		for (int i = 0; i < tickerCount; i++)
		{
			// Read from the file
			fis.read(tickPos);
			tickables[i] = deserializeMedium(tickPos);
		}
		
		Chunk chunk = new Chunk(world, cx, cy, cz);
		chunk.deserialize(blockData, blockLighting, blockMeta, blockLayers, tickables, blockCount);
		return chunk;
	}
	
	private ChunkColumn deserializeColumn(byte[] data)
	{
		final ByteBuffer buf = ByteBuffer.allocate(COLUMN_ENTRY_SIZE);
		buf.put(data);
		buf.flip();
		
		// Save format
		// cX | cZ | opaqueColumns
		
		// Fetch the column
		int cx = buf.getInt();
		int cz = buf.getInt();
		
		// Fetch the column data
		byte[] opaqueColumns = new byte[16 * 16];
		buf.get(opaqueColumns);
		
		return new ChunkColumn(cx, cz, opaqueColumns);
	}
	
}
