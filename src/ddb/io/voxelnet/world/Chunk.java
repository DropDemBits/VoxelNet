package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;

import java.util.Arrays;

/**
 * Representation of a game chunk (16*16*16 chunk of tiles)
 */
public class Chunk
{
	public final int chunkX, chunkY, chunkZ;
	public final World world;
	
	// Highest opaque block height for each column. -1 indicates that the
	// column is empty
	private byte[] blockColumns = new byte[16 * 16];
	// Number of solid blocks on each layer
	private short[] blockLayers = new short[16];
	// Light levels of each block
	// Right now, x-axis is crushed down into 8 block clusters
	private byte[] blockLights = new byte[2 * 16 * 16];
	// The number of columns that are not empty
	private byte filledColumns = 0;
	// Actual chunk data
	private byte[] blockData = new byte[16 * 16 * 16];
	// If the chunk holds data (by default, they are empty)
	private boolean isEmpty = true;
	// If the chunk needs to be re-rendered
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
		
		Arrays.fill(blockColumns, (byte)-1);
		Arrays.fill(blockLayers, (byte)0);
		Arrays.fill(blockLights, (byte)0);
	}
	
	/**
	 * Gets the block data for the chunk
	 * The data is organized in a single dimension list, and is always accessed
	 * using the following formula:
	 * <code>x + z * 16 + y * (16 * 16)</code>
	 * @return The block data for this chunk
	 */
	public byte[] getData()
	{
		return blockData;
	}
	
	/**
	 * Gets the array which holds the highest blocks for each block column
	 * @return An array holding the specified data
	 */
	public byte[] getColumns()
	{
		return blockColumns;
	}
	
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
		
		blockData[x + z * 16 + y * 256] = id;
		
		// Update the dirty state
		isDirty = true;
		
		if(Game.showThings)
			System.out.println("block ("+x+", "+y+", "+z+") "+id);
		
		// Update the block column
		byte lastColumnHeight = blockColumns[x + z * 16];
		
		if (blockColumns[x + z * 16] == (byte)y && (id == 0 || Block.idToBlock(id).isTransparent()))
		{
			System.out.print("colupd: " + blockColumns[x + z * 16] + " -> ");
			// If the block being placed is air and the block replaced is the
			// tallest block, search for the next lowest opaque block
			byte height = (byte) (y - 1);
			
			for(; height >= 0; height--)
			{
				byte block = blockData[x + z * 16 + height * 256];
				if (block != 0 && !Block.idToBlock(block).isTransparent())
					break;
			}
			
			blockColumns[x + z * 16] = height;
			System.out.println(height);
		}
		else if (blockColumns[x + z * 16] < (byte)y)
		{
			if(Game.showThings)
				System.out.println("colupd-replace: " + blockColumns[x + z * 16] + " -> " + y);
			blockColumns[x + z * 16] = (byte) y;
		}
		
		// Update the filled column count and isEmpty
		if (lastColumnHeight == -1 && blockColumns[x + z * 16] != -1)
		{
			filledColumns++;
			isEmpty = false;
		}
		else if (lastColumnHeight != -1 && blockColumns[x + z * 16] == -1)
		{
			filledColumns--;
			
			if (filledColumns == 0)
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
	 * Checks if the chunk is dirty and needs to be re-rendered
	 * @return True if the chunk needs to be re-rendered
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
