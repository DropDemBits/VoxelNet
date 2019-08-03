package ddb.io.voxelnet.block;

import ddb.io.voxelnet.world.World;

public class BlockDoor extends Block
{
	private boolean isUpper = false;
	private static final int[] UPPER_FACES = new int[] {8, 8, 8, 8,  9, -1};
	private static final int[] LOWER_FACES = new int[] {7, 7, 7, 7, -1,  9};
	
	BlockDoor()
	{
		setSolid(false);
		setTransparent(true);
	}
	
	/**
	 * Sets whether the block is the upper or lower half of a door
	 * Also updates the face textures
	 * @param isUpper The new isUpper state
	 * @return Instance of this to allow for chaining
	 */
	public BlockDoor setUpper(boolean isUpper)
	{
		this.isUpper = isUpper;
		
		if (isUpper)
			setFaceTextures(UPPER_FACES);
		else
			setFaceTextures(LOWER_FACES);
		
		return this;
	}
	
	@Override
	public void onBlockPlaced(World world, int x, int y, int z)
	{
		super.onBlockPlaced(world, x, y, z);
		
		// Let the lower half place the upper half
		if(!isUpper)
			world.setBlock(x, y + 1, z, Blocks.DOOR_UPPER.getId());
	}
	
	public void onBlockBroken(World world, int x, int y, int z)
	{
		if(isUpper)
		{
			world.setBlock(x, y - 1, z, Blocks.AIR.getId());
			System.out.println("UPPER -> LOWER");
		}
		else
		{
			world.setBlock(x, y + 1, z, Blocks.AIR.getId());
			System.out.println("LOWER -> UPPER");
		}
	}
	
	@Override
	public boolean canPlaceBlock(World world, int x, int y, int z)
	{
		// A Player can't manually place the upper half of a block
		if(isUpper)
			return false;
		
		return     world.getBlock(x, y, z) == Blocks.AIR.getId()
				&& world.getBlock(x, y + 1, z) == Blocks.AIR.getId();
	}
}
