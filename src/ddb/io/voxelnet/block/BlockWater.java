package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.World;

import java.util.Stack;

public class BlockWater extends Block
{
	// Masks
	public static final int IS_FALLING = 0b1000;
	public static final int DISTANCE = 0b0111;
	
	// XXX: BAD! Replace with something better
	public static Stack<FluidPlacement> availableBlocks = new Stack<>();
	
	public BlockWater()
	{
		setSolid(false);
		setFaceTextures(new int[] {13, 13, 13, 13, 13, 13});
		setTransparent(true);
		setHitBox(null);
		setTickable(true);
	}
	
	@Override
	public void onNeighborUpdated(World world, int x, int y, int z, Facing dir)
	{
		super.onNeighborUpdated(world, x, y, z, dir);
		//doWaterSpread(world, x, y, z);
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
		byte oldMeta = srcMeta;
		boolean isFalling = (srcMeta & IS_FALLING) != 0;
		
		// IF Block below is air:
		// Falling = true for next block, next block down is falling
		
		for (Facing facing : dirs)
		{
			// Don't spread outwards if falling or at max distance
			if ((srcMeta & DISTANCE) >= 7)
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
			if ((block == Blocks.WATER && srcMeta <= (adjacentMeta & DISTANCE) && newMeta < (adjacentMeta & DISTANCE))
					|| block == Blocks.AIR
					|| block.canBeReplacedBy(world, this, blockX, blockY, blockZ))
			{
				// Check for falling:
				int off = 1;
				if (facing == Facing.DOWN)
					off = 0;
				
				Block adjBelow = Block.idToBlock(world.getBlock(blockX, blockY - off, blockZ));
				if (adjBelow == Blocks.AIR || adjBelow == Blocks.WATER)
					newMeta |= IS_FALLING;
				
				if (facing == Facing.DOWN)
				{
					// Keep distance the same
					newMeta &= ~DISTANCE;
					newMeta |= (srcMeta & DISTANCE);
				}
				
				FluidPlacement newPlace = new FluidPlacement(new Vec3i(blockX, blockY, blockZ), newMeta);
				newPlace.newMeta = newMeta;
				//System.out.println("NP: " + newPlace.newMeta + ", " + newPlace.pos);
				
				if (!availableBlocks.contains(newPlace))
					availableBlocks.push(newPlace);
			}
			
			if (facing == Facing.DOWN)
			{
				if (block != Blocks.AIR && block != Blocks.WATER)
					srcMeta &= ~IS_FALLING;
			}
			
			if (srcMeta != oldMeta)
				world.setBlockMeta(x, y, z, srcMeta);
			
			if (isFalling)
				break;
		}
	}
	
	public static void updateWater(World world)
	{
		while (!availableBlocks.isEmpty())
		{
			FluidPlacement nextPlace = availableBlocks.pop();
			world.setBlock(nextPlace.pos.getX(), nextPlace.pos.getY(), nextPlace.pos.getZ(), Blocks.WATER.getId(), nextPlace.newMeta, 3);
		}
	}
	
	@Override
	public boolean canBeReplacedBy(World world, Block block, int x, int y, int z)
	{
		// Water can be replaced by anything but water, unless the distance is smaller (handled above)
		return block != Blocks.WATER;
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.FLUID;
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
			return pos.equals(obj);
		}
	}
	
}
