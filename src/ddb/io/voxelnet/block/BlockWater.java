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
	public static Stack<FluidPlacement> makeClear = new Stack<>();
	
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
		
		Block[] adjacentBlocks = new Block[Facing.values().length];
		byte[] adjacentMetas = new byte[Facing.values().length];
		
		// Build the adjacent block / meta arrays
		for (Facing dir : Facing.values())
		{
			int adjacentX = x + dir.getOffsetX();
			int adjacentY = y + dir.getOffsetY();
			int adjacentZ = z + dir.getOffsetZ();
			
			adjacentBlocks[dir.ordinal()] = world.getBlock(adjacentX, adjacentY, adjacentZ);
			adjacentMetas[dir.ordinal()] = world.getBlockMeta(adjacentX, adjacentY, adjacentZ);
		}
		
		byte srcMeta = world.getBlockMeta(x, y, z);
		boolean isFalling = (srcMeta & IS_FALLING) != 0;
		int srcLevel = srcMeta & DISTANCE;
		
		int adjacentSources = 0;
		boolean canConvertToSource = true;
		boolean noChange = true;
		boolean stopFalling = false;
		
		if ((srcMeta & DISTANCE) != 0)
		{
			int inflows = 0;
			int outflows = 0;
			
			// Check for a conversion to a source block, and count the number of inflows
			for (Facing dir : Facing.values())
			{
				Block adjacentBlock = adjacentBlocks[dir.ordinal()];
				int adjacentLevel = adjacentMetas[dir.ordinal()] & DISTANCE;
				
				// Skip non-water blocks
				if (!(adjacentBlock instanceof BlockWater))
					continue;
				
				int flow = adjacentLevel - srcLevel;
				
				// Don't check for inflows when going down
				// Is inflow if flow is less than 0 or if there is a block of water above
				if (dir != Facing.DOWN)
				{
					if (flow < 0 || dir == Facing.UP)
						++inflows;
				}
				
				if (dir != Facing.UP)
				{
					if (flow >= 0 || dir == Facing.DOWN)
						++outflows;
				}
				
				// Skip the up direction beyond calculating inflows
				if (dir == Facing.UP)
					continue;
				
				int adjacentX = x + dir.getOffsetX();
				int adjacentY = y + dir.getOffsetY();
				int adjacentZ = z + dir.getOffsetZ();
				
				if (dir == Facing.DOWN && (adjacentBlock == Blocks.AIR || adjacentBlock.canBeReplacedBy(world, this, srcMeta, adjacentX, adjacentY, adjacentZ)))
					// Direction is down & water can spread there, don't convert to source
					canConvertToSource = false;
				// TODO: Remember to change this when adding other fluids
				if (dir != Facing.DOWN && adjacentLevel == 0)
					// Direction is one of the cardinals and it's a source block
					adjacentSources++;
			}
			
			if (!isFalling && canConvertToSource && adjacentSources > 1)
			{
				// Convert into source water
				availableBlocks.push(new FluidPlacement(new Vec3i(x, y, z), (byte) 0));
				return;
			}
			
			// Can't convert into source, check inflow count
			if (inflows == 0)
			{
				// No inflows, begin drainage
				int drainRate = 1;
				
				// If there is more than one outflow or it is falling, increase the drain rate
				if (outflows > 1 || isFalling)
					drainRate = 2;
				
				byte newMeta = (byte) (srcLevel + drainRate);
				
				if (newMeta > 7)
					newMeta = 7;
				
				if (isFalling)
					newMeta |= IS_FALLING;
				
				FluidPlacement newPlace = new FluidPlacement(new Vec3i(x, y, z), newMeta);
				
				if ((srcLevel + drainRate) > 7)
				{
					// Dry up
					if (!makeClear.contains(newPlace))
						makeClear.push(newPlace);
				}
				else
				{
					// Gradually disappear
					if (!availableBlocks.contains(newPlace))
						availableBlocks.push(newPlace);
				}
				return;
			}
		}
		
		// IF Block below is air:
		// Falling = true for next block, next block down is falling
		
		for (Facing dir : dirs)
		{
			// Don't spread outwards if falling or at max distance
			if (dir != Facing.DOWN && (srcMeta & DISTANCE) >= 7)
				continue;
			
			int adjacentX = x + dir.getOffsetX();
			int adjacentY = y + dir.getOffsetY();
			int adjacentZ = z + dir.getOffsetZ();
			
			/*if (!world.isChunkPresent(adjacentX >> 4, adjacentY >> 4, adjacentZ >> 4))
				continue;*/
			
			Block adjacentBlock = adjacentBlocks[dir.ordinal()];
			int adjacentLevel = adjacentMetas[dir.ordinal()] & DISTANCE;
			
			int newLevel = srcLevel + 1;
			
			// Cap the new distance
			if (newLevel > 7)
				newLevel = 7;
			
			// Flow to the adjacent dir if:
			// - The block is water & the path is smaller
			//   hasn't been reached
			// - The block is air
			// - The block can be replaced by water
			
			if (dir == Facing.DOWN)
			{
				// Revert to not falling if there's not air or water below this block
				if (adjacentBlock != Blocks.AIR && !(adjacentBlock instanceof BlockWater))
					stopFalling = true;
			}
			
			// TODO: Fix water not replacing with the shortest path while falling
			if (((adjacentBlock instanceof BlockWater) && (newLevel < adjacentLevel))
					|| adjacentBlock == Blocks.AIR
					|| adjacentBlock.canBeReplacedBy(world, this, (byte) newLevel, adjacentX, adjacentY, adjacentZ))
			{
				noChange = false;
				
				// Check for falling:
				int yOff = 1;
				if (dir == Facing.DOWN)
					yOff = 0;
				
				Block adjBelow = world.getBlock(adjacentX, adjacentY - yOff, adjacentZ);
				
				// Make the new water fall
				byte newMeta = (byte)newLevel;
				
				if (adjBelow == Blocks.AIR || adjBelow instanceof BlockWater)
					newMeta |= IS_FALLING;
				
				if (dir == Facing.DOWN && srcLevel > 0)
				{
					// Keep distance the same (unless it's a source block)
					newMeta &= ~DISTANCE;
					newMeta |= (srcMeta & DISTANCE);
				}
				
				FluidPlacement newPlace = new FluidPlacement(new Vec3i(adjacentX, adjacentY, adjacentZ), newMeta);
				newPlace.newMeta = newMeta;
				
				if (!availableBlocks.contains(newPlace))
					availableBlocks.push(newPlace);
			}
			
			if (isFalling)
				break;
		}
		
		if (isFalling && stopFalling)
		{
			noChange = false;
			
			Block above = adjacentBlocks[Facing.UP.ordinal()];
			
			if ((above instanceof BlockWater))
			{
				// Spread! (Stopped falling)
				world.setBlockMeta(x, y, z, (byte) 1);
			}
			else
			{
				// Restore old water level, not falling
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
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.WATER, nextPlace.newMeta, 0);
		}
		
		while (!makeClear.isEmpty())
		{
			FluidPlacement nextPlace = makeClear.pop();
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.AIR, (byte) 0, 7);
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
