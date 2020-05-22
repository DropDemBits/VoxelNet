package ddb.io.voxelnet.client;

import ddb.io.voxelnet.entity.EntityPlayer;
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
		// TODO: Check if chunks are still within the loaded radius & unload if not
		// Check if any of the chunk columns in the loaded radius are loaded
		for (int cx = -loadRadius; cx <= loadRadius; cx++)
		{
			for (int cz = -loadRadius; cz <= loadRadius; cz++)
			{
				int cxOrigin = (int)(clientPlayer.xPos / 16f);
				int czOrigin = (int)(clientPlayer.zPos / 16f);
				
				if (!chunkManager.isColumnLoaded(cxOrigin + cx, czOrigin + cz))
					chunkManager.loadColumn(cxOrigin + cx, czOrigin + cz);
			}
		}
		
		// Update the client version of the world
		super.update(delta);
	}
}
