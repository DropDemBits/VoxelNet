package ddb.io.voxelnet.block;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;

public class BlockFalling extends Block
{
	@Override
	public void onNeighborUpdated(World world, int x, int y, int z, Facing dir)
	{
		super.onNeighborUpdated(world, x, y, z, dir);
		doFalling(world, x, y, z);
	}
	
	@Override
	public void onBlockPlaced(World world, int x, int y, int z)
	{
		super.onBlockPlaced(world, x, y, z);
		doFalling(world, x, y, z);
	}
	
	private void doFalling(World world, int x, int y, int z)
	{
		// If the block below isn't air, don't do anything
		if(world.getBlock(x, y - 1, z) != Blocks.AIR.getId())
			return;
		
		int newY = y - 1;
		
		// Find the lowest block position that has air above it
		for (; newY >= 0; newY--)
		{
			if(world.getBlock(x, newY, z) != Blocks.AIR.getId())
				break;
		}
		
		// If the new position is above the world, set it
		if (newY >= 0)
			world.setBlock(x, newY + 1, z, getId());
		
		// Set the old position to air
		world.setBlock(x, y, z, Blocks.AIR.getId());
	}
	
}
