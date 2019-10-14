package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockWater;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.PerlinOctaves;
import ddb.io.voxelnet.util.Vec3i;

import java.util.*;

public class World
{
	// Map of currently loaded chunks
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
	// Highest, opaque block in each chunk column (vertical group of chunks)
	// Accessed by index = x + z * 16
	// TODO: Add Vec2i
	final Map<Vec3i, ChunkColumn> chunkColumns = new LinkedHashMap<>();
	// Empty Chunk
	public final Chunk EMPTY_CHUNK;
	
	private float accumulatedWorldTick;
	
	// List of entities that are waiting to be added
	public List<Entity> pendingEntities;
	// List of currently loaded entities
	public List<Entity> loadedEntities;
	
	// WorldGen
	private long worldSeed;
	private Random worldRandom;
	private PerlinOctaves perlinNoise;
	
	public World()
	{
		EMPTY_CHUNK = new Chunk(this, 0, -64, 0);
		//worldSeed = System.currentTimeMillis();
		//worldSeed = 1566757735901L;
		worldSeed = 1566847034636L;
		worldRandom = new Random(worldSeed);
		loadedEntities = new ArrayList<>();
		pendingEntities = new ArrayList<>();
		perlinNoise = new PerlinOctaves(1, 0.9);
		
		setWorldSeed(worldSeed);
	}
	
	/**
	 * Generates a set of chunks
	 */
	public void generate()
	{
		System.out.println("Generating world with seed " + worldSeed);
		
		long startGen = System.currentTimeMillis();
		for (int cx = -8; cx <= 7; cx++)
		{
			for (int cz = -8; cz <= 7; cz++)
			{
				generateChunk(cx, cz);
			}
		}
		System.out.println("Done base generation in " + (System.currentTimeMillis() - startGen) + " ms");
		
		startGen = System.currentTimeMillis();
		System.out.println("Done generation in " + (System.currentTimeMillis() - startGen) + " ms");
	}
	
	public void setWorldSeed(long newSeed)
	{
		worldSeed = newSeed;
		worldRandom.setSeed(newSeed);
		perlinNoise.seed(worldSeed);
	}
	
	public long getWorldSeed()
	{
		return worldSeed;
	}
	
	/**
	 * Gets the block light at the given position, accounting for sky light
	 * @param x The x position to fetch
	 * @param y The y position to fetch
	 * @param z The z position to fetch
	 * @return The combined block light and sky light values
	 */
	public byte getBlockLight(int x, int y, int z)
	{
		if (y < 0)
			return 0;
		if (y >= 256)
			return 15;
		
		int chunkX = x >> 4;
		int chunkY = y >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		Vec3i chunkPos = new Vec3i(chunkX, chunkY, chunkZ);
		byte baseLight;
		
		baseLight = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getBlockLight(blockX, blockY, blockZ);
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkColumns.getOrDefault(new Vec3i(chunkX, 0, chunkZ), null);
		boolean canSeeSky;
		
		if (column == null)
			canSeeSky = true; // If a column is missing, the blocks can definitely see the sky
		else
			canSeeSky = y >= Byte.toUnsignedInt(column.opaqueColumns[blockX + blockZ * 16]);
		
		if (canSeeSky)
			baseLight = 15;
		
		return baseLight;
	}
	
	/**
	 * Gets the block at the specified position
	 * If the position is not inside of a loaded chunk, air (0) is returned
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @return The specified block, or Blocks.VOID if
	 * the position is out of bounds
	 */
	public Block getBlock (int x, int y, int z)
	{
		if (y < 0)
			return Blocks.VOID;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		if (!loadedChunks.containsKey(chunkPos))
			return Blocks.AIR;
		
		return Block.idToBlock(loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getBlock(x & 0xF, y & 0xF, z & 0xF));
	}
	
	/**
	 * Sets the block id at the given position
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to placek
	 */
	public void setBlock (int x, int y, int z, Block block)
	{
		setBlock(x, y, z, block, (byte)0, 7);
	}
	
	/**
	 * Sets the block id at the given position, with some metadata
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to place
	 * @param meta The metadata of the block
	 */
	public void setBlock (int x, int y, int z, Block block, byte meta)
	{
		setBlock(x, y, z, block, meta, 7);
	}
	
