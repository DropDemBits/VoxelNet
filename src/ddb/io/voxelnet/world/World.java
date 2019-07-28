package ddb.io.voxelnet.world;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class World
{
	public final Map<Vec3i, Chunk> loadedChunks = new LinkedHashMap<>();
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
		for (int cx = -4; cx <= 4; cx++)
		{
			for (int cz = -4; cz <= 4; cz++)
			{
				generateChunk(cx, cz);
			}
		}
	}
	
	/**
	 * Gets the block at the specified position
	 * If the position is not inside of a loaded chunk, air (0) is returned
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @return The block id of the specified block, or 0 if the position is
	 * out of bounds
	 */
	public byte getBlock (int x, int y, int z)
	{
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		if (!loadedChunks.containsKey(chunkPos))
			return 0;
		
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
		
		if (blockX == 0 || blockX == 15)
			System.out.println("X UPD!");
		if (blockY == 0 || blockY == 15)
			System.out.println("Y UPD!");
		if (blockZ == 0 || blockZ == 15)
			System.out.println("Z UPD!");
		
		// Update the adjacent chunks if the block is at one of the chunk edges
		if (blockX == 0)
			loadedChunks.getOrDefault(chunkPos.add(-1,  0,  0), EMPTY_CHUNK).makeDirty();
		else if (blockX == 15)
			loadedChunks.getOrDefault(chunkPos.add( 1,  0,  0), EMPTY_CHUNK).makeDirty();
		
		if (blockY == 0)
			loadedChunks.getOrDefault(chunkPos.add( 0, -1,  0), EMPTY_CHUNK).makeDirty();
		else if (blockY == 15)
			loadedChunks.getOrDefault(chunkPos.add( 0,  1,  0), EMPTY_CHUNK).makeDirty();
		
		if (blockZ == 0)
			loadedChunks.getOrDefault(chunkPos.add( 0,  0,  -1), EMPTY_CHUNK).makeDirty();
		else if (blockZ == 15)
			loadedChunks.getOrDefault(chunkPos.add( 0,  0,   1), EMPTY_CHUNK).makeDirty();
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
		for(int cy = 0; cy < 4; cy++)
		{
			Chunk chunk = new Chunk(this, cx, cy, cz);
			
			// Fill the chunk
			for (int i = 0; i < chunk.getData().length; i++)
			{
				int x = i & 0xF;
				int y = i >> 8;
				int z = (i >> 4) & 0xF;
				
				if (cy == 3 && y == 15)
					chunk.setBlock(x, y, z, (byte) 1);
				else if (cy == 3 && y >= 12)
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
