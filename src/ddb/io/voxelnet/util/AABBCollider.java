package ddb.io.voxelnet.util;

public class AABBCollider
{
	float xMin, yMin, zMin;
	float xMax, yMax, zMax;
	
	public AABBCollider(float x0, float y0, float z0, float x1, float y1, float z1)
	{
		this.xMin = x0;
		this.yMin = y0;
		this.zMin = z0;
		this.xMax = x1;
		this.yMax = y1;
		this.zMax = z1;
	}
}