	/**
	 * Sets the block id at the given position, with some metadata and flags
	 * The flags are defined to be as follows:
	 * Bit 0: When set, updates the lighting
	 * Bit 1: When set, updates the adjacent chunks
	 * Bit 2: When set, updates the adjacent neighbors
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to place
	 * @param meta The metadata of the block
	 * @param flags The bitmap of the flags to use
	 */
	public void setBlock (int x, int y, int z, Block block, byte meta, int flags)
	{
		// If a lighting update needs to occur
		boolean lightingUpdate = false;
		
		// Decode the flags
		boolean updateLighting = (flags & 1) != 0;
		boolean updateNeighborChunks = (flags & 2) != 0;
		boolean updateNeighbors = (flags & 4) != 0;
		
		// Don't set block below or above the world
		if (y < 0 || y > 255)
			return;
		
		// Do nothing for void
		if (block == Blocks.VOID)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK && block != Blocks.AIR)
		{
			// Add a new chunk if the id is not zero
			chunk = new Chunk(this, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
			loadedChunks.put(chunkPos, chunk);
		}
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Set the block & meta
		chunk.setBlock(blockX, blockY, blockZ, block.getId());
		chunk.setBlockMeta(blockX, blockY, blockZ, meta);
		
		// Update the chunk column
		Vec3i columnPos = new Vec3i(x >> 4, 0, z >> 4);
		ChunkColumn chunkColumn = chunkColumns.getOrDefault(columnPos, null);
		
		if (chunkColumn == null)
		{
			if (block == Blocks.AIR)
				return; // Don't need to add a column if air is being placed
			
			// Add a new chunk column if the block isn't air
			chunkColumn = new ChunkColumn(columnPos.getX(), columnPos.getZ());
			chunkColumns.put(columnPos, chunkColumn);
		}
		
		if (updateLighting)
		{
			// Update the appropriate column
			int columnIdx = blockX + blockZ * 16;
			int tallestOpaque = Byte.toUnsignedInt(chunkColumn.opaqueColumns[columnIdx]);
			
			// 3 Main Groups for Column placement
			// - Below tallest opaque block
			// - At tallest opaque block
			// - Above tallest opaque block
			// If the block is below the tallest opaque block, then do nothing
			// If the block is above the tallest block and is opaque, make this
			//    block the tallest one, otherwise do nothing
			// If the block is at the tallest opaque block and is not opaque (i.e
			//    air or isTransparent), search for the next opaque block below.
			//    Otherwise, nothing happens
			// If no match is found, set the tallest opaque block to zero.
			
			if (y > tallestOpaque)
			{
				// Only do update for opaque blocks
				if (!block.isTransparent())
				{
					chunkColumn.opaqueColumns[columnIdx] = (byte) y;
					lightingUpdate = true;
				}
			}
			
			if (y == tallestOpaque && block.isTransparent())
			{
				// Only update on same height if the block is not opaque or a search should be performed
				int height = y - 1;
				
				for (; height != y; height = (--height) & 0xFF)
				{
					if (!getBlock(x, height, z).isTransparent())
						break;
				}
				
				// If the height is the same, make the column empty
				if (height == y)
					height = 0;
				
				chunkColumn.opaqueColumns[columnIdx] = (byte) height;
				lightingUpdate = true;
			}
		}
		
		if (updateNeighborChunks)
		{
			// Update the adjacent in the column chunks if the block is at one of the chunk edges
			if (blockY == 15)
				loadedChunks.getOrDefault(chunkPos.add(0, 1, 0), EMPTY_CHUNK).forceRebuild();
			
			for (int yPos = chunkPos.getY(); yPos >= 0; yPos--)
			{
				int yOff = yPos - chunkPos.getY();
				
				if (yOff != 0)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, 0), EMPTY_CHUNK).forceRebuild();
				
				// If there was no lighting update, only update the directly
				// adjacent chunks
				if (!lightingUpdate && yOff < -1)
					break;
				
				if (blockX == 0)
					loadedChunks.getOrDefault(chunkPos.add(-1, yOff, 0), EMPTY_CHUNK).forceRebuild();
				else if (blockX == 15)
					loadedChunks.getOrDefault(chunkPos.add(1, yOff, 0), EMPTY_CHUNK).forceRebuild();
				
				if (blockZ == 0)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, -1), EMPTY_CHUNK).forceRebuild();
				else if (blockZ == 15)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, 1), EMPTY_CHUNK).forceRebuild();
			}
		}
		
		if (updateNeighbors)
		{
			// Update the neighboring blocks
			for (Facing face : Facing.values())
			{
				Block neighbor = getBlock(x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
				
				// Update the neighbor if it's not air
				if (neighbor != Blocks.AIR)
					neighbor.onNeighborUpdated(
							this,
							x + face.getOffsetX(),
							y + face.getOffsetY(),
							z + face.getOffsetZ(),
							face.getOpposite());
			}
		}
	}
	
	/**
	 * Sets the block metadata for the given position
	 * @param x The x position of the target block
	 * @param y The y position of the target block
	 * @param z The z position of the target block
	 * @param meta The new metadata for the block
	 */
	public void setBlockMeta (int x, int y, int z, byte meta)
	{
		// Don't set block below or above the world
		if (y < 0 || y > 255)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK)
			return;
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Set the block meta
		chunk.setBlockMeta(blockX, blockY, blockZ, meta);
	}
	
	/**
	 * Gets the block metadata for the given position
	 * @param x The x position of the target block
	 * @param y The y position of the target block
	 * @param z The z position of the target block
	 * @return  The metadata for the block position
	 */
	public byte getBlockMeta (int x, int y, int z)
	{
		// Don't fetch data below or above the world
		if (y < 0 || y > 255)
			return 0;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK)
			return 0;
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Get the block meta
		return chunk.getBlockMeta(blockX, blockY, blockZ);
	}
	
	/**
	 * Gets the chunk for the requested position
	 * @param cx The x position of the target chunk (in chunks)
	 * @param cy The y position of the target chunk (in chunks)
	 * @param cz The z position of the target chunk (in chunks)
	 * @return The requested chunk
	 */
	public Chunk getChunk(int cx, int cy, int cz)
	{
		Vec3i pos = new Vec3i(cx, cy, cz);
		return loadedChunks.getOrDefault(pos, EMPTY_CHUNK);
	}
	
	public boolean isChunkPresent(int cx, int cy, int cz)
	{
		//return getChunk(cx, cy, cz) != EMPTY_CHUNK;
		return loadedChunks.containsKey(new Vec3i(cx, cy, cz));
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
		
		for (int z = 0; z < 16; z++)
		for (int x = 0; x < 16; x++)
		{
			boolean foundTallest = false;
			int colIdx = x + (z << 4);
			
			int height = 55 + (int)Math.floor(heights[colIdx] * 28.0d);
			int y = height;
			
			if (y < 63)
			{
				// Generate water
				y = 63;
			}
			
			// Used in generating the blocks below the top layer
			Block blockBelow = Blocks.AIR;
			
			for (; y >= 0; y--)
			{
				Block block = Blocks.STONE;
				
				// Setup the top layers
				if (depth == 0)
				{
					if (y >= 64)
					{
						block = Blocks.GRASS;
						blockBelow = Blocks.DIRT;
					}
					else if (y >= 60)
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
				else if ((y <= 4 && worldRandom.nextInt(8) == 0) || y == 0)
				{
					block = Blocks.PLANKS;
				}
				
				if (y > height)
				{
					block = Blocks.WATER;
				}
				
				setBlock((cx << 4) + x, y, (cz << 4) + z, block, (byte)0, 0);
				
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
	
	public void explode(int centreX, int centreY, int centreZ, int radius)
	{
		// Pass 1: Initial block removal
		// No updates to adjacent chunks, or nearest neighbor updates
		for (int x = -radius; x <= radius; x++)
		{
			for (int y = -radius; y <= radius; y++)
			{
				for (int z = -radius; z <= radius; z++)
				{
					int dist = x*x + y*y + z*z;
					if (dist > radius*radius)
						continue;
					setBlock(centreX + x, centreY + y, centreZ + z, Blocks.AIR, (byte)0, 1);
				}
			}
		}
		
		// Pass 2: Updates
		// Update all the things (flags=7) at the edge
		for (int x = -radius; x <= radius; x++)
		{
			for (int y = -radius; y <= radius; y++)
			{
				for (int z = -radius; z <= radius; z++)
				{
					int dist = x*x + y*y + z*z;
					if (dist < (radius*radius) - 2*radius + 1 || dist > (radius*radius))
						continue;
					
					setBlock(centreX + x, centreY + y, centreZ + z, Blocks.AIR, (byte)0, 7);
				}
			}
		}
	}
	
	/**
	 * Adds an entity to the world
	 * @param e The entity to add
	 */
	public void addEntity(Entity e)
	{
		// Set the entity's world
		e.setWorld(this);
		
		// Add to the list
		pendingEntities.add(e);
	}
	
	/**
	 * Updates the world
	 * @param delta The delta for updates
	 */
	public void update(float delta)
	{
		accumulatedWorldTick += delta;
		if (accumulatedWorldTick > 1f/4f)
		{
			accumulatedWorldTick = 0;
			doBlockTick();
		}
		
		// Add all of the pending entities
		loadedEntities.addAll(pendingEntities);
		pendingEntities.clear();
		
		// Update all of the entities
		for (Entity e : loadedEntities)
		{
			e.update(delta);
		}
		
		// Remove all the entities that need to be removed
		loadedEntities.removeIf((e) -> e.isRemoved);
	}
	
	private void doBlockTick()
	{
		// Update the loaded chunks
		List<Chunk> workingList = new ArrayList<>(loadedChunks.values());
		workingList.iterator().forEachRemaining((chunk) ->
		{
			// y z x
			for (int y = 0; y < 16; y++)
			{
				for (int z = 0; z < 16; z++)
				{
					for (int x = 0; x < 16; x++)
					{
						Block block = Block.idToBlock(chunk.getBlock(x, y, z));
						
						if (block.isTickable())
							block.onTick(this, x + chunk.chunkX * 16, y + chunk.chunkY * 16, z + chunk.chunkZ * 16);
					}
				}
			}
		});
		
		// XXX: AGGGH! Use a better solution?
		BlockWater.updateWater(this);
	}
	
}
