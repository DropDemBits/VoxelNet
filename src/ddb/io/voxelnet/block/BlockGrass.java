package ddb.io.voxelnet.block;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;

public class BlockGrass extends Block
{
	BlockGrass()
	{
		setSolid(true);
		setFaceTextures(new int[] {1, 1, 1, 1, 0, 2});
	}
	
	@Override
	public boolean isRandomlyTickable()
	{
		return true;
	}
	
	@Override
	public void onRandomTick(World world, int x, int y, int z)
	{
		if (world.getBlock(x, y + 1, z) != Blocks.AIR)
		{
			// Bye grass
			world.setBlock(x, y, z, Blocks.DIRT);
		}
	}
}
