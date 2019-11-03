package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.BlockRenderModel;
import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.Facing;

public class BlockTorch extends Block
{
	
	BlockTorch()
	{
		setFaceTextures(new int[] { 14, 14, 14, 14, 14, 14, });
		setTransparent(true);
		setHitBox(new AABBCollider(7/16f, 0f, 7/16f, 2/16f, 9/16f, 2/16f));
	}
	
	@Override
	public boolean isSolid()
	{
		return false;
	}
	
	@Override
	public boolean isFilledCube()
	{
		return false;
	}
	
	@Override
	public BlockRenderModel getRenderModel()
	{
		return BlockRenderModel.TORCH;
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.TRANSPARENT;
	}
	
	@Override
	public byte getBlockLight()
	{
		return 14;
	}
	
	@Override
	public boolean showFace(Block adjacent, Facing face)
	{
		return face != Facing.DOWN || adjacent.isTransparent();
	}
}
