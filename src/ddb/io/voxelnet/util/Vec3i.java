package ddb.io.voxelnet.util;

public class Vec3i
{
	// TODO: Add a pool of Vec3i's
	private int x, y, z;
	
	public Vec3i() {}
	
	public Vec3i(int x, int y, int z)
	{
		set(x, y, z);
	}
	
	public Vec3i set(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public Vec3i add(int xOff, int yOff, int zOff)
	{
		return new Vec3i(x + xOff, y + yOff, z + zOff);
	}
	
	public Vec3i add(Facing dir)
	{
		return new Vec3i(x + dir.getOffsetX(), y + dir.getOffsetY(), z + dir.getOffsetZ());
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Vec3i))
			return false;
		
		Vec3i other = (Vec3i)obj;
		return x == other.x && y == other.y && z == other.z;
	}
	
	@Override
	public int hashCode()
	{
		// Order:
		// (Y hi 14) (X lo 5) (Z lo 5) (Y lo 4)
		return ((y & ~0xF) << (14 - 4)) | ((x & 0x1F) << 9) | ((z & 0x1F) << 4) | (y & 0xF);
	}
	
	@Override
	public String toString()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
