package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.PerlinOctaves;
import ddb.io.voxelnet.util.Vec3i;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public class ChunkManager
{
	// Map of currently loaded chunks
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
	// List/cache of previously loaded chunks and columns
	public final List<ChunkCacheEntry> chunkCache = new CopyOnWriteArrayList<>();
	// Pending set of chunks to unload
	public final Set<Long> pendingUnloads = new HashSet<>();
	
	// List of active chunk columns
	// TODO: Add Vec2i
	public final Map<Vec3i, ChunkColumn> chunkColumns = new LinkedHashMap<>();
	
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
	
	// Method to override
	protected Chunk doLoadChunk(Vec3i pos)
	{
		Chunk chunk = new Chunk(world, pos.getX(), pos.getY(), pos.getZ());
		loadedChunks.put(pos, chunk);
		chunk.chunkField.rebuildField();
		chunk.chunkField.rebuildNeighborFields();
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
	
	/**
	 * Marks a chunk column for unload
	 * @param column The chunk to be added to the pending unload queue
	 */
	public void markColumnForUnload(ChunkColumn column)
	{
		if (column == null || column.isUnloaded())
			return;
		
		// Add column to pending unload set
		long columnPair = makeColumnPair(column.columnX, column.columnZ);
		pendingUnloads.add(columnPair);
		column.markUnloaded();
		
		// Mark all chunks in a column to be unloaded
		// TODO: Associate chunks with columns
		for (int i = 0; i < 16; i++)
		{
			Chunk chunk = getChunk(column.columnX, i, column.columnZ);
			
			if (chunk != EMPTY_CHUNK)
				chunk.markUnloaded();
		}
	}
	
	/**
	 * Marks a chunk for preservation
	 * @param column The chunk to be removed from the pending unload queue
	 */
	public void markColumnForKeep(ChunkColumn column)
	{
		if (column == null || !column.isUnloaded())
			return;
		
		// Remove the column from the pending unload set
		long columnPair = makeColumnPair(column.columnX, column.columnZ);
		pendingUnloads.remove(columnPair);
		column.markLoaded();
		
		for (int i = 0; i < 16; i++)
		{
			Chunk chunk = getChunk(column.columnX, i, column.columnZ);
			
			if (chunk != EMPTY_CHUNK)
			{
				chunk.setRecentlyLoaded();
				chunk.markLoaded();
			}
		}
	}
	
	private long makeColumnPair(int columnX, int columnZ)
	{
		return (Integer.toUnsignedLong(columnX) << 32) | Integer.toUnsignedLong(columnZ);
	}
	
	// *---* Chunk Cache Management *---* //
	
	/**
	 * Unloads chunks from the unload set
	 */
	public void pruneChunks()
	{
		Iterator<Long> toEvict = pendingUnloads.iterator();
		
		if (toEvict.hasNext())
			System.out.printf("Pruning (%d)\n", pendingUnloads.size());
		
		// Evict the first 256 pairs of the set
		for (int count = 0; count < 256 && toEvict.hasNext(); count++)
		{
			// Get an eviction entry
			long evictPair = toEvict.next();
			toEvict.remove();
			
			int evictX = (int) ((evictPair >> 32));
			int evictZ = (int) ((evictPair >>  0));
			
			Vec3i pos = new Vec3i(evictX, 0, evictZ);
			
			ChunkColumn preserveColumn = chunkColumns.remove(pos);
			List<Chunk> preserveEntries = new ArrayList<>();
			
			chunkColumns.remove(pos);
			
			for (int y = 0; y < 16; y++)
			{
				pos.set(evictX, y, evictZ);
				Chunk chunk = loadedChunks.remove(pos);
				
				if (chunk != null && !chunk.isPlaceholder())
				{
					preserveEntries.add(chunk);
					
					// Update the adjacent neighbor's fields
					chunk.chunkField.rebuildNeighborFields();
					// Clear this chunk field
					chunk.chunkField.clearField();
				}
			}
			
			// Add to the chunk cache
			System.out.println("In cache: " + pos);
			chunkCache.add(new ChunkCacheEntry(preserveColumn, preserveEntries));
		}
	}
	
	/**
	 * Trys to load a column from the chunk cache
	 * @param chunkPos The chunk position to load in
	 * @return True if an entry from the cache was loaded in
	 */
	public boolean loadFromChunkCache(Vec3i chunkPos)
	{
		// If cache is empty, do nothing
		if (chunkCache.isEmpty())
			return false;
		
		// Slow check through the cache
		Predicate<ChunkCacheEntry> entFilter = (ent) ->
				ent.column != null
						&& ent.column.columnX == chunkPos.getX()
						&& ent.column.columnZ == chunkPos.getZ();
		
		long duplicates = chunkCache.parallelStream().filter(entFilter).count();
		if (duplicates > 1)
			System.out.println("Duplicate entries (" + chunkPos + "): " + duplicates);
		
		ChunkCacheEntry entry = chunkCache.parallelStream()
				.filter(entFilter)
				.findAny()
				.orElse(null);
		
		if (entry == null)
			return false;
		
		chunkCache.remove(entry);
		
		System.out.println("Loading from cache " + chunkPos);
		
		// Entry is not null, add back to cache
		entry.column.markLoaded();
		chunkColumns.put(new Vec3i(entry.column.columnX, 0, entry.column.columnZ), entry.column);
		
		entry.chunks.parallelStream()
				.forEach((chunk) -> {
					chunk.setRecentlyLoaded();
					chunk.markLoaded();
					loadedChunks.put(new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ), chunk);
					
					// Rebuild the chunk fields
					chunk.chunkField.rebuildField();
					chunk.chunkField.rebuildNeighborFields();
				});
		
		return true;
	}
	
	private static class ChunkCacheEntry
	{
		final ChunkColumn column;
		final List<Chunk> chunks;
		
		private ChunkCacheEntry(ChunkColumn column, List<Chunk> chunks)
		{
			this.column = column;
			this.chunks = chunks;
		}
	}
}
