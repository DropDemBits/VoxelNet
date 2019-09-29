package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.World;

import java.util.Stack;

public class BlockWater extends Block
{
	// XXX: BAD! Replace with something better
	public static Stack<Vec3i> availableBlocks = new Stack<>();
	
	public BlockWater()
	{
		setSolid(false);
		setFaceTextures(new int[] {13, 13, 13, 13, 13, 13});
		setTransparent(true);
		setHitBox(null);
		setTickable(true);
	}
	
	@Override
	public void onTick(World world, int x, int y, int z)
	{
		// Check surronding blocks
		final Facing[] dirs = new Facing[] { Facing.DOWN, Facing.NORTH, Facing.WEST, Facing.SOUTH, Facing.EAST, };
		for (Facing facing : dirs)
		{
			int blockX = x + facing.getOffsetX();
			int blockY = y + facing.getOffsetY();
			int blockZ = z + facing.getOffsetZ();
			
			if (!world.isChunkPresent(blockX >> 4, blockY >> 4, blockZ >> 4))
				continue;
			
			Block block = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
			
			Vec3i newPlace = new Vec3i(blockX, blockY, blockZ);
			if ((block == Blocks.AIR || block.canBeReplacedBy(world, this, blockX, blockY, blockZ)) && !availableBlocks.contains(newPlace))
				availableBlocks.push(newPlace);
			
			if (facing == Facing.DOWN && (block == Blocks.AIR || block == Blocks.WATER))
				break;
		}
	}
	
	public static void updateWater(World world)
	{
		while (!availableBlocks.isEmpty())
		{
			Vec3i nextPos = availableBlocks.pop();
			world.setBlock(nextPos.getX(), nextPos.getY(), nextPos.getZ(), Blocks.WATER.getId());
		}
	}
	
	@Override
	public boolean canBeReplacedBy(World world, Block block, int x, int y, int z)
	{
		// Water can be replaced by anything but water
		return block != Blocks.WATER;
	}
	
	@Override
	public RenderLayer getRenderLayer()
	{
		return RenderLayer.FLUID;
	}
	
}
