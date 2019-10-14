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
			world.setBlock(x, y, z, Blocks.UPDATING_WATER.getId(), world.getBlockMeta(x, y, z), 0);
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
				
				Block adjacent = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
				byte adjacentMeta = world.getBlockMeta(blockX, blockY, blockZ);
				
				if (facing == Facing.DOWN && (adjacent == Blocks.AIR || adjacent.canBeReplacedBy(world, this, blockX, blockY, blockZ)))
					canConvertToSource = false;
				if (facing != Facing.DOWN && (adjacent == Blocks.WATER || adjacent == Blocks.UPDATING_WATER) && (adjacentMeta & DISTANCE) == 0)
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
			
			Block block = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
			byte newMeta = (byte)(srcMeta & DISTANCE);
			++newMeta;
			byte adjacentMeta = world.getBlockMeta(blockX, blockY, blockZ);
			
			// Flow to the adjacent dir if:
			// - The block is water & the path is smaller
			// - The block is air
			// - The block can be replaced by water
			if (((block == Blocks.WATER || block == Blocks.UPDATING_WATER) && (srcMeta & DISTANCE) <= (adjacentMeta & DISTANCE) && (newMeta & DISTANCE) < (adjacentMeta & DISTANCE))
					|| block == Blocks.AIR
					|| block.canBeReplacedBy(world, this, blockX, blockY, blockZ))
			{
				noChange = false;
				
				// Check for falling:
				int off = 1;
				if (facing == Facing.DOWN)
					off = 0;
				
				Block adjBelow = Block.idToBlock(world.getBlock(blockX, blockY - off, blockZ));
				if (adjBelow == Blocks.AIR || adjBelow == Blocks.UPDATING_WATER || adjBelow == Blocks.WATER)
					newMeta = IS_FALLING | 1;
				
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
				if (block != Blocks.AIR && block != Blocks.UPDATING_WATER && block != Blocks.WATER)
					stopFalling = true;
			}
			
			if (isFalling)
				break;
		}
		
		if (isFalling && stopFalling)
		{
			noChange = false;
			int averageLevel = 0;
			int waterCount = 0;
			
			// Find the average water level
			for (Facing dir : dirs)
			{
				if (dir == Facing.DOWN)
					continue;
				
				int adjacentX = x + dir.getOffsetX();
				int adjacentY = y + dir.getOffsetY();
				int adjacentZ = z + dir.getOffsetZ();
				
				if (!world.isChunkPresent(adjacentX >> 4, adjacentY >> 4, adjacentZ >> 4))
					continue;
				
				Block adjacent = Block.idToBlock(world.getBlock(adjacentX, adjacentY, adjacentZ));
				if (adjacent != Blocks.WATER)
					continue;
				
				byte adjacentMeta = world.getBlockMeta(adjacentX, adjacentY, adjacentZ);
				
				if ((adjacentMeta & IS_FALLING) == 0)
				{
					// Only account for things that don't fall
					waterCount++;
					averageLevel += adjacentMeta & DISTANCE;
				}
			}
			
			if (waterCount > 0)
			{
				averageLevel /= waterCount;
				++averageLevel;
				
				if (averageLevel > 7)
					averageLevel = 7;
				
				// Apply average water level and update adjacents
				world.setBlock(x, y, z, Blocks.UPDATING_WATER.getId(), (byte) (averageLevel & 0xF));
			}
			else
			{
				// Stop falling
				world.setBlockMeta(x, y, z, (byte) (srcMeta & ~IS_FALLING));
			}
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
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.WATER.getId(), nextPlace.newMeta, 0);
		}
		
		while (!availableBlocks.isEmpty())
		{
			FluidPlacement nextPlace = availableBlocks.pop();
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.UPDATING_WATER.getId(), nextPlace.newMeta, 7);
		}
	}
	
	@Override
	public boolean canBeReplacedBy(World world, Block block, int x, int y, int z)
	{
		// Water can be replaced by anything but water, unless the distance is smaller (handled above)
		return block != Blocks.WATER && block != Blocks.UPDATING_WATER;
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
		return block != Blocks.WATER && block != Blocks.UPDATING_WATER;
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
