package ddb.io.voxelnet.block;

import ddb.io.voxelnet.entity.EntityFallingBlock;
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
		// If the block below isn't air or a fluid, don't do anything
		Block block = world.getBlock(x, y - 1, z);
		if(block != Blocks.AIR && !(block instanceof BlockFluid))
			return;
		
		// Add a new falling entity
		EntityFallingBlock falling = new EntityFallingBlock(this, world.getBlockMeta(x, y, z));
		falling.setPos(x + 0.5f, y, z + 0.5f);
		
		world.addEntity(falling);
		
		// The current block will be removed by the falling entity
	}
	
}
