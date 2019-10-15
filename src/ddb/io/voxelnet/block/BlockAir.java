package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;

public class BlockAir extends Block
{
	
	BlockAir()
	{
		setSolid(false);
		setFaceTextures(new int[] {-1, -1, -1, -1, -1, -1});
		setTransparent(true);
		setHitBox(null);
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		// Because of the weird rendering implementation, air is "opaque"
		return RenderLayer.OPAQUE;
	}
	
}
