package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.fluid.Fluid;

public class BlockLava extends BlockFluid
{
	BlockLava()
	{
		setFaceTextures(new int[] {15, 15, 15, 15, 15, 15});
		setTransparent(true);
	}
	
	@Override
	public byte getBlockLight()
	{
		return 14;
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.OPAQUE;
	}
	
	@Override
	public Fluid getFluid()
	{
		return Fluid.LAVA;
	}
}
