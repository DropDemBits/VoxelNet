package ddb.io.voxelnet.world;

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
		EMPTY_CHUNK = new Chunk(this, 0, 0, 0);
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
				if (cy == 3 && i / 256 == 15)
					chunk.setBlock(i % 16, i / 256, (i / 16) % 16, (byte) 1);
				else if (cy == 3 && i / 256 >= 12)
					chunk.setBlock(i % 16, i / 256, (i / 16) % 16, (byte) 2);
				else
					chunk.setBlock(i % 16, i / 256, (i / 16) % 16, (byte) 3);
			}
			
			loadedChunks.put(new Vec3i(cx, cy, cz), chunk);
		}
	}
	
	public void update()
	{
		// Go through all of the chunks
	}
}
