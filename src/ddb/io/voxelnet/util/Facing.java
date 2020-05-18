package ddb.io.voxelnet.util;

/**
 * An enum representing all directions
 *
 * Note: Facing.values() also contains "Facing.NONE". Facing.directions() contains every direction except "Facing.NONE"
 */
public enum Facing
{
	SOUTH   ( 0,  0,  1),
	NORTH   ( 0,  0, -1),
	EAST    ( 1,  0,  0),
	WEST    (-1,  0,  0),
	UP      ( 0,  1,  0),
	DOWN    ( 0, -1,  0),
	NONE    ( 0,  0,  0);
	
	// Cardinal direction faces
	public static final Facing[] CARDINAL_FACES = new Facing[] { SOUTH, NORTH, EAST, WEST };
	// Facing.NONE is opposite to itself (keeps checks simple)
	private static final Facing[] OPPOSITE_FACES = new Facing[] { NORTH, SOUTH, WEST, EAST, DOWN, UP, NONE };
	// All outward directions
	private static final Facing[] DIRECTIONS = new Facing[] { SOUTH, NORTH, EAST, WEST, UP, DOWN };
	
	final int xOff;
	final int yOff;
	final int zOff;
	
	Facing(int xOff, int yOff, int zOff)
	{
		this.xOff = xOff;
		this.yOff = yOff;
		this.zOff = zOff;
	}
	
	public int[] getOffsets()
	{
		return new int[] {xOff, yOff, zOff};
	}
	
	public int getOffsetX()
	{
		return xOff;
	}
	
	public int getOffsetY()
	{
		return yOff;
	}
	
	public int getOffsetZ()
	{
		return zOff;
	}
	
	/**
	 * Gets all of the outward facing directions
	 * This array excludes Facing.NONE
	 * @return An array containing the outward facing directions
	 */
	public static Facing[] directions()
	{
		return DIRECTIONS;
	}
	
	/**
	 * Gets the opposite facing direction
	 * @return The opposite facing direction
	 */
	public Facing getOpposite()
	{
		return OPPOSITE_FACES[ordinal()];
	}
	
}
