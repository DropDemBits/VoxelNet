package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.BlockRenderModel;
import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.World;

import java.util.Stack;

public class BlockWater extends Block
{
	// Masks
	private static final int IS_FALLING = 0b1000;
	public static final int DISTANCE = 0b0111;
	
	// XXX: BAD! Replace with something better
	public static Stack<FluidPlacement> availableBlocks = new Stack<>();
	public static Stack<FluidPlacement> makeStatic = new Stack<>();
	
	private boolean isUpdating;
	
	public BlockWater(boolean isUpdating)
	{
		this.isUpdating = isUpdating;
		
		setSolid(false);
		if (isUpdating)
			setFaceTextures(new int[] {3, 3, 3, 3, 3, 3});
		else
			setFaceTextures(new int[] {13, 13, 13, 13, 13, 13});
		setTransparent(true);
		setHitBox(null);
		setTickable(isUpdating);
	}
	
	@Override
	public void onNeighborUpdated(World world, int x, int y, int z, Facing dir)
	{
		super.onNeighborUpdated(world, x, y, z, dir);
		
		if (!isUpdating)
		{
			// Make it an updating block! (Same block, just a different state)
			world.setBlock(x, y, z, Blocks.UPDATING_WATER, world.getBlockMeta(x, y, z), 0);
		}
		
	}
	
	@Override
	public void onTick(World world, int x, int y, int z)
	{
		doWaterSpread(world, x, y, z);
	}
	
	private void doWaterSpread(World world, int x, int y, int z)
	{
		// Check surronding blocks & update metas
		final Facing[] dirs = new Facing[] { Facing.DOWN, Facing.NORTH, Facing.WEST, Facing.SOUTH, Facing.EAST, };
		
		byte srcMeta = world.getBlockMeta(x, y, z);
		boolean isFalling = (srcMeta & IS_FALLING) != 0;
		
		int adjacentSources = 0;
		boolean canConvertToSource = true;
		boolean noChange = true;
		boolean stopFalling = false;
		
		if ((srcMeta & DISTANCE) != 0)
		{
			// Check for a conversion to a source block
			
			for (Facing facing : dirs)
			{
				int blockX = x + facing.getOffsetX();
				int blockY = y + facing.getOffsetY();
				int blockZ = z + facing.getOffsetZ();
				
				Block adjacent = world.getBlock(blockX, blockY, blockZ);
				byte adjacentMeta = world.getBlockMeta(blockX, blockY, blockZ);
				
				if (facing == Facing.DOWN && (adjacent == Blocks.AIR || adjacent.canBeReplacedBy(world, this, srcMeta, blockX, blockY, blockZ)))
					// Direction is down & water can spread there, don't convert to source
					canConvertToSource = false;
				// TODO: Remember to change this when adding other fluids
				if (facing != Facing.DOWN && adjacent instanceof BlockWater && (adjacentMeta & DISTANCE) == 0)
					adjacentSources++;
			}
			
			if (!isFalling && canConvertToSource && adjacentSources > 1)
			{
				// Convert into source water
				availableBlocks.push(new FluidPlacement(new Vec3i(x, y, z), (byte) 0));
				return;
			}
		}
		
		// IF Block below is air:
		// Falling = true for next block, next block down is falling
		
		for (Facing facing : dirs)
		{
			// Don't spread outwards if falling or at max distance
			if (facing != Facing.DOWN && (srcMeta & DISTANCE) >= 7)
				continue;
			
			int blockX = x + facing.getOffsetX();
			int blockY = y + facing.getOffsetY();
			int blockZ = z + facing.getOffsetZ();
			
			if (!world.isChunkPresent(blockX >> 4, blockY >> 4, blockZ >> 4))
				continue;
			
			Block block = world.getBlock(blockX, blockY, blockZ);
			byte newMeta = (byte)(srcMeta & DISTANCE);
			++newMeta;
			
			// Cap the new distance
			if (newMeta > 7)
				newMeta = 7;
			
			byte adjacentMeta = world.getBlockMeta(blockX, blockY, blockZ);
			
			// Flow to the adjacent dir if:
			// - The block is water & the path is smaller
			//   hasn't been reached
			// - The block is air
			// - The block can be replaced by water
			
			if (((block instanceof BlockWater) && (newMeta & DISTANCE) < (adjacentMeta & DISTANCE))
					|| block == Blocks.AIR
					|| block.canBeReplacedBy(world, this, newMeta, blockX, blockY, blockZ))
			{
				noChange = false;
				
				// Check for falling:
				int off = 1;
				if (facing == Facing.DOWN)
					off = 0;
				
				Block adjBelow = world.getBlock(blockX, blockY - off, blockZ);
				
				// Make the new water fall
				if (adjBelow == Blocks.AIR || adjBelow instanceof BlockWater)
					newMeta |= IS_FALLING;
				
				if (facing == Facing.DOWN)
				{
					// Keep distance the same
					newMeta &= ~DISTANCE;
					newMeta |= (srcMeta & DISTANCE);
				}
				
				FluidPlacement newPlace = new FluidPlacement(new Vec3i(blockX, blockY, blockZ), newMeta);
				newPlace.newMeta = newMeta;
				
				if (!availableBlocks.contains(newPlace))
					availableBlocks.push(newPlace);
			}
			
			if (facing == Facing.DOWN)
			{
				// Revert to not falling
				if (block != Blocks.AIR && !(block instanceof BlockWater))
					stopFalling = true;
			}
			
			if (isFalling)
				break;
		}
		
		if (isFalling && stopFalling)
		{
			noChange = false;
			
			FluidPlacement replace = new FluidPlacement(new Vec3i(x, y, z), (byte) 0);
			Block above = world.getBlock(x, y + 1, z);
			if (!(above instanceof BlockWater))
			{
				// Restore old water level, not falling
				replace.newMeta = (byte) (srcMeta & ~IS_FALLING);
			}
			else
			{
				// Spread! (Stopped falling)
				replace.newMeta = 1;
			}
			
			if (!availableBlocks.contains(replace))
				availableBlocks.push(replace);
			
		}
		
		// Make the current fluid block static
		if (noChange)
		{
			// Add to the "make static" stack
			FluidPlacement replace = new FluidPlacement(new Vec3i(x, y, z), srcMeta);
			if (!makeStatic.contains(replace))
				makeStatic.push(replace);
		}
	}
	
	public static void updateWater(World world)
	{
		while (!makeStatic.isEmpty())
		{
			FluidPlacement nextPlace = makeStatic.pop();
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.WATER, nextPlace.newMeta, 0);
		}
		
		while (!availableBlocks.isEmpty())
		{
			FluidPlacement nextPlace = availableBlocks.pop();
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.UPDATING_WATER, nextPlace.newMeta, 7);
		}
	}
	
	@Override
	public boolean canBeReplacedBy(World world, Block block, byte newMeta, int x, int y, int z)
	{
		// Water can be replaced by anything but water, unless the distance is smaller (handled above)
		byte srcMeta = world.getBlockMeta(x, y, z);
		
		return !(block instanceof BlockWater);
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
	public boolean showFace(Block block, Facing dir)
	{
		return !(block instanceof BlockWater);
	}
	
	private class FluidPlacement
	{
		Vec3i pos;
		byte newMeta;
		
		FluidPlacement(Vec3i pos, byte newMeta)
		{
			this.pos = pos;
			this.newMeta = newMeta;
		}
		
		@Override
		public int hashCode()
		{
			return pos.hashCode();
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof FluidPlacement))
				return false;
			return pos.equals(obj);
		}
	}
	
}
