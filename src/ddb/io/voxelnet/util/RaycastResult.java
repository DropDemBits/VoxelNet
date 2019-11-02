package ddb.io.voxelnet.util;

/**
 * Representation of a hit result
 *
 * Essentially just a struct of values
 *
 */
public class RaycastResult
{
	public static final RaycastResult NO_RESULT = new RaycastResult();
	
	// The specific hit coordinate
	public double hitX;
	public double hitY;
	public double hitZ;
	
	// The block coordinates of the hit
	public int blockX;
	public int blockY;
	public int blockZ;
	
	// The face that was hit
	public Facing face;
	
}
