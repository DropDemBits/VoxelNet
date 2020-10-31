package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.BlockRenderModel;
import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;

public abstract class BlockFluid extends Block
{
	// Masks
	public static final int IS_FALLING = 0b1000;
	public static final int DISTANCE = 0b0111;
	
	BlockFluid()
	{
		setSolid(false);
		setHitBox(null);
	}
	
	@Override
	public int getOpacity()
	{
		return 1;
	}
	
	@Override
	public boolean isFilledCube()
	{
		return false;
	}
	
	@Override
	public void onBlockPlaced(World world, int x, int y, int z)
	{
		super.onBlockPlaced(world, x, y, z);
		world.getFluidInstance(getFluid()).addFluidUpdate(x, y, z);
	}
	
	@Override
	public void onNeighborUpdated(World world, int x, int y, int z, Facing dir)
	{
		super.onNeighborUpdated(world, x, y, z, dir);
		
		// Schedule a fluid update
		world.getFluidInstance(getFluid()).addFluidUpdate(x, y, z);
	}
	
	@Override
	public boolean canBeReplacedBy(World world, Block block, int newMeta, int x, int y, int z)
	{
		// A fluid can be replaced by anything but itself, unless the distance is smaller (handled above)
		return !isSameFluid(block);
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.FLUID;
	}
	
	@Override
	public BlockRenderModel getRenderModel()
	{
		return BlockRenderModel.FLUID;
	}
	
	@Override
	public boolean showFace(Block adjacent, Facing dir)
	{
		if (dir == Facing.UP)
			return !getFluid().isSameFluid(adjacent);
		
		return !getFluid().isSameFluid(adjacent) && (!adjacent.isSolid() || adjacent.isTransparent());
	}
	
	public boolean isSameFluid(Block block)
	{
		return getFluid().isSameFluid(block);
	}
	
	public abstract Fluid getFluid();
	
}
