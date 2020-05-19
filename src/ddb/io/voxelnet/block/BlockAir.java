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
		// Keep air on the opaque layer (blocks changing to air force a rebuild of all layers)
		return RenderLayer.OPAQUE;
	}
	
	@Override
	public int getOpacity()
	{
		return 0;
	}
}
