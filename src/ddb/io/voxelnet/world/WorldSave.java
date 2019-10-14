package ddb.io.voxelnet.world;

import ddb.io.voxelnet.util.Vec3i;

import java.io.*;
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
	private static final byte[] SAVE_MAGIC = "VXNT".getBytes();
	private static final int CHUNK_ENTRY_SIZE = 4*3 + 2 + 512 + 4096;
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
			// ColumnData: Varible length array holding the column data
			// ChunkCount (int): Number of chunk save entries
			// ChunkData: Varible length array holding the chunk data
			
			// Header:
			// magic | saveVersion | worldSeed
			// Magic (char[4]): "VXNT" in bytes
			// saveVersion (int): Save version (initially 0)
			// worldSeed (long): Seed used in world generation
			
			stream.write(SAVE_MAGIC);                          // Magic
			stream.write(serializeInt(0));               // Save Version
			stream.write(serializeLong(world.getWorldSeed())); // Seed
			
			///////////////////////////////////////////////////////////////////
			// Write out the column entry count
			int columnEntries = world.chunkColumns.size();
			stream.write(serializeInt(columnEntries)); // Column Entry Count
			
			// Iterate through all of the ChunkColumns
			for (ChunkColumn column : world.chunkColumns.values())
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
			for (Chunk chunk : world.loadedChunks.values())
			{
				if (!chunk.isEmpty())
					++chunkEntries;
			}
			
			stream.write(serializeInt(chunkEntries)); // Chunk Entry Count
			
			// Iterate through all of the loaded chunks in the world again to
			// actually save them
			for (Chunk chunk : world.loadedChunks.values())
			{
				// Skip empty chunks
				if (chunk.isEmpty())
					continue;
				
				// Need to save
				// Position (Cx, Cy, Cz)
				// Block Lighting (blockLights)
				// Block count (blockCount)
				// Block Data (blockData)
				
				// Don't need to save
				// blockLayers (not used)
				// isEmpty (implied in block count)
				// needsRebuild & isDirty (only used during runtime)
				// recentlyGenerated (only used to generate the chunk's ChunkModel)
				
				// Each chunk costs 4622 Bytes / ~4KiB to store
				// blockData: 4096 bytes
				// blockLighting: 512 bytes
				// chunkX, chunkY, chunkZ: 4+4+4 bytes
				// blockCount: 2 bytes
				
				// Save Format:
				// cX | cY | cZ | blockCount | blockLighting | blockData
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
			// Skip the header magic & the save version
			long skipped = fis.skip(SAVE_MAGIC.length + 4);
			
			// Sanity check
			if (skipped != SAVE_MAGIC.length + 4)
			{
				System.err.println("Error: save file is too small!");
				return false;
			}
			
			// worldSeed (long): Seed used in world generation
			
			// Fetch the seed
			byte[] seedBytes = new byte[8];
			fis.read(seedBytes);
			long seed = deserializeLong(seedBytes);
			world.setWorldSeed(seed);
			
			// Temporary holder
			byte[] intBytes = new byte[4];
			
			// Build the column data
			fis.read(intBytes);
			int columnEntries = deserializeInt(intBytes);
			
			byte[] columnData = new byte[COLUMN_ENTRY_SIZE];
			for (int i = 0; i < columnEntries; i++)
			{
				fis.read(columnData);
				ChunkColumn column = deserializeColumn(columnData);
				world.chunkColumns.put(new Vec3i(column.columnX, 0, column.columnZ), column);
			}
			
			// Build the chunk data
			fis.read(intBytes);
			int chunkEntries = deserializeInt(intBytes);
			
			byte[] chunkData = new byte[CHUNK_ENTRY_SIZE];
			for (int i = 0; i < chunkEntries; i++)
			{
				fis.read(chunkData);
				Chunk chunk = deserializeChunk(chunkData);
				world.loadedChunks.put(new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ), chunk);
			}
		} catch (IOException e)
		{
			System.out.println("Failed to load a world from " + saveFile + ", generating a new one");
			e.printStackTrace();
			return false;
		}
		
		System.out.println("Successfully loaded world");
		return true;
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
		// cX | cY | cZ | blockCount | blockLighting | blockData
		byte[] data = new byte[CHUNK_ENTRY_SIZE];
		final ByteBuffer buf = ByteBuffer.allocate(data.length).order(ByteOrder.BIG_ENDIAN);
		
		// Serialize the chunk in the specified format
		buf.putInt(chunk.chunkX);
		buf.putInt(chunk.chunkY);
		buf.putInt(chunk.chunkZ);
		buf.putShort(chunk.getBlockCount());
		buf.put(chunk.getLightData());
		buf.put(chunk.getData());
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
		final ByteBuffer buf = ByteBuffer.allocate(data.length).order(ByteOrder.BIG_ENDIAN);
		
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
	
	private Chunk deserializeChunk(byte[] data)
	{
		final ByteBuffer buf = ByteBuffer.allocate(CHUNK_ENTRY_SIZE);
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
		
		return new Chunk(world, cx, cy, cz, blockData, blockLighting, blockCount);
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
