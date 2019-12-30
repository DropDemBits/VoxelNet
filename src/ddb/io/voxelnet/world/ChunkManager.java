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
	private final World world;
	
	public ChunkManager(World world)
	{
		EMPTY_CHUNK = new Chunk(world, 0, -64, 0);
		
		perlinNoise = new PerlinOctaves(1, 0.9);
		
		this.world = world;
	}
	
	/**
	 * Gets the chunk for the requested position
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
		
		// If there's a loaded cache miss, go to the chunk manager
		if (chunk == EMPTY_CHUNK && loadNewChunks)
		{
			// Add a new chunk if the new light value is not zero
			chunk = loadChunk(pos);
		}
		
		return chunk;
	}
	
	public Chunk loadChunk(Vec3i pos)
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
	 * @param columnX The x position of the chunk column (in chunks)
	 * @param columnZ The z position of the chunk column (in chunks)
	 * @return The requested column
	 */
	public ChunkColumn loadColumn(int columnX, int columnZ)
	{
		Vec3i pos = new Vec3i(columnX, 0, columnZ);
		return loadColumn(pos);
	}
	
	/**
	 * Loads a chunk column at the specified chunk position
	 * @param pos The position of the chunk column (in chunks)
	 * @return The requested column
	 */
	public ChunkColumn loadColumn(Vec3i pos)
	{
		ChunkColumn column = new ChunkColumn(pos.getX(), pos.getZ());
		chunkColumns.put(pos, column);
		return column;
	}
	
	/**
	 * Generates a chunk column at the specified chunk position
	 * @param cx The x position of the new chunk column
	 * @param cz The z position of the new chunk column
	 */
	public void generateChunk(int cx, int cz)
	{
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
		
		int depth = 0;
		int waterLevel = 64;
		
		for (int z = 0; z < 16; z++)
			for (int x = 0; x < 16; x++)
			{
				boolean foundTallest = false;
				int colIdx = x + (z << 4);
				
				int height = 55 + (int)Math.floor(heights[colIdx] * 28.0d);
				int y = height;
				
				if (y < waterLevel)
				{
					// Generate water
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
						if (y >= waterLevel + 1)
						{
							block = Blocks.GRASS;
							blockBelow = Blocks.DIRT;
						}
						else if (y >= (waterLevel - 3))
						{
							block = Blocks.SAND;
							blockBelow = Blocks.SAND;
						}
						else
						{
							block = Blocks.GRAVEL;
							blockBelow = Blocks.GRAVEL;
						}
					}
					else if (depth < 3)
					{
						block = blockBelow;
					}
					else if ((y <= 4 && world.worldRandom.nextInt(8) == 0) || y == 0)
					{
						block = Blocks.PLANKS;
					}
					
					if (y > height)
					{
						// Generate water between the water level & the generated height
						block = Blocks.WATER;
					}
					
					byte flags = 0b1000;
					
					world.setBlock((cx << 4) + x, y, (cz << 4) + z, block, (byte)0, flags);
					
					if ((block == Blocks.WATER && y < waterLevel) || !block.isTransparent())
					{
						// Remove skylight if the height is 1 below the water level, or the block is not transparent
						getChunk(cx, y >> 4, cz, false).setSkyLight(x & 0xF, y & 0xF, z & 0xF, (byte) 0);
					}
					else if ((block == Blocks.WATER && y == waterLevel))
					{
						// Set the light level to the attenuated light
						getChunk(cx, y >> 4, cz, false).setSkyLight(x & 0xF, y & 0xF, z & 0xF, (byte) (15-block.getOpacity()));
						// Add pending sky light updates
						world.pendingShadowUpdates.add(new World.LightUpdate(new Vec3i((cx << 4) + x, y, (cz << 4) + z), (byte) 0));
					}
					
					if (!foundTallest)
					{
						// Update the respective column so that the lighting is correct
						if (!block.isTransparent() && column.opaqueColumns[colIdx] < y)
						{
							column.opaqueColumns[colIdx] = (byte) y;
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
