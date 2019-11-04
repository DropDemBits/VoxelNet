package ddb.io.voxelnet.fluid;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockFluid;
import ddb.io.voxelnet.block.Blocks;

/**
 * Representation of a fluid
 */
public class Fluid
{
	
	public static Fluid WATER;
	public static Fluid LAVA;
	public static Fluid[] ALL_FLUIDS;
	
	public static void init()
	{
		 WATER = new Fluid(Blocks.WATER, Blocks.UPDATING_WATER, 7, 1, 5, true);
		 LAVA = new Fluid(Blocks.LAVA, Blocks.UPDATING_LAVA, 7, 2, 20, false);
		 ALL_FLUIDS = new Fluid[] { WATER, LAVA };
	}
	
	// Actively updating version of the fluid
	public final BlockFluid updatingFluid;
	// Static form of the fluid
	public final BlockFluid staticFluid;
	// Max spreading distance
	public final int maxSpread;
	// Amount to increase distance
	public final int spreadBy;
	
	// The rate at which to update the fluid by
	public int updateRate;
	
	// If a fluid is able to form source blocks from adjacent fluid blocks
	public boolean canFormSources;
	
	/**
	 * Contructs a new fluid
	 * @param staticFluid Block form of the static fluid
	 * @param updatingFluid Block form of the updating
	 * @param maxSpread The maximum spreading distance of the fluid, in blocks
	 * @param spreadBy The amount to increase the distance by, in blocks
	 * @param updateRate The rate at which the fluid will update, in ticks (20Hz) per second
	 * @param canFormSources Whether the fluid can form source blocks
	 */
	public Fluid(BlockFluid staticFluid, BlockFluid updatingFluid, int maxSpread, int spreadBy, int updateRate, boolean canFormSources)
	{
		this.staticFluid = staticFluid;
		this.updatingFluid = updatingFluid;
		this.maxSpread = maxSpread;
		this.spreadBy = spreadBy;
		this.canFormSources = canFormSources;
		this.updateRate = updateRate;
	}
	
	public boolean isSameFluid(Block other)
	{
		return other instanceof BlockFluid && ((BlockFluid)other).getFluid() == this;
	}
	
}
