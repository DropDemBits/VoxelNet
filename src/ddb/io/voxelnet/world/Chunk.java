package ddb.io.voxelnet.world;

import java.util.Arrays;

/**
 * Representation of a game chunk (16*16*16 chunk of tiles)
 */
public class Chunk
{
	public final int chunkX, chunkY, chunkZ;
	public final World world;
	
	// Highest block height for each column. -1 indicates that the column is
	// empty
	private byte[] blockColumns = new byte[16 * 16];
	// Number of solid blocks on each layer
	private short[] blockLayers = new short[16];
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
		
		// Update the block column
		byte lastColumnHeight = blockColumns[x + z * 16];
		
		if (blockColumns[x + z * 16] == (byte)y && id == 0)
		{
			// If the block being placed is air and the block replaced is the
			// tallest block, search for the next lowest non-block
			byte height = (byte) (y - 1);
			
			for(; height >= 0; height--)
			{
				if (blockData[x + z * 16 + height * 256] != 0)
					break;
			}
			
			blockColumns[x + z * 16] = height;
		}
		else if (blockColumns[x + z * 16] < (byte)y)
		{
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
