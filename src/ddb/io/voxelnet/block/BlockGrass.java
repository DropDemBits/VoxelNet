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
		if (!canGrowUnder(world.getBlock(x, y + 1, z)))
		{
			// Bye grass
			world.setBlock(x, y, z, Blocks.DIRT);
			return;
		}
		
		// 1/4 chance of doing a spread
		if (world.worldRandom.nextFloat() > 0.25)
			return;
		
		for (Facing dir : Facing.CARDINAL_FACES)
		{
			if (world.getBlock(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ()) == Blocks.DIRT)
			{
				if (canGrowUnder(world.getBlock(x + dir.getOffsetX(), y + dir.getOffsetY() + 1, z + dir.getOffsetZ())))
				{
					// Found dirt block, spread to it
					world.setBlock(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ(), Blocks.GRASS);
					
					// Only spread once
					break;
				}
			}
		}
	}
	
	public static boolean canGrowUnder(Block block)
	{
		return block.isTransparent() && block.getOpacity() == 0;
	}
}
