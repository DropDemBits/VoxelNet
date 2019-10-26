package ddb.io.voxelnet.block;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;

public class BlockDirt extends Block
{
	
	BlockDirt()
	{
		setSolid(true);
		setFaceTextures(new int[] {2, 2, 2, 2, 2, 2});
	}
	
	@Override
	public boolean isRandomlyTickable()
	{
		return true;
	}
	
	@Override
	public void onRandomTick(World world, int x, int y, int z)
	{
		// 1/4 chance of receiving a spread
		if (world.worldRandom.nextFloat() > 0.25
				|| world.getBlock(x, y + 1, z) != Blocks.AIR)
			return;
		
		for (Facing dir : Facing.CARDINAL_FACES)
		{
			if (world.getBlock(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ()) == Blocks.GRASS)
			{
				// Spread randomly
				if (world.getBlock(x + dir.getOffsetX(), y + dir.getOffsetY() + 1, z + dir.getOffsetZ()) == Blocks.AIR)
				{
					world.setBlock(x, y, z, Blocks.GRASS);
					
					// Only spread once
					break;
				}
			}
		}
	}
	
}
