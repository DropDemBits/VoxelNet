package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.Vec3i;

import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class World
{
	// Map of currently loaded chunks
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
	// Highest, opaque block in each chunk column (vertical group of chunks)
	// Accessed by index = x + z * 16
	private final Map<Vec3i, ChunkColumn> chunkColumns = new LinkedHashMap<>();
	private final Chunk EMPTY_CHUNK;
	
	public World()
	{
		EMPTY_CHUNK = new Chunk(this, 0, -64, 0);
		generate();
	}
	
	/**
	 * Generates a set of chunks
	 */
	private void generate()
	{
		for (int cx = -2; cx <= 2; cx++)
		{
			for (int cz = -2; cz <= 2; cz++)
			{
				generateChunk(cx, cz);
			}
		}
	}
	
	// Working on: Lighting
	public byte getBlockLight(int x, int y, int z)
	{
		if (y < 0)
			return 0;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		byte baseLight = 0;
		
		if (loadedChunks.containsKey(chunkPos))
			baseLight = loadedChunks.getOrDefault(chunkPos, EMPTY_CHUNK).getBlockLight(x & 0xF, y & 0xF, z & 0xF);
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkColumns.getOrDefault(new Vec3i(x >> 4, 0, z >> 4), null);
		boolean canSeeSky = false;
		
		if (column == null)
		{
			// If a column is missing, the blocks can definitely see the sky
			canSeeSky = true;
		}
		else
			canSeeSky = y >= Byte.toUnsignedInt(column.opaqueColumns[(x & 0xF) + (z & 0xF) * 16]);
		
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
			System.out.println("NuChunk " + chunkPos.toString());
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
			System.out.println("NuColumn " + columnPos.toString());
		}
		
		// Update the appropriate column
		int columnIdx = blockX + blockZ * 16;
		//int tallestBlock  = Byte.toUnsignedInt(chunkColumn.blockColumns[columnIdx]);
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
		
		if (y < tallestOpaque) {} // Do nothing, will never become the tallest opaque
		else if (y > tallestOpaque)
		{
			// Only do update for opaque blocks
			if(!block.isTransparent())
			{
				if (Game.showThings)
					System.out.println("Tallest block: " + y + " @ (" + x + ", " + z + ")");
				chunkColumn.opaqueColumns[columnIdx] = (byte) y;
				lightingUpdate = true;
			}
			else
			{
				if(Game.showThings)
					System.out.println("Searching...");
				performSearch = true;
			}
		}
		
		if (y == tallestOpaque && block.isTransparent() || performSearch)
		{
			// Only update on same height if the block is not opaque or a search should be performed
			int height = y - 1;
			
			// If a search was requested, start from the top
			if (performSearch)
				height = 255;
			
			for (; height != y; height = (--height) & 0xFF)
			{
				if(!Block.idToBlock(getBlock(x, height, z)).isTransparent())
					break;
			}
			
			// If the height is the same, make the column empty
			if (height == y)
				height = 0;
			
			if (Game.showThings)
				System.out.println("Tallest block: " + height + " @ (" + x + ", " + z + ")");
			
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
	public void generateChunk (int cx, int cz)
	{
		// Make the chunk columns
		chunkColumns.put(new Vec3i(cx, 0, cz), new ChunkColumn(cx, cz));
		
		for(int cy = 0; cy < 8; cy++)
		{
			Chunk chunk = new Chunk(this, cx, cy, cz);
			
			// Fill the chunk
			for (int i = 0; i < chunk.getData().length; i++)
			{
				int x = i & 0xF;
				int y = i >> 8;
				int z = (i >> 4) & 0xF;
				
				if (cy == 7 && y == 15)
					chunk.setBlock(x, y, z, (byte) 1);
				else if (cy == 7 && y >= 12)
					chunk.setBlock(x, y, z, (byte) 2);
				else
					chunk.setBlock(x, y, z, (byte) 3);
			}
			
			loadedChunks.put(new Vec3i(cx, cy, cz), chunk);
		}
	}
	
	public void update()
	{
		// Go through all of the chunks
	}
}
