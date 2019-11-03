package ddb.io.voxelnet.fluid;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.World;

import java.util.Stack;

/**
 * Per-world instance of a fluid
 *
 * Right now, only simulates flood-fill type fluids
 */
public class FluidInstance
{
	
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
		pendingUpdates.add(pos);
	}
	
	/**
	 * Performs a single fluid tick
	 * @param world The world to perform the fluid tick in
	 */
	public void doFluidTick(World world)
	{
		Stack<Vec3i> temp = processingUpdates;
		
		// Swap the lists (processing updates should be empty)
		processingUpdates = pendingUpdates;
		pendingUpdates = temp;
		
		assert (pendingUpdates.isEmpty());
		
		while (!processingUpdates.isEmpty())
		{
			Vec3i pos = processingUpdates.pop();
			
			// Check surrounding positions
			for (Facing dir : Facing.CARDINAL_FACES)
			{
				Vec3i newPos = pos.add(dir);
				Block adjacent = world.getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				
				if (adjacent == Blocks.AIR)
				{
					// Spread to that position, and schedule an update
					world.setBlock(newPos.getX(), newPos.getY(), newPos.getZ(), fluid.staticFluid);
					addFluidUpdate(newPos);
				}
			}
		}
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
