package ddb.io.voxelnet.util;

public enum Facing
{
	NORTH   ( 0,  0, -1),
	WEST    (-1,  0,  0),
	SOUTH   ( 0,  0,  1),
	EAST    ( 1,  0,  0),
	UP      ( 0,  1,  0),
	DOWN    ( 0, -1,  0);
	
	int xOff;
	int yOff;
	int zOff;
	
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
}
