package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;

public class BlockAir extends Block
{
	
	BlockAir()
	{
		setFaceTextures(new int[] {-1, -1, -1, -1, -1, -1});
		setHitBox(null);
	}
	
	@Override
	public boolean isSolid()
	{
		return false;
	}
	
	@Override
	public boolean isTransparent()
	{
		return true;
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		// Because of the weird rendering implementation, air is "opaque"
		// TODO: Make render layers make more sense
		return RenderLayer.OPAQUE;
	}
	
	@Override
	public int getOpacity()
	{
		return 0;
	}
}
