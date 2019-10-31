package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockWater;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.PerlinOctaves;
import ddb.io.voxelnet.util.Vec3i;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	public final List<Entity> pendingEntities;
	// List of currently loaded entities
	public final List<Entity> loadedEntities;
	
	private Queue<LightUpdate> pendingLightRemoves;
	private Queue<LightUpdate> pendingLightUpdates;
	private Queue<LightUpdate> pendingShadowRemoves;
	private Queue<LightUpdate> pendingShadowUpdates;
	
	// WorldGen
	private long worldSeed;
	public final Random worldRandom;
	private final PerlinOctaves perlinNoise;
	
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
		
		pendingLightUpdates = new ConcurrentLinkedQueue<>();
		pendingLightRemoves = new ConcurrentLinkedQueue<>();
		
		pendingShadowUpdates = new ConcurrentLinkedQueue<>();
		pendingShadowRemoves = new ConcurrentLinkedQueue<>();
		
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
		System.out.println("Done terrain generation in " + (System.currentTimeMillis() - startGen) + " ms");
		
		// Old explosion generation
		/*int explosionCount = worldRandom.nextInt(100) + 50;
		for (int i = 0; i < explosionCount; i++)
		{
			int x = worldRandom.nextInt(256) - 128;
			int y = worldRandom.nextInt(64);
			int z = worldRandom.nextInt(256) - 128;
			int radius = worldRandom.nextInt(20) + 5;
			
			explode(x, y, z, radius);
		}*/
		
		startGen = System.currentTimeMillis();
		processLightUpdate();
		System.out.println("Done light generation in " + (System.currentTimeMillis() - startGen) + " ms");
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
	
	public int getColumnHeight(int x, int y, int z)
	{
		if (y >= 256)
			return y;
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockZ = z & 0xF;
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkColumns.getOrDefault(new Vec3i(chunkX, 0, chunkZ), null);
		if (column == null)
			return 0;
		
		return column.opaqueColumns[blockX + blockZ * 16];
	}
	
	public boolean canBlockSeeSky(int x, int y, int z)
	{
		if (y >= 256)
			return true;
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockZ = z & 0xF;
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkColumns.getOrDefault(new Vec3i(chunkX, 0, chunkZ), null);
		boolean canSeeSky;
		
		if (column == null)
			canSeeSky = true; // If a column is missing, the blocks can definitely see the sky
		else if(y >= 0)
			canSeeSky = y > Byte.toUnsignedInt(column.opaqueColumns[blockX + blockZ * 16]); // Can't see at the opaque
		else
			canSeeSky = Byte.toUnsignedInt(column.opaqueColumns[blockX + blockZ * 16]) == 0; // If 0 (column empty), can see the sky
		
		return canSeeSky;
	}
	
	public boolean canBlockDirectlySeeSky(int x, int y, int z)
	{
		if (y >= 256)
			return true;
		
		if (!canBlockSeeSky(x, y, z))
			return false;
		
		for (int checkY = y+1; checkY < 256; checkY++)
		{
			if (getBlock(x, checkY, z) != Blocks.AIR)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Gets the sky light at the given position
	 * Note:
	 * 15 indicates the maximum amount of skylight, 0 indicates the least
	 * amount
	 *
	 * @param x The x position to fetch
	 * @param y The y position to fetch
	 * @param z The z position to fetch
	 * @return The sky light value
	 */
	public byte getSkyLight(int x, int y, int z)
	{
		if (y < 0)
		{
			// If the position can see the sky, full skylight
			return canBlockSeeSky(x, y, z) ? (byte)15 : (byte)0;
		}
		if (y >= 256)
			return 15;
		
		int chunkX = x >> 4;
		int chunkY = y >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		Vec3i chunkPos = new Vec3i(chunkX, chunkY, chunkZ);
		byte skyLight = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getSkyLight(blockX, blockY, blockZ);
		
		// Technically a shadow map, but whatever
		return skyLight;
	}
	
	/**
	 * Gets the block light at the given position
	 * @param x The x position to fetch
	 * @param y The y position to fetch
	 * @param z The z position to fetch
	 * @return The block light value
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
		return loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getBlockLight(blockX, blockY, blockZ);
	}
	
	private void setBlockLight(int x, int y, int z, byte newLight)
	{
		if (newLight < 0)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK && newLight > 0)
		{
			// Add a new chunk if the new light value is not zero
			chunk = new Chunk(this, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
			loadedChunks.put(chunkPos, chunk);
		}
		
		chunk.setBlockLight(x & 0xF, y & 0xF, z & 0xF, newLight);
	}
	
	private void setSkyLight(int x, int y, int z, byte newLight)
	{
		if (newLight > 15 || newLight < 0)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK && newLight != 0)
		{
			// Add a new chunk if the new light value is not zero
			chunk = new Chunk(this, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
			loadedChunks.put(chunkPos, chunk);
		}
		
		chunk.setSkyLight(x & 0xF, y & 0xF, z & 0xF, newLight);
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
	 * @param block The block to place
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
	 * Bit 3: When set, doesn't update the opaque chunk column
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
		// TODO: (re)build shadow map
		// Decode the flags
		boolean updateLighting  = (flags & 1) != 0;
		boolean updateNeighborChunks = (flags & 2) != 0;
		boolean updateNeighbors = (flags & 4) != 0;
		boolean updateColumns   = (flags & 8) == 0;
		
		// If a lighting update needs to occur
		boolean lightingUpdate = false;
		// If the light update was the result of the tallest block moving down
		boolean tallestDown = false;
		// Last block light
		int lastBlockLight;
		int lastSkyLight;
		
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
		
		// Chunk column index
		int columnIdx = blockX + blockZ * 16;
		
		// Set the block, block light & meta
		lastBlockLight = chunk.getBlockLight(blockX, blockY, blockZ);
		lastSkyLight = chunk.getSkyLight(blockX, blockY, blockZ);
		
		chunk.setBlock(blockX, blockY, blockZ, block.getId());
		chunk.setBlockMeta(blockX, blockY, blockZ, meta);
		
		// Only update the current light value if
		// - The new block is air
		// - The new block is not transparent
		// - The new block's light is brighter than the old one
		if (block == Blocks.AIR || !block.isTransparent() || block.getBlockLight() > lastBlockLight)
			chunk.setBlockLight(blockX, blockY, blockZ, block.getBlockLight());
		
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
		
		int oldestHeight = Byte.toUnsignedInt(chunkColumn.opaqueColumns[columnIdx]);
		
		if (updateLighting && updateColumns)
		{
			// Update the appropriate column
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
					System.out.println("above");
					chunkColumn.opaqueColumns[columnIdx] = (byte) y;
					lightingUpdate = true;
					tallestDown = false;
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
				tallestDown = true;
			}
		}
		
		if (updateLighting)
		{
			// Only check for unrestricted skylight
			boolean skyAvailable = canBlockDirectlySeeSky(x, y, z);
			
			// Was doing: Fixing issues with opacity propagation
			// Also: Sometimes lighting isn't visually updated in adjacent chunks,
			// enqueue force rebuild for afflicted chunks
			
			// Check needs to be done so that opacity is correctly applied
			if (block == Blocks.AIR && skyAvailable)
			{
				// If the block can see the sky, and the tallest block has moved down, set it to the maximum light value
				chunk.setSkyLight(blockX, blockY, blockZ, (byte) 15);
				pendingShadowUpdates.add(new LightUpdate(new Vec3i(x, tallestDown ? oldestHeight : y, z), (byte) 0));
				
				if (Game.showThings)
					System.out.println("ho boi! " + tallestDown + ", " + oldestHeight + ", " + y);
			}
			else
			{
				if ((!block.isTransparent()) || block.getOpacity() > 0)
				{
					if (lastSkyLight != 0)
					{
						// Remove light
						chunk.setSkyLight(blockX, blockY, blockZ, (byte) 0);
						pendingShadowRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte) lastSkyLight));
					}
				}
				else //if (!skyAvailable)
				{
					// "Remove" the light to find the closest one
					pendingShadowRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte) 0));
				}
			}
			
			// If the new block is air or not transparent or is somewhat opaque, set it up for removal
			if (block == Blocks.AIR || !block.isTransparent())
				pendingLightRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte)lastBlockLight));
			else
				pendingLightUpdates.add(new LightUpdate(new Vec3i(x, y, z), (byte)0));
		}
		
		if (updateNeighborChunks)
		{
			// Update the adjacent in the column chunks if the block is at one of the chunk edges
			if (blockY == 15)
				loadedChunks.getOrDefault(chunkPos.add(0, 1, 0), EMPTY_CHUNK).forceLayerRebuild();
			
			int limit = y - oldestHeight;
			
			for (int yPos = chunkPos.getY(); yPos >= 0; yPos--)
			{
				int yOff = yPos - chunkPos.getY();
				
				if (yOff != 0)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, 0), EMPTY_CHUNK).forceLayerRebuild();
				
				// If there was no lighting update, only update the directly
				// adjacent chunks
				if (!lightingUpdate && yOff <= -1)
					break;
				
				// TODO update corner chunks
				
				if (blockX == 0)
					loadedChunks.getOrDefault(chunkPos.add(-1, yOff, 0), EMPTY_CHUNK).forceLayerRebuild();
				else if (blockX == 15)
					loadedChunks.getOrDefault(chunkPos.add(1, yOff, 0), EMPTY_CHUNK).forceLayerRebuild();
				
				if (blockZ == 0)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, -1), EMPTY_CHUNK).forceLayerRebuild();
				else if (blockZ == 15)
					loadedChunks.getOrDefault(chunkPos.add(0, yOff, 1), EMPTY_CHUNK).forceLayerRebuild();
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
		int waterLevel = 68;
		
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
				else if ((y <= 4 && worldRandom.nextInt(8) == 0) || y == 0)
				{
					block = Blocks.PLANKS;
				}
				
				if (y > height)
				{
					// Generate water between the water level & the generated height
					block = Blocks.WATER;
				}
				
				byte flags = 0b1000;
				
				setBlock((cx << 4) + x, y, (cz << 4) + z, block, (byte)0, flags);
				
				if ((block == Blocks.WATER && y < waterLevel) || !block.isTransparent())
				{
					// Remove skylight if the height is 1 below the water level, or the block is not transparent
					getChunk(cx, y >> 4, cz).setSkyLight(x & 0xF, y & 0xF, z & 0xF, (byte) 0);
				}
				else if ((block == Blocks.WATER && y == waterLevel))
				{
					// Set the light level to the attenuated light
					getChunk(cx, y >> 4, cz).setSkyLight(x & 0xF, y & 0xF, z & 0xF, (byte) (15-block.getOpacity()));
					// Add pending sky light updates
					pendingShadowUpdates.add(new LightUpdate(new Vec3i((cx << 4) + x, y, (cz << 4) + z), (byte) 0));
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
	
	public void explode(int centreX, int centreY, int centreZ, int radius)
	{
		// Pass 1: Initial block removal
		// No updates to adjacent chunks, or nearest neighbor updates
		for (int y = -radius; y <= radius; y++)
		{
			for (int z = -radius; z <= radius; z++)
			{
				for (int x = -radius; x <= radius; x++)
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
		for (int y = -radius; y <= radius; y++)
		{
			for (int z = -radius; z <= radius; z++)
			{
				for (int x = -radius; x <= radius; x++)
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
		
		// Process the lighting updates
		processLightUpdate();
	}
	
	private void doBlockTick()
	{
		if (!pendingShadowUpdates.isEmpty() || !pendingShadowRemoves.isEmpty())
			return;
		
		// Update the loaded chunks
		List<Chunk> workingList = new ArrayList<>(loadedChunks.values());
		workingList.iterator().forEachRemaining((chunk) ->
		{
			for (int pos : chunk.tickables)
			{
				int x = (pos >> 0) & 0xF;
				int y = (pos >> 8) & 0xF;
				int z = (pos >> 4) & 0xF;
				
				Block block = Block.idToBlock(chunk.getBlock(x, y, z));
				block.onTick(this, x + chunk.chunkX * 16, y + chunk.chunkY * 16, z + chunk.chunkZ * 16);
			}
			
			// Select 24 different positions
			for (int i = 0; i < 24; i++)
			{
				int x = worldRandom.nextInt(16);
				int y = worldRandom.nextInt(16);
				int z = worldRandom.nextInt(16);
				
				Block block = Block.idToBlock(chunk.getBlock(x, y, z));
				
				if (block.isRandomlyTickable())
					block.onRandomTick(this, x + chunk.chunkX * 16, y + chunk.chunkY * 16, z + chunk.chunkZ * 16);
			}
		});
		
		// XXX: AGGGH! Refactor so that no statics are used
		BlockWater.updateWater(this);
	}
	
	private void processLightUpdate()
	{
		int procn = 0;
		
		// Remove old sky light
		while (!pendingShadowRemoves.isEmpty())
		{
			LightUpdate update = pendingShadowRemoves.poll();
			byte lastLight = update.newLight;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				byte adjacentLight = getSkyLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				//if (Game.showThings && newPos.getX() == 0 && newPos.getZ() == 0)
				//	System.out.println("("+adjacentLight+","+lastLight+")"+dir.toString()+newPos.toString()+": CA.A " + (adjacentLight != 0) + " CA.B " + (adjacentLight < lastLight) + ", CB " + (lastLight == 15 && dir == Facing.DOWN) + ", CC " + (adjacentLight >= lastLight));
				
				if ((adjacentLight != 0 && adjacentLight < lastLight)
					|| (lastLight == 15 && dir == Facing.DOWN))
				{
					// Propagate the emptiness...
					setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte)0);
					pendingShadowRemoves.add(new LightUpdate(newPos, adjacentLight));
				}
				else if (adjacentLight >= lastLight && adjacentLight > 0)
				{
					// Change to propagate, adjacent light is equal or bigger (and not zero)
					pendingShadowUpdates.add(new LightUpdate(newPos, adjacentLight));
				}
			}
			
			/*if (Game.showThings && (--procn) < 0)
				break;*/
		}
		
		procn = 1;
		
		// Propagate sky light
		while (pendingShadowRemoves.isEmpty() && !pendingShadowUpdates.isEmpty())
		{
			LightUpdate update = pendingShadowUpdates.poll();
			int x = update.pos.getX();
			int y = update.pos.getY();
			int z = update.pos.getZ();
			
			// Fetch the light value
			byte currentLight = getSkyLight(x, y, z);
			
			// Don't propagate emptiness
			if (currentLight == 0)
				continue;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				Block adjacentBlock = getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				byte adjacentSkylight = getSkyLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				byte newLight = (byte)(currentLight - Math.max(1, adjacentBlock.getOpacity()));
				
				// For horizontal spreading : spread as normal
				// For vertical spreading :
				// - If the difference between adjacent skylight & current
				//   skylight is greater than the opacity, spread
				
				// Check if the adjacent block can propagate shadow
				if (adjacentBlock.isTransparent()
						&& (adjacentSkylight + 1 <= newLight
						|| (dir == Facing.DOWN && adjacentSkylight <= newLight && newLight > 0)))
				{
					// When propagating the maximum light down, only be affected by opacity
					if (dir == Facing.DOWN && currentLight == 15)
					{
						/*if (Game.showThings)
							setBlock(newPos.getX(), newPos.getY(), newPos.getZ(), Blocks.GLASS, (byte)0, (byte)0);*/
						setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte) (currentLight - adjacentBlock.getOpacity()));
					}
					else
					{
						/*if (Game.showThings)
							setBlock(newPos.getX(), newPos.getY(), newPos.getZ(), Blocks.GLASS, (byte)0, (byte)0);*/
						setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), newLight);
					}
					
					pendingShadowUpdates.add(new LightUpdate(newPos, (byte)0));
				}
				/*else {
					if (Game.showThings)
						setBlock(newPos.getX(), newPos.getY(), newPos.getZ(), Blocks.DIRT, (byte)0, (byte)0);
				}*/
			}
			
			if (Game.showThings && (--procn) < 0)
			{
				System.out.println("size"+pendingShadowUpdates.size());
				break;
			}
		}
		
		// Remove old lights
		while (!pendingLightRemoves.isEmpty())
		{
			LightUpdate update = pendingLightRemoves.poll();
			byte lastLight = update.newLight;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				byte adjacentLight = getBlockLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				if (adjacentLight != 0 && adjacentLight < lastLight)
				{
					// Propagate the emptiness...
					setBlockLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte)0);
					pendingLightRemoves.add(new LightUpdate(newPos, adjacentLight));
				}
				else if (adjacentLight >= lastLight)
				{
					// Change to propagate, adjacent light is bigger
					pendingLightUpdates.add(new LightUpdate(newPos, (byte)0));
				}
			}
		}
		
		// Propagate light
		while (!pendingLightUpdates.isEmpty())
		{
			LightUpdate update = pendingLightUpdates.poll();
			int x = update.pos.getX();
			int y = update.pos.getY();
			int z = update.pos.getZ();
			
			// Fetch the light value
			byte currentLight = getBlockLight(x, y, z);
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				Block adjacentBlock = getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				// Allow block light to be affected by opacity
				byte newLight = (byte)(currentLight - 1);
				
				// Check if the adjacent block can propagate light
				if (adjacentBlock.isTransparent()
						&& getBlockLight(newPos.getX(), newPos.getY(), newPos.getZ()) + 1 <= newLight)
				{
					setBlockLight(newPos.getX(), newPos.getY(), newPos.getZ(), newLight);
					pendingLightUpdates.add(new LightUpdate(newPos, (byte)0));
				}
			}
		}
	}
	
	private static class LightUpdate
	{
		final Vec3i pos;
		final byte newLight;
		
		private LightUpdate(Vec3i pos, byte newLight)
		{
			this.pos = pos;
			this.newLight = newLight;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof LightUpdate))
				return false;
			
			LightUpdate other = (LightUpdate)(obj);
			return this.pos.equals(other.pos);
		}
	}
	
}
