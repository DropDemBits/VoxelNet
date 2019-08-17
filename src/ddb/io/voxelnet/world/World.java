package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.Vec3i;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class World
{
	// Map of currently loaded chunks
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
	// Highest, opaque block in each chunk column (vertical group of chunks)
	// Accessed by index = x + z * 16
	// TODO: Add Vec2i
	private final Map<Vec3i, ChunkColumn> chunkColumns = new LinkedHashMap<>();
	private final Chunk EMPTY_CHUNK;
	private Random worldRandom;
	
	public World()
	{
		EMPTY_CHUNK = new Chunk(this, 0, -64, 0);
		worldRandom = new Random(System.currentTimeMillis());
		generate();
	}
	
	/**
	 * Generates a set of chunks
	 */
	private void generate()
	{
		for (int cx = -8; cx <= 7; cx++)
		{
			for (int cz = -8; cz <= 7; cz++)
			{
				generateChunk(cx, cz);
			}
		}
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
		
		/*if (baseLight < 15)
			baseLight = 3;*/
		
		return baseLight;
	}
	
	/**
	 * Gets the block at the specified position
	 * If the position is not inside of a loaded chunk, air (0) is returned
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @return The block id of the specified block, or Blocks.AIR.getId() if
	 * the position is out of bounds
	 */
	public byte getBlock (int x, int y, int z)
	{
		if (y < 0)
			return Blocks.AIR.getId();
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		if (!loadedChunks.containsKey(chunkPos))
			return Blocks.AIR.getId();
		
		return loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getBlock(x & 0xF, y & 0xF, z & 0xF);
	}
	
	/**
	 * Sets the block id at the given position
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param id The id of the block
	 */
	public void setBlock (int x, int y, int z, byte id)
	{
		// Don't set block below or above the world
		if (y < 0 || y > 255)
			return;
		
		// Do nothing for this id
		if (id == -1)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK);
		
		if (chunk == EMPTY_CHUNK && id != 0)
		{
			// Add a new chunk if the id is not zero
			chunk = new Chunk(this, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
			loadedChunks.put(chunkPos, chunk);
		}
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Set the block
		chunk.setBlock(blockX, blockY, blockZ, id);
		
		// Update the chunk column
		Vec3i columnPos = new Vec3i(x >> 4, 0, z >> 4);
		ChunkColumn chunkColumn = chunkColumns.getOrDefault(columnPos, null);
		
		if (chunkColumn == null)
		{
			if (id == 0)
				return; // Don't need to add a column if air is being placed
			
			// Add a new chunk column if the block isn't air
			chunkColumn = new ChunkColumn(columnPos.getX(), columnPos.getZ());
			chunkColumns.put(columnPos, chunkColumn);
		}
		
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
		
		Block block = Block.idToBlock(id);
		
		boolean performSearch = false;
		boolean lightingUpdate = false;
		
		if (y > tallestOpaque)
		{
			// Only do update for opaque blocks
			if(!block.isTransparent())
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
				if(!Block.idToBlock(getBlock(x, height, z)).isTransparent())
					break;
			}
			
			// If the height is the same, make the column empty
			if (height == y)
				height = 0;
			
			chunkColumn.opaqueColumns[columnIdx] = (byte)height;
			lightingUpdate = true;
		}
		
		// Update the adjacent in the column chunks if the block is at one of the chunk edges
		if (blockY == 15)
			loadedChunks.getOrDefault(chunkPos.add( 0,  1,  0), EMPTY_CHUNK).makeDirty();
		
		for(int yPos = chunkPos.getY(); yPos >= 0; yPos--)
		{
			int yOff = yPos - chunkPos.getY();
			
			if (yOff != 0)
				loadedChunks.getOrDefault(chunkPos.add(0, yOff, 0), EMPTY_CHUNK).makeDirty();
			
			// If there was no lighting update, only update the directly
			// adjacent chunks
			if (!lightingUpdate && yOff < -1)
				break;
			
			if (blockX == 0)
				loadedChunks.getOrDefault(chunkPos.add(-1, yOff, 0), EMPTY_CHUNK).makeDirty();
			else if (blockX == 15)
				loadedChunks.getOrDefault(chunkPos.add(1, yOff, 0), EMPTY_CHUNK).makeDirty();
			
			if (blockZ == 0)
				loadedChunks.getOrDefault(chunkPos.add(0, yOff, -1), EMPTY_CHUNK).makeDirty();
			else if (blockZ == 15)
				loadedChunks.getOrDefault(chunkPos.add(0, yOff, 1), EMPTY_CHUNK).makeDirty();
		}
	}
	
	public Chunk getChunk(int cx, int cy, int cz)
	{
		Vec3i pos = new Vec3i(cx, cy, cz);
		return loadedChunks.getOrDefault(pos, EMPTY_CHUNK);
	}
	
	/**
	 * Generates a chunk column at the specified chunk position
	 * @param cx The x position of the new chunk column
	 * @param cz The z position of the new chunk column
	 */
	private void generateChunk (int cx, int cz)
	{
		// Make the chunk columns
		ChunkColumn column = new ChunkColumn(cx, cz);
		chunkColumns.put(new Vec3i(cx, 0, cz), column);
		
		for(int cy = 3; cy >= 0; cy--)
		{
			Chunk chunk = new Chunk(this, cx, cy, cz);
			loadedChunks.put(new Vec3i(cx, cy, cz), chunk);
			
			// Fill the chunk
			for (int i = 0; i < chunk.getData().length; i++)
			{
				int x = i & 0xF;
				int y = 15 - (i >> 8);
				int z = (i >> 4) & 0xF;
				
				int blockY = (cy << 4) + y;
				
				Block block = Blocks.PLANKS;
				
				if (blockY == 63)
					block = Blocks.GRASS;
				else if (blockY >= 60)
					block = Blocks.DIRT;
				else if (blockY >= 4)
					block = Blocks.STONE;
				else if (worldRandom.nextInt(4) == 0)
					block = Blocks.PLANKS;
				else
					block = Blocks.STONE;
				
				chunk.setBlock(x, y, z, block.getId());
				
				// Update the respective column so that the lighting is correct
				int colIdx = x + (z << 4);
				if (!block.isTransparent() && column.opaqueColumns[colIdx] < blockY)
					column.opaqueColumns[colIdx] = (byte)blockY;
			}
		}
	}
	
	public void update()
	{
		int dirtyCount = 0;
		
		// TODO: REM TO REMOVE THIS FROM TESTING
		// Go through all of the chunks
		/*for(Chunk chunk : loadedChunks.values())
		{
			if (worldRandom.nextInt(4) == 0)
				chunk.makeDirty();
			if(++dirtyCount >= 8)
				break;
		}*/
	}
}
