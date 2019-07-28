package ddb.io.voxelnet.block;

import ddb.io.voxelnet.util.AABBCollider;

import java.util.ArrayList;
import java.util.List;

public class Block
{
	/// Global Block Registry ///
	private static final List<Block> idToBlockMap = new ArrayList<>(255);
	
	public static void init()
	{
		Blocks.AIR      = addBlock(0, new Block().setSolid(false).setFaceTextures(new int[] {-1, -1, -1, -1, -1, -1}));
		Blocks.GRASS    = addBlock(1, new Block().setSolid(true).setFaceTextures(new int[] {1, 1, 1, 1, 0, 2}));
		Blocks.DIRT     = addBlock(2, new Block().setSolid(true).setFaceTextures(new int[] {2, 2, 2, 2, 2, 2}));
		Blocks.STONE    = addBlock(3, new Block().setSolid(true).setFaceTextures(new int[] {3, 3, 3, 3, 3, 3}));
	}
	
	private static Block addBlock(int id, Block instance)
	{
		idToBlockMap.add(id, instance);
		return instance;
	}
	
	public static Block idToBlock(byte id)
	{
		if (id == -1)
			return Blocks.AIR;
		
		return idToBlockMap.get(Byte.toUnsignedInt(id));
	}
	
	/// Per-Block variables ///
	private static AABBCollider DEFAULT_COLLIDER = new AABBCollider(0f, 0f, 0f, 1f, 1f, 1f);
	private int[] faceTextures;
	private boolean isSolid;
	
	protected Block() {}
	
	/// Local setters
	/**
	 * Sets the face textures used for rendering
	 * @param faceTextures The new set of face textures
	 * @return Instance of this to allow for chaining
	 */
	public Block setFaceTextures(int[] faceTextures)
	{
		assert (faceTextures.length == 6);
		this.faceTextures = faceTextures;
		return this;
	}
	
	/**
	 * Sets the solid state of the block
	 * @param isSolid The new solid state of the block
	 * @return Instance of this to allow for chaining
	 */
	public Block setSolid(boolean isSolid)
	{
		this.isSolid = isSolid;
		return this;
	}
	
	/// Public Getters ///
	/**
	 * Gets the face textures associated with this block
	 * @return The face textures associated with this block
	 */
	public int[] getFaceTextures()
	{
		return faceTextures;
	}
	
	/**
	 * Gets if the block is solid
	 * @return True if the block is solid
	 */
	public boolean isSolid()
	{
		return isSolid;
	}
	
	/**
	 * Gets the collision box of this box
	 * @return The appropriate collision box for the block
	 */
	public AABBCollider getCollisionBox()
	{
		return DEFAULT_COLLIDER;
	}
}
