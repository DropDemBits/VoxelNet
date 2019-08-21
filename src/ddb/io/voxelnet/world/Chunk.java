package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;

import java.util.Arrays;

/**
 * Representation of a game chunk (16*16*16 chunk of tiles)
 */
public class Chunk
{
	public final int chunkX, chunkY, chunkZ;
	public final World world;
	
	// Number of solid blocks on each layer
	private short[] blockLayers = new short[16];
	// Light levels of each block
	// Right now, x-axis is crushed down into 8 block clusters
	private byte[] blockLights = new byte[2 * 16 * 16];
	// The number of blocks in the chunk
	private short blockCount = 0;
	// Actual chunk data
	private byte[] blockData = new byte[16 * 16 * 16];
	// If the chunk holds data (by default, they are empty)
	private boolean isEmpty = true;
	// If the chunk needs to be re-rendered
	private boolean needsRebuild = false;
	// If the chunk needs to be saved to disk
	private boolean isDirty = false;
	// If the chunk was recently generated
	private boolean recentlyGenerated = true;
	
	/**
	 * Constructs a new chunk
	 * @param world The world associated with this chunk
	 * @param x The chunk x position
	 * @param y The chunk x position
	 * @param z The chunk z position
	 */
	public Chunk(World world, int x, int y, int z)
	{
		this.world = world;
		this.chunkX = x;
		this.chunkY = y;
		this.chunkZ = z;
		
		Arrays.fill(blockLayers, (byte)0);
		Arrays.fill(blockLights, (byte)0);
	}
	
	/**
	 * Constructs a chunk from existing data
	 * @param world The world associated with the chunk
	 * @param x The chunk x position
	 * @param y The chunk y position
	 * @param z The chunk z position
	 * @param blockData The chunk block data
	 * @param blockLights The chunk block light data
	 * @param blockCount The chunk's block count
	 */
	public Chunk(World world, int x, int y, int z, byte[] blockData, byte[] blockLights, short blockCount)
	{
		this(world, x, y, z);
		
		this.blockCount = blockCount;
		System.arraycopy(blockData, 0, this.blockData, 0, this.blockData.length);
		System.arraycopy(blockLights, 0, this.blockLights, 0, this.blockLights.length);
		
		// Update the rebuild state
		needsRebuild = true;
		
		// Most likely not empty
		isEmpty = false;
	}
	
	/**
	 * Gets the block data for the chunk
	 * The data is organized in a single dimension list, and is always accessed
	 * using the following formula:
	 * <code>x + z * 16 + y * (16 * 16)</code>
	 * @return The block data for this chunk
	 */
	public byte[] getData() { return blockData; }
	
	/**
	 * Gets the per-block light data in the chunk
	 * @return The block light data of the chunk
	 */
	public byte[] getLightData() { return blockLights; }
	
	/**
	 * Gets the number of blocks in the chunk
	 * @return The number of blocks in the chunk
	 */
	public short getBlockCount() { return blockCount; }
	
	/**
	 * Sets the block at the specified block position to the specified id
	 * If the id is equal to -1, nothing occurs
	 * @param x The x position of the block, in blocks
	 * @param y The y position of the block, in blocks
	 * @param z The z position of the block, in blocks
	 * @param id The id of the new block
	 */
	public void setBlock(int x, int y, int z, byte id)
	{
		// Check for out of bounds access or the out of bounds id
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16 || id == -1)
			return;
		
		byte lastBlock = blockData[x + z * 16 + y * 256];
		blockData[x + z * 16 + y * 256] = id;
		
		// Update the dirty & rebuild states
		isDirty = true;
		needsRebuild = true;
		
		if(Game.showThings)
			System.out.println("block ("+x+", "+y+", "+z+") "+id);
		
		// Update the block count & isEmpty
		if (lastBlock == 0 && id != 0)
		{
			++blockCount;
			isEmpty = false;
		}
		else if (lastBlock != 0 && id == 0)
		{
			--blockCount;
			
			if (blockCount < 0)
				blockCount = 0;
			
			if (blockCount == 0)
				isEmpty = true;
		}
	}
	
	/**
	 * Gets the block id for the given position
	 * @param x The x position of the block, in blocks
	 * @param y The x position of the block, in blocks
	 * @param z The x position of the block, in blocks
	 * @return The id of the block, or -1 if the position is out of bounds
	 */
	public byte getBlock(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		return blockData[x + z * 16 + y * 256];
	}
	
	/**
	 * Gets the block light for the given position
	 * The base light level will be a value between 0-15. If the value is
	 * greater than 16, the light level can be affected by the sky.
	 *
	 * @param x The x position of the block, in blocks
	 * @param y The x position of the block, in blocks
	 * @param z The x position of the block, in blocks
	 * @return The light level of the block, as specified above, or -1 if the
	 *         block position is out of bounds
	 */
	public byte getBlockLight(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		
		// Bits
		// Y    | Z    | X
		// 0000 | 0000 | 0 000
		//    5      1   0
		
		// Block light will be in the range of 0-15, but only handling 0 & 15 & no skylight
		byte baseLevel = (byte)((blockLights[(x >> 3) + z * 2 + y * 2 * 16] >> (x & 0x7)) & 1);
		
		return baseLevel == 0 ? (byte)3 : (byte)15;
	}
	
	/**
	 * Checks if the chunk is dirty and needs to be saved to disk
	 * @return True if the chunk needs to be saved
	 */
	public boolean isDirty()
	{
		return isDirty;
	}
	
	/**
	 * Makes the chunk clean
	 */
	public void makeClean()
	{
		isDirty = false;
	}
	
	/**
	 * Makes the chunk dirty
	 */
	public void makeDirty()
	{
		isDirty = true;
	}
	
	/**
	 * Checks if the chunk needs to be re-rendered
	 * @return True if the chunk needs to be re-rendered
	 */
	public boolean needsRebuild()
	{
		return needsRebuild;
	}
	
	/**
	 * Resets the chunk rebuild status
	 */
	public void resetRebuildStatus()
	{
		needsRebuild = false;
	}
	
	/**
	 * Forces a chunk rebuild to occur
	 */
	public void forceRebuild()
	{
		needsRebuild = true;
	}
	
	/**
	 * Checks if the chunk is empty
	 * @return True if the chunk is empty
	 */
	public boolean isEmpty()
	{
		return isEmpty;
	}
	
	/**
	 * Checks if the chunk is recently generated
	 * @return True if the chunk was recently generated
	 */
	public boolean recentlyGenerated()
	{
		return recentlyGenerated;
	}
	
	/**
	 * Sets the chunk to be already generated
	 */
	public void setGenerated()
	{
		recentlyGenerated = false;
	}
}
