package ddb.io.voxelnet.util;

import java.util.Objects;

public class Vec3i
{
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
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Vec3i))
			return false;
		
		Vec3i other = (Vec3i)obj;
		return other.hashCode() == hashCode();
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(x, y, z);
	}
	
	@Override
	public String toString()
	{
		return "(" + x + ", " + y + ", " + z + ")";
	}
}
