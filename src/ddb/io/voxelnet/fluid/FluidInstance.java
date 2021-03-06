package ddb.io.voxelnet.fluid;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockFluid;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.World;

import java.util.Stack;

/**
 * Per-world instance of a fluid
 *
 * As part of the process, fluid placements are done as soon as possible
 */
public class FluidInstance
{
	private static final Facing[] flowDirs = new Facing[] { Facing.DOWN, Facing.NORTH, Facing.WEST, Facing.SOUTH, Facing.EAST, };
	
	Fluid fluid;
	
	private Stack<Vec3i> pendingUpdates;
	private Stack<Vec3i> processingUpdates;
	
	/**
	 * Creates a new fluid instance
	 * @param fluid The fluid to simulate
	 */
	public FluidInstance (Fluid fluid)
	{
		this.fluid = fluid;
		
		pendingUpdates = new Stack<>();
		processingUpdates = new Stack<>();
	}
	
	/**
	 * Schedules a position to perform the fluid update
	 * @param x The x position of the update
	 * @param y The y position of the update
	 * @param z The z position of the update
	 */
	public void addFluidUpdate(int x, int y, int z)
	{
		addFluidUpdate(new Vec3i(x, y, z));
	}
	
	/**
	 * Schedules a position to perform the fluid update
	 * @param pos The position of the update
	 */
	public void addFluidUpdate(Vec3i pos)
	{
		if (pendingUpdates.contains(pos))
			return;
		pendingUpdates.add(pos);
	}
	
	/**
	 * Performs a single fluid tick
	 * @param world The world to perform the fluid tick in
	 */
	public void doFluidTick(World world)
	{
		if (processingUpdates.isEmpty())
		{
			// Swap the lists (processing updates should be empty)
			Stack<Vec3i> temp = processingUpdates;
			processingUpdates = pendingUpdates;
			pendingUpdates = temp;
		}
		
		// Process the fluid updates
		while (!processingUpdates.isEmpty())
		{
			Vec3i pos = processingUpdates.pop();
			
			// Skip blocks that aren't the same fluid
			if (!getFluid().isSameFluid(world.getBlock(pos.getX(), pos.getY(), pos.getZ())))
				continue;
			
			doFluidSpread(world, pos);
		}
	}
	
