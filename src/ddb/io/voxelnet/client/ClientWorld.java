package ddb.io.voxelnet.client;

import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.world.ChunkColumn;
import ddb.io.voxelnet.world.World;

/**
 * Client side version of a world
 */
public class ClientWorld extends World
{
	// The radius of chunks to load into the chunk manager's cache
	public static int loadRadius = 5;
	
	// The client player
	EntityPlayer clientPlayer;
	
	// Next time for pruning
	private long nextPruneTime = 0;
	// Prune every 40 seconds
	private final long pruneInterval = 40 * 1000L;
	
	public ClientWorld()
	{
		super(true);
	}
	
	/**
	 * Associate the client world with the given player
	 * @param player The player to associate with
	 */
	public void associateWith(EntityPlayer player)
	{
		this.clientPlayer = player;
	}
	
	@Override
	public void update(float delta)
	{
		int cxOrigin = (int)(clientPlayer.xPos) >> 4;
		int czOrigin = (int)(clientPlayer.zPos) >> 4;
		
		// Check if any of the chunk columns in the loaded radius are loaded
		for (int cx = -loadRadius; cx <= loadRadius; cx++)
		{
			for (int cz = -loadRadius; cz <= loadRadius; cz++)
			{
				if (!chunkManager.isColumnLoaded(cxOrigin + cx, czOrigin + cz))
					chunkManager.loadColumn(cxOrigin + cx, czOrigin + cz);
			}
		}
		
		// Mark columns for unload
		for (ChunkColumn column : chunkManager.chunkColumns.values())
		{
			int unloadDistance = loadRadius;
			
			// If the chunk is inside of the square distance of the load radius, plus some buffer of 3 extra chunks,
			// mark for preservation
			if (Math.abs(column.columnX - cxOrigin) <= unloadDistance
			 && Math.abs(column.columnZ - czOrigin) <= unloadDistance)
				chunkManager.markColumnForKeep(column);
			else
				chunkManager.markColumnForUnload(column);
		}
		
		// Prune chunks every `pruneInterval` seconds
		if (nextPruneTime < System.currentTimeMillis())
		{
			nextPruneTime = System.currentTimeMillis() + pruneInterval;
			chunkManager.pruneChunks();
		}
		
		// Update the client version of the world
		super.update(delta);
	}
}
