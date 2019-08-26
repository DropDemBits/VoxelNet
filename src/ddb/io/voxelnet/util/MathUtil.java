package ddb.io.voxelnet.util;

/**
 * Various math utilities
 */
public class MathUtil
{
	
	public static double lerp(double a, double b, double t)
	{
		return a + t * (b - a);
	}
	
}
