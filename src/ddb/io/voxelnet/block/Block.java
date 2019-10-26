package ddb.io.voxelnet.block;

import ddb.io.voxelnet.client.render.BlockRenderModel;
import ddb.io.voxelnet.client.render.RenderLayer;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;

import java.util.ArrayList;
import java.util.List;

public class Block
{
	/// Global Block Registry ///
	private static final List<Block> idToBlockMap = new ArrayList<>(256);
	
	public static void init()
	{
		Blocks.VOID         = addBlock(-1, new BlockAir());
		Blocks.AIR          = addBlock(0,  new BlockAir());
		Blocks.GRASS        = addBlock(1,  new BlockGrass());
		Blocks.DIRT         = addBlock(2,  new BlockDirt());
		Blocks.STONE        = addBlock(3,  new Block().setSolid(true).setFaceTextures(new int[] {3, 3, 3, 3, 3, 3}));
		Blocks.PLANKS       = addBlock(4,  new Block().setSolid(true).setFaceTextures(new int[] {4, 4, 4, 4, 4, 4}));
		Blocks.STONE_BRICKS = addBlock(5,  new Block().setSolid(true).setFaceTextures(new int[] {5, 5, 5, 5, 5, 5}));
		Blocks.CLAY_BRICKS  = addBlock(6,  new Block().setSolid(true).setFaceTextures(new int[] {6, 6, 6, 6, 6, 6}));
		Blocks.DOOR_LOWER   = addBlock(7,  new BlockDoor().setUpper(false));
		Blocks.DOOR_UPPER   = addBlock(8,  new BlockDoor().setUpper(true));
		Blocks.GLASS        = addBlock(9,  new BlockGlass());
		Blocks.SAND         = addBlock(10, new BlockFalling().setSolid(true).setFaceTextures(new int[] {11, 11, 11, 11, 11, 11}));
		Blocks.GRAVEL       = addBlock(11, new BlockFalling().setSolid(true).setFaceTextures(new int[] {12, 12, 12, 12, 12, 12}));
		Blocks.UPDATING_WATER = addBlock(12, new BlockWater(true));
		Blocks.WATER        = addBlock(13, new BlockWater(false));
	}
	
	private static Block addBlock(int id, Block instance)
	{
		instance.setId((byte) id);
		
		if (id == -1)
			return instance;
		
		idToBlockMap.add((byte)id, instance);
		return instance;
	}
	
	public static Block idToBlock(byte id)
	{
		if (id == -1)
			return Blocks.VOID;
		
		return idToBlockMap.get(Byte.toUnsignedInt(id));
	}
	
	/// Per-Block variables ///
	private static final AABBCollider DEFAULT_COLLIDER = new AABBCollider(0f, 0f, 0f, 1f, 1f, 1f);
	private int[] faceTextures;
	
	// Conditional Setters
	private boolean isSolid = true;
	private boolean isTransparent = false;
	
	private AABBCollider hitBox = DEFAULT_COLLIDER;
	private byte id;
	
	protected Block() {}
	
	/// Local setters
	private void setId(byte id)
	{
		this.id = id;
	}
	
	/// Global setters
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
	
	/**
	 * Sets the transparent state of the block
	 * @param isTransparent The new transparent state of the block
	 * @return Instance of this to allow for chaining
	 */
	public Block setTransparent(boolean isTransparent)
	{
		this.isTransparent = isTransparent;
		return this;
	}
	
	/**
	 * Sets the hit box of the block
	 * @param hitBox The new hit box of the block
	 * @return Instance of this to allow for chaining
	 */
	protected Block setHitBox (AABBCollider hitBox)
	{
		this.hitBox = hitBox;
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
	 * Gets if the block is transparent
	 * @return True if the block is transparent
	 */
	public boolean isTransparent()
	{
		return isTransparent;
	}
	
	/**
	 * Gets if the block can receive block ticks
	 * @return True if the block can receive block ticks
	 */
	public boolean isTickable()
	{
		return false;
	}
	
	/**
	 * Gets if the block can receive random block ticks
	 * @return True if the block can receive random block ticks
	 */
	public boolean isRandomlyTickable()
	{
		return false;
	}
	
	/**
	 * Gets the collision box of this box
	 * @return The appropriate collision box for the block
	 */
	public AABBCollider getCollisionBox()
	{
		return DEFAULT_COLLIDER;
	}
	
	/**
	 * Gets the hit box of this block
	 * Used to detect if a block should be selectable or not
	 * Can be null to prevent selection
	 * @return The hit box of this block
	 */
	public AABBCollider getHitBox() { return hitBox; }
	
	/**
	 * Gets the render layer for this block
	 * @return The layer to render the block on
	 */
	public RenderLayer getRenderLayer() { return RenderLayer.OPAQUE; }
	
	public BlockRenderModel getRenderModel()
	{
		return BlockRenderModel.CUBE;
	}
	
	/**
	 * Gets the ID of the block
	 * @return The block id
	 */
	public byte getId()
	{
		return id;
	}
	
	// Event responses
	/**
	 * Called whenever a block is placed by a player
	 * @param world The world the new block was placed in
	 * @param x The x coordinate of the block
	 * @param y The y coordinate of the block
	 * @param z The z coordinate of the block
	 */
	public void onBlockPlaced(World world, int x, int y, int z) {}
	
	/**
	 * Called whenever a block is broken by a player
	 * @param world The world the block was destroyed in
	 * @param x The x coordinate of the block
	 * @param y The y coordinate of the block
	 * @param z The z coordinate of the block
	 */
	public void onBlockBroken(World world, int x, int y, int z) {}
	
	/**
	 * Called whenever one of the neighboring blocks is updated
	 * @param world The world the neighboring block was update
	 * @param x The x coordinate of the current block
	 * @param y The y coordinate of the current block
	 * @param z The z coordinate of the current block
	 * @param dir The direction of the neighbor block, relative to the current one
	 */
	public void onNeighborUpdated(World world, int x, int y, int z, Facing dir) {}
	
	public void onTick(World world, int x, int y, int z) {}
	
	public void onRandomTick(World world, int x, int y, int z) {}
	
	/**
	 * Checks if a player can place a block in the specified location
	 * @param world The world that the block will be placed in
	 * @param x The x coordinate of the block to place
	 * @param y The y coordinate of the block to place
	 * @param z The z coordinate of the block to place
	 * @return True if the block can be placed in the specified location
	 */
	public boolean canPlaceBlock(World world, int x, int y, int z)
	{
		// By default, the block can only be placed if there is air if canBeReplacedWith returns true
		Block block = world.getBlock(x, y, z);
		if (block == Blocks.AIR)
			return true;
		
		return block.canBeReplacedBy(world, this, (byte)0, x, y, z);
	}
	
	/**
	 * Checks if the block in the specified location can be replaced by the current one
	 * @param world The world that the block would be replaced in
	 * @param block The block to place
	 * @param x The x coordinate of the block to replace (the current block)
	 * @param y The y coordinate of the block to replace (the current block)
	 * @param z The z coordinate of the block to replace (the current block)
	 * @return True if the block can be replaced by the specified block
	 */
	public boolean canBeReplacedBy(World world, Block block, byte newMeta, int x, int y, int z)
	{
		return false;
	}
	
	public boolean showFace(Block adjacent, Facing dir)
	{
		return (!adjacent.isSolid() || adjacent.isTransparent()) && adjacent != this;
	}
	
}
