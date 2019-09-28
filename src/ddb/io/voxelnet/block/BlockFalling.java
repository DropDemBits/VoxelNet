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
		// If the block below isn't air or water, don't do anything
		Block block = Block.idToBlock(world.getBlock(x, y - 1, z));
		if(block != Blocks.AIR && block != Blocks.WATER)
			return;
		
		// Set the old position to air
		world.setBlock(x, y, z, Blocks.AIR.getId());
		
		// Add a new falling entity
		EntityFallingBlock falling = new EntityFallingBlock(this);
		falling.setPos(x + 0.5f, y, z + 0.5f);
		
		world.addEntity(falling);
	}
	
}
