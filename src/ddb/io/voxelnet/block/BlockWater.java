package ddb.io.voxelnet.block;

import ddb.io.voxelnet.fluid.Fluid;

public class BlockWater extends BlockFluid
{
	BlockWater(boolean isUpdating)
	{
		super(isUpdating);
		
		if (isUpdating && false)
			setFaceTextures(new int[] {3, 3, 3, 3, 3, 3});
		else
			setFaceTextures(new int[] {13, 13, 13, 13, 13, 13});
		setTransparent(true);
	}
	
	@Override
	public Fluid getFluid()
	{
		return Fluid.WATER;
	}
}
