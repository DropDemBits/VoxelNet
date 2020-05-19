package ddb.io.voxelnet.world;

public class ChunkColumn
{
	public static final int COLUMNS_SIZE = 16 * 16;
	
	// 1 Column is defined to be a vertical stack of 256 blocks
	
	// Tallest opaque block in each column
	private final byte[] opaqueColumns = new byte[COLUMNS_SIZE];
	// Tallest block in each column, can be either transparent or opaque
	private byte[] blockColumns = new byte[16 * 16];
	
	// Column positions
	public final int columnX, columnZ;
	
	/**
	 * Creates a new chunk column
	 * @param x The x position of the column
	 * @param z The z position of the column
	 */
	public ChunkColumn(int x, int z)
	{
		this.columnX = x;
		this.columnZ = z;
	}
	
	/**
	 * Creates a chunk column from existing data
	 * @param x The x position of the column
	 * @param z The z position of the column
	 * @param opaqueColumns Opaque column data
	 */
	public ChunkColumn(int x, int z, byte[] opaqueColumns)
	{
		this(x, z);
		
		System.arraycopy(opaqueColumns, 0, this.opaqueColumns, 0, this.opaqueColumns.length);
	}
	
	/**
	 * Gets the height of the tallest opaque block
	 * This ignores transparent blocks (e.g. glass)
	 *
	 * @param blockX The x position inside the column
	 * @param blockZ The z position inside the column
	 * @return The tallest opaque block
	 */
	public int getTallestOpaque(int blockX, int blockZ)
	{
		int columnIdx = blockX + blockZ * 16;
		return Byte.toUnsignedInt(opaqueColumns[columnIdx]);
	}
	
	/**
	 * Sets the height of the tallest opaque block
	 *
	 * @param blockX The x position of the block column to update
	 * @param blockZ The z position of the block column to update
	 * @param y The new height of the tallest opaque block
	 */
	public void setTallestOpaque(int blockX, int blockZ, int y)
	{
		int columnIdx = blockX + blockZ * 16;
		
		// Lossy / truncate convert into a byte
		opaqueColumns[columnIdx] = (byte)y;
	}
	
	public byte[] getOpaqueColumnData()
	{
		return opaqueColumns;
	}
	
}
