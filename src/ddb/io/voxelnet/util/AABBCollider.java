package ddb.io.voxelnet.util;

public class AABBCollider
{
	float x, y, z;
	float width, height, depth;
	
	public AABBCollider(float x0, float y0, float z0, float w, float h, float d)
	{
		this.x = x0;
		this.y = y0;
		this.z = z0;
		this.width  = w;
		this.height = h;
		this.depth  = d;
	}
	
	public void setPosition(float x, float y, float z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public boolean intersectsWith(AABBCollider other)
	{
		return     x <= other.x + other.width  && other.x <= x + width
				&& y <= other.y + other.height && other.y <= y + height
				&& z <= other.z + other.depth  && other.z <= z + depth;
	}
	
	public void add(float xOff, float yOff, float zOff)
	{
		x += xOff;
		y += yOff;
		z += zOff;
	}
}
