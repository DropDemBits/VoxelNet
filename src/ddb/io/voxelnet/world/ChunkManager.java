package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.PerlinOctaves;
import ddb.io.voxelnet.util.Vec3i;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChunkManager
{
	// Map of currently loaded chunks
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
	// Highest, opaque block in each chunk column (vertical group of chunks)
	// Accessed by index = x + z * 16
	// TODO: Add Vec2i
	final Map<Vec3i, ChunkColumn> chunkColumns = new LinkedHashMap<>();
	// Empty Chunk
	public final Chunk EMPTY_CHUNK;
	
	public final PerlinOctaves perlinNoise;
	
	// World associated with this chunk manager
	protected final World world;
	
	public ChunkManager(World world)
	{
		EMPTY_CHUNK = new Chunk(world, 0, -64, 0);
		
		perlinNoise = new PerlinOctaves(1, 0.9);
		
		this.world = world;
	}
	
	/**
	 * Gets the chunk for the requested position
	 * By default, does not load in new chunks
	 * @param chunkX The x position of the target chunk (in chunks)
	 * @param chunkY The y position of the target chunk (in chunks)
	 * @param chunkZ The z position of the target chunk (in chunks)
	 * @return The requested chunk
	 */
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		return getChunk(chunkX, chunkY, chunkZ, false);
	}
	
	/**
	 * Gets the chunk for the requested position
	 * By default, does not load in new chunks
	 * @param pos The position of the target chunk (in chunks)
	 * @return The requested chunk
	 */
	public Chunk getChunk(Vec3i pos)
	{
		return getChunk(pos, false);
	}
	
	/**
	 * Gets the chunk for the requested position
	 * @param chunkX The x position of the target chunk (in chunks)
	 * @param chunkY The y position of the target chunk (in chunks)
	 * @param chunkZ The z position of the target chunk (in chunks)
	 * @param loadNewChunks Whether to load new chunks if none was found
	 * @return The requested chunk
	 */
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ, boolean loadNewChunks)
	{
		Vec3i pos = new Vec3i(chunkX, chunkY, chunkZ);
		return getChunk(pos, loadNewChunks);
	}
	
	/**
	 * Gets the chunk for the requested position
	 * @param pos The position of the target chunk (in chunks)
	 * @param loadNewChunks Whether to create a new chunk if none was found
	 * @return The requested chunk
	 */
	public Chunk getChunk(Vec3i pos, boolean loadNewChunks)
	{
		Chunk chunk = loadedChunks.getOrDefault(pos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK && loadNewChunks)
		{
			// If we are to load new chunks, do so
			chunk = loadChunk(pos);
		}
		
		return chunk;
	}
	
	/**
	 * Loads the requested chunk
	 * A chunk may be loaded from disk, or requested from the network
	 * @param pos The chunk coordinate of the chunk to load
	 * @return The requested chunk
	 */
	public Chunk loadChunk(Vec3i pos)
	{
		return doLoadChunk(pos);
	}
	
	protected Chunk doLoadChunk(Vec3i pos)
	{
		Chunk chunk = new Chunk(world, pos.getX(), pos.getY(), pos.getZ());
		loadedChunks.put(pos, chunk);
		return chunk;
	}
	
	/**
	 * Gets a chunk column at the specified chunk position
	 * @param columnX The x position of the chunk column (in chunks)
	 * @param columnZ The z position of the chunk column (in chunks)
	 * @return The requested column
	 */
	public ChunkColumn getColumn(int columnX, int columnZ)
	{
		Vec3i pos = new Vec3i(columnX, 0, columnZ);
		return getColumn(pos);
	}
	
	/**
	 * Gets the chunk column at the specified chunk position
	 * @param pos The position of the chunk column (in chunks)
	 * @return The requested column
	 */
	public ChunkColumn getColumn(Vec3i pos)
	{
		return chunkColumns.getOrDefault(pos, null);
	}
	
	/**
	 * Loads a chunk column at the specified chunk position
	 * The chunk column may either be loaded from disk or requested over the network
	 * @param columnX The x position of the chunk column (in chunks)
	 * @param columnZ The z position of the chunk column (in chunks)
	 */
	public void loadColumn(int columnX, int columnZ)
	{
		Vec3i pos = new Vec3i(columnX, 0, columnZ);
		doColumnLoad(pos);
	}
	
	/**
	 * Loads a chunk column at the specified chunk position
	 * The chunk column may either be loaded from disk or requested over the network
	 * @param pos The position of the chunk column (in chunks)
	 */
	public void loadColumn(Vec3i pos)
	{
		doColumnLoad(pos);
	}
	
	/**
	 * Implementation of "loadColumn"
	 * @param pos The position of the chunk column (in chunks)
	 */
	protected void doColumnLoad(Vec3i pos)
	{
		// TODO: Check if the column is in the unloaded column cache
		// Default: Generate new chunks
		generateChunk(pos.getX(), pos.getZ());
	}
	
	/**
	 * Checks if the column at the given coordinates is loaded.
	 * A column is loaded if it exists either in the chunk manager's cache, or
	 * is in the active column set.
	 *
	 * @param pos The chunk column to check
	 * @return True if the column is loaded
	 */
	public boolean isColumnLoaded(Vec3i pos)
	{
		return getColumn(pos) != null;
	}
	
	/**
	 * Checks if the column at the given coordinates is loaded.
	 * A column is loaded if it exists either in the chunk manager's cache, or
	 * is in the active column set.
	 *
	 * @param chunkX The chunk column's x position
	 * @param chunkZ The chunk column's z position
	 * @return True if the column is loaded into the chunk manager's cache
	 */
	public boolean isColumnLoaded(int chunkX, int chunkZ)
	{
		return getColumn(chunkX, chunkZ) != null;
	}
	
	/**
	 * Generates a chunk column at the specified chunk position
	 * @param cx The x position of the new chunk column
	 * @param cz The z position of the new chunk column
	 */
	public void generateChunk(int cx, int cz)
	{
		// ???: The server sends out the chunk column before a light update is performed, should the server send out a light update packet/notification?
		// Make the chunk columns
		ChunkColumn column = new ChunkColumn(cx, cz);
		chunkColumns.put(new Vec3i(cx, 0, cz), column);
		
		// Pre-generate heightmap
		double[] heights = new double[16 * 16];
		for (int z = 15; z >= 0; z--)
		{
			for (int x = 15; x >= 0; x--)
			{
				double noiseX, noiseZ;
				
				noiseX = (0.25d * (cx * 16f + x) / 16.0d);
				noiseZ = (0.25d * (cz * 16f + z) / 16.0d);
				
				heights[x + z * 16] = perlinNoise.perlinOctaves(noiseX, noiseZ, 0.5d);
			}
		}
		
		// Distance below the heightmap sample point
		int depth = 0;
		int waterLevel = 64;
		
		// Fill in the heightmap, starting from the heightmap or the water level, whichever is taller
		for (int z = 0; z < 16; z++)
		{
			for (int x = 0; x < 16; x++)
			{
				boolean foundTallest = false;
				int colIdx = x + (z << 4);
				
				// Height sampled from the heightmap
				int height = 55 + (int)Math.floor(heights[colIdx] * 28.0d);
				// Filling in height
				int y = height;
				
				if (y < waterLevel)
				{
					// Generate water below the water level
					y = waterLevel;
				}
				
				// Used in generating the blocks below the top layer
				Block blockBelow = Blocks.AIR;
				
				for (; y >= 0; y--)
				{
					Block block = Blocks.STONE;
					
					// Setup the top layers
					if (depth == 0)
					{
						// Setup the blocks to place
						if (y >= waterLevel + 1)
						{
							// Above the water level, generate grass & dirt
							block = Blocks.GRASS;
							blockBelow = Blocks.DIRT;
						} else if (y >= (waterLevel - 3))
						{
							// At and 3 block below water level, generate sand
							block = Blocks.SAND;
							blockBelow = Blocks.SAND;
						} else
						{
							// Below water level, generate gravel
							block = Blocks.GRAVEL;
							blockBelow = Blocks.GRAVEL;
						}
					} else if (depth < 3)
					{
						// Start placing the below block
						block = blockBelow;
					} else if ((y <= 4 && world.worldRandom.nextInt(8) == 0) || y == 0)
					{
						// Start filling in random places with planks
						block = Blocks.PLANKS;
					}
					
					if (y > height)
					{
						// Generate water between the water level & the generated height
						block = Blocks.WATER;
					}
					
					byte flags = 0b1000;
					
					world.setBlock((cx << 4) + x, y, (cz << 4) + z, block, 0, flags);
					
					if ((block == Blocks.WATER && y < waterLevel) || !block.isTransparent())
					{
						// Remove skylight if the height is 1 below the water level, or the block is not transparent
						getChunk(cx, y >> 4, cz, false).setSkyLight(x & 0xF, y & 0xF, z & 0xF, 0);
					} else if ((block == Blocks.WATER && y == waterLevel))
					{
						// Set the light level to the attenuated light
						getChunk(cx, y >> 4, cz, false).setSkyLight(x & 0xF, y & 0xF, z & 0xF, (15 - block.getOpacity()));
						// Add pending sky light updates
						world.addSkyLightUpdate(new Vec3i((cx << 4) + x, y, (cz << 4) + z), 0);
					}
					
					if (!foundTallest)
					{
						// Update the respective column so that the lighting is correct
						if (!block.isTransparent() && column.getTallestOpaque(x, z) < y)
						{
							column.setTallestOpaque(x, z, y);
							foundTallest = true;
						}
					}
					
					// Only increase the depth once the generated height is reached
					if (y <= height)
						depth++;
				}
				
				depth = 0;
			}
		}
	}
}
