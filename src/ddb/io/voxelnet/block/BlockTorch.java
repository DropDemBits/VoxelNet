package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.BlockRenderModel;
import ddb.io.voxelnet.client.render.RenderLayer;

public class BlockTorch extends Block
{
	
	BlockTorch()
	{
		setFaceTextures(new int[] { 14, 14, 14, 14, 14, 14, });
		setTransparent(true);
	}
	
	@Override
	public boolean isSolid()
	{
		return true;
	}
	
	@Override
	public boolean isFilledCube()
	{
		return false;
	}
	
	@Override
	public BlockRenderModel getRenderModel()
	{
		return BlockRenderModel.CUBE;
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
	
}