	private void doFluidSpread(World world, Vec3i pos)
	{
		int currentMeta = world.getBlockMeta(pos.getX(), pos.getY(), pos.getZ());
		boolean isFalling = (currentMeta & BlockFluid.IS_FALLING) != 0;
		int currentDistance = currentMeta & BlockFluid.DISTANCE;
		int newDistance;
		
		int adjacentSources = 0;
		int inFlows = 0, outFlows = 0;
		boolean canFormSource = getFluid().canFormSources;
		
		// Compute the number of inflows & outflows
		for (Facing dir : Facing.directions())
		{
			Vec3i adjPos = pos.add(dir);
			
			Block adjacentBlock = world.getBlock(adjPos.getX(), adjPos.getY(), adjPos.getZ());
			int adjacentMeta = world.getBlockMeta(adjPos.getX(), adjPos.getY(), adjPos.getZ());
			int adjacentDistance = adjacentMeta & BlockFluid.DISTANCE;
			
			// Skip fluids that aren't the same
			if (!getFluid().isSameFluid(adjacentBlock))
				continue;
			
			int flow = adjacentDistance - currentDistance;
			
			// Don't check for inflows when going down
			// Is inflow if flow is less than 0 or if there is a block of water above
			if (dir != Facing.DOWN)
			{
				if (flow < 0 || dir == Facing.UP)
					++inFlows;
			}
			
			if (dir != Facing.UP)
			{
				if (flow >= 0 || dir == Facing.DOWN)
					++outFlows;
			}
			
			// Skip the up direction beyond calculating inflows
			if (dir == Facing.UP)
				continue;
			
			// Don't check for source checks if the fluid can't form sources
			if (!getFluid().canFormSources)
				continue;
			
			if (dir == Facing.DOWN && adjacentBlock == Blocks.AIR)
				// Direction is down & water can spread there, don't convert to source
				canFormSource = false;
			if (dir != Facing.DOWN && adjacentDistance == 0)
				// Direction is one of the cardinals and it's a source block
				adjacentSources++;
		}
		
		// Don't convert source blocks into themselves
		if (currentDistance > 0 && !isFalling && canFormSource && adjacentSources > 1)
		{
			// If there is at least one nearby source, the current fluid
			// isn't falling, and a conversion to a source can be done,
			// Convert into source fluid
			world.setBlock(pos.getX(), pos.getY(), pos.getZ(), getFluid().staticFluid, 0);
			
			// Update the current distance & new distance
			currentDistance = 0;
		}
		
		// If there are no inflows, and the fluid isn't a source fluid,
		// begin drainage
		if ((isFalling || currentDistance > 0) && inFlows == 0)
		{
			// No inflows, begin drainage
			int drainRate = 1;
			
			// If there is more than one outflow, increase the drain rate
			if (outFlows > 2)
				drainRate = 2;
			
			int newMeta = (currentDistance + drainRate);
			
			// Apply drain to get effective current distance
			currentDistance += drainRate;
			
			if (currentDistance > getFluid().maxSpread || isFalling)
			{
				// Dry up if the new distance is larger than the max spread,
				// or is falling
				world.setBlock(pos.getX(), pos.getY(), pos.getZ(), Blocks.AIR, 0, 7);
				
				// If not falling, don't spread out
				if (!isFalling)
					return;
			}
			else
			{
				// Gradually disappear
				addFluidUpdate(pos);
				// Update the current position
				world.setBlock(pos.getX(), pos.getY(), pos.getZ(), getFluid().staticFluid, newMeta, 7);
			}
		}
		
		if (isFalling)
		{
			// Check if the fluid can stop falling
			Block adjacentBlock = world.getBlock(pos.getX(), pos.getY() - 1, pos.getZ());
			
			if (adjacentBlock != Blocks.AIR && !getFluid().isSameFluid(adjacentBlock))
			{
				// Stop falling
				isFalling = false;
				
				Block above = world.getBlock(pos.getX(), pos.getY() + 1, pos.getZ());
				
				if (getFluid().isSameFluid(above))
				{
					// Spread! (Stopped falling)
					world.setBlockMeta(pos.getX(), pos.getY(), pos.getZ(), 1);
				}
				else
				{
					// Restore old water level, not falling
					world.setBlockMeta(pos.getX(), pos.getY(), pos.getZ(), currentDistance);
				}
			}
		}
		
		// Derive the new distance from the current distance
		newDistance = currentDistance + getFluid().spreadBy;
		
		// Flow to the surrounding positions
		for (Facing dir : flowDirs)
		{
			// Stop spreading if
			// - The flow direction isn't down
			// - the max limit has been reached
			if (dir != Facing.DOWN && newDistance > getFluid().maxSpread)
				break;
			
			// Don't spread to the other directions if the fluid is currently falling
			if (dir != Facing.DOWN && isFalling)
				break;
			
			Vec3i newPos = pos.add(dir);
			Block adjacentBlock = world.getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
			int adjacentMeta = world.getBlockMeta(newPos.getX(), newPos.getY(), newPos.getZ());
			int adjacentDistance = adjacentMeta & BlockFluid.DISTANCE;
			
			// Fluid spreading rules:
			// - If the block is air
			// - If the fluid is the same and the distance is smaller than
			//   the expected gradient
			// - If the block can be replaced by the fluid
			if (adjacentBlock == Blocks.AIR
					|| (getFluid().isSameFluid(adjacentBlock) && newDistance <= adjacentDistance - 1))
			{
				// Check if the new placement is falling
				boolean isPlacementFalling = false;
				Block belowPlacement = world.getBlock(newPos.getX(), newPos.getY() - 1, newPos.getZ());
				
				if (getFluid().isSameFluid(belowPlacement)
						|| belowPlacement == Blocks.AIR)
					isPlacementFalling = true;
				
				// Spread to that position, and schedule an update
				int newMeta = newDistance;
				
				// If the placement is falling and is flowing down,
				// preserve the distance
				// Otherwise, just indicate that it is falling
				if (isPlacementFalling && dir == Facing.DOWN)
					newMeta = BlockFluid.IS_FALLING | currentDistance;
				else if(isPlacementFalling)
					newMeta = BlockFluid.IS_FALLING | newDistance;
				
				// If the placement isn't falling, but the current fluid is,
				// stop falling, and spread outwards
				if (!isPlacementFalling && isFalling)
					newMeta = 1;
				
				world.setBlock(newPos.getX(), newPos.getY(), newPos.getZ(), getFluid().staticFluid, newMeta, 7);
				addFluidUpdate(newPos);
			}
		}
	}
	
	public boolean isFluidTickPending()
	{
		return !pendingUpdates.empty();
	}
	
	/**
	 * Gets the fluid simulated with this instance
	 * @return The fluid simulated
	 */
	public Fluid getFluid()
	{
		return fluid;
	}
	
}
