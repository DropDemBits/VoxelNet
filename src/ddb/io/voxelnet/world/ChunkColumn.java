package ddb.io.voxelnet.world;

public class ChunkColumn
{
	// 1 Column is defined to be a vertical stack of 256 blocks
	
	// Tallest opaque block in each column
	public byte[] opaqueColumns = new byte[16 * 16];
	// Tallest block in each column, can be either transparent or opaque
	public byte[] blockColumns = new byte[16 * 16];
	
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
}
