package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;

public class BlockGlass extends Block
{
	BlockGlass()
	{
		setSolid(true);
		setFaceTextures(new int[] {10, 10, 10, 10, 10, 10});
		setTransparent(true);
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.TRANSPARENT;
	}
	
}
