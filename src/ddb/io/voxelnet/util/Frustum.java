package ddb.io.voxelnet.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Frustum
{
	// Perspective Related
	private float fov, aspect, zNear, zFar;
	private float wNearHalf, hNearHalf, wFarHalf, hFarHalf;
	private Vector3f up, right, forward;
	// Near & Far plane (stored as half of real dimensions)
	// Basis/Pointing vectors
	// Frustum points
	//      0       1
	// z is FAR  or NEAR
	// y is TOP  or BOTTOM
	// x is LEFT or RIGHT
	public Vector3f[][][] frustumPoints = new Vector3f[2][2][2];
	
	// Normal indices
	public static final int TOP = 0;
	public static final int BOTTOM = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int FAR = 4;
	public static final int NEAR = 5;
	
	// Frustum normals
	public Vector3f[] frustumNormals = new Vector3f[6];
	// Frustum D values
	public float[] frustumDvals = new float[6];
	
	public Frustum()
	{
		right   = new Vector3f(1, 0,  0);
		up      = new Vector3f(0, 1,  0);
		forward = new Vector3f(0, 0, -1);
		
		for (int i = 0; i < 8; i++)
			frustumPoints[i / 4][(i / 2) % 2][i % 2] = new Vector3f(0);
		for (int i = 0; i < 6; i++)
		{
			frustumNormals[i] = new Vector3f(0);
			frustumDvals[i] = 0f;
		}
	}
	
	public Frustum(float fov, float aspect, float zNear, float zFar)
	{
		super();
		updateShape(fov, aspect, zNear, zFar);
	}
	
	/**
	 * Updates the shape of the view frustum
	 * @param fov The FOV of the frustum, in radians
	 * @param aspect The aspect ratio of the screen
	 * @param zNear The distance of the zNear plane from the camera
	 * @param zFar The distance of the zFar plane from the camera
	 */
	public void updateShape(float fov, float aspect, float zNear, float zFar)
	{
		this.fov = fov;
		this.aspect = aspect;
		this.zNear = zNear;
		this.zFar = zFar;
		
		// Calculate the half-dimensions of the near and far planes
		hNearHalf = (float) (Math.tan(fov / 2d) * this.zNear);
		wNearHalf = hNearHalf * aspect;
		
		hFarHalf = (float) (Math.tan(fov / 2d) * this.zFar);
		wFarHalf = hFarHalf * aspect;
	}
	
	/**
	 * Updates the view frustum using the given view matrix and position
	 * @param view The view matrix to use
	 * @param x The x position of the view frustum
	 * @param y The x position of the view frustum
	 * @param z The x position of the view frustum
	 */
	public void updateFrustum(Matrix4f view, float x, float y, float z)
	{
		resetBasisAndPoints();
		
		// Rotate all the basis vectors
		Matrix4f inView = new Matrix4f();
		view.invert(inView);
		right.mulDirection(inView);
		up.mulDirection(inView);
		forward.mulDirection(inView);
		
		Vector3f tempA = new Vector3f();    // Use for single & double args
		Vector3f tempB = new Vector3f();    // Use for double only
		
		// Calculate the frustum points & normals
		// Far plane
		Vector3f farC = new Vector3f(x, y, z);
		farC.add(tempA.set(forward).mul(zFar));
		
		// Pointing
		Vector3f upFar = tempA.set(up).mul(hFarHalf);
		Vector3f rightFar = tempB.set(right).mul(wFarHalf);
		
		// Far Top Left
		frustumPoints[0][0][0].set(farC);
		frustumPoints[0][0][0].add(upFar);
		frustumPoints[0][0][0].sub(rightFar);
		
		// Far Top Right
		frustumPoints[0][0][1].set(farC);
		frustumPoints[0][0][1].add(upFar);
		frustumPoints[0][0][1].add(rightFar);
		
		// Far Bottom Left
		frustumPoints[0][1][0].set(farC);
		frustumPoints[0][1][0].sub(upFar);
		frustumPoints[0][1][0].sub(rightFar);
		
		// Far Bottom Right
		frustumPoints[0][1][1].set(farC);
		frustumPoints[0][1][1].sub(upFar);
		frustumPoints[0][1][1].add(rightFar);
		
		Vector3f nearC = farC.set(x, y, z);
		nearC.add(tempA.set(forward).mul(zNear));
		
		// Pointing
		Vector3f upNear = tempA.set(up).mul(hNearHalf);
		Vector3f rightNear = tempB.set(right).mul(wNearHalf);
		
		// Near Top Left
		frustumPoints[1][0][0].set(nearC);
		frustumPoints[1][0][0].add(upNear);
		frustumPoints[1][0][0].sub(rightNear);
		
		// Near Top Right
		frustumPoints[1][0][1].set(nearC);
		frustumPoints[1][0][1].add(upNear);
		frustumPoints[1][0][1].add(rightNear);
		
		// Near Bottom Left
		frustumPoints[1][1][0].set(nearC);
		frustumPoints[1][1][0].sub(upNear);
		frustumPoints[1][1][0].sub(rightNear);
		
		// Near Bottom Right
		frustumPoints[1][1][1].set(nearC);
		frustumPoints[1][1][1].sub(upNear);
		frustumPoints[1][1][1].add(rightNear);
		
		// Setup the normals (points inwards) & d values (-n . p0)
		
		// (ftl - ntl) X (ntr - ntl)
		frustumNormals[TOP].set(computeNormal(
				frustumPoints[1][0][0],
				frustumPoints[0][0][0],
				frustumPoints[1][0][1])
		);
		// -n . ntl
		frustumDvals[TOP] = tempA.set(frustumNormals[TOP]).mul(-1).dot(
				frustumPoints[1][0][0]
		);
		
		// (fbl - nbr) X (nbl - nbr)
		frustumNormals[BOTTOM].set(computeNormal(
				frustumPoints[1][1][1],
				frustumPoints[0][1][0],
				frustumPoints[1][1][0])
		);
		// -n . nbr
		frustumDvals[BOTTOM] = tempA.set(frustumNormals[BOTTOM]).mul(-1).dot(
				frustumPoints[1][1][1]
		);
		
		// (fbl - nbl) X (ntl - nbl)
		frustumNormals[LEFT].set(computeNormal(
				frustumPoints[1][1][0],
				frustumPoints[0][1][0],
				frustumPoints[1][0][0])
		);
		// -n . nbl
		frustumDvals[LEFT] = tempA.set(frustumNormals[LEFT]).mul(-1).dot(
				frustumPoints[1][1][0]
		);
		
		// (ftr - ntr) X (nbr - ntr)
		frustumNormals[RIGHT].set(computeNormal(
				frustumPoints[1][0][1],
				frustumPoints[0][0][1],
				frustumPoints[1][1][1])
		);
		// -n . ntr
		frustumDvals[RIGHT] = tempA.set(frustumNormals[RIGHT]).mul(-1).dot(
				frustumPoints[1][0][1]
		);
		
		// Near plane: use nearC as a point
		frustumNormals[NEAR].set(forward);
		frustumDvals[NEAR] = forward.mul(-1).dot(nearC);
		
		// Far plane: use farC as a point
		frustumNormals[FAR].set(forward);
		frustumDvals[FAR] = forward.mul(-1).dot(forward.mul(zFar));
	}
	
	private Vector3f computeNormal(Vector3f p0, Vector3f p1, Vector3f p2)
	{
		Vector3f v = new Vector3f(p1).sub(p0);
		Vector3f u = new Vector3f(p2).sub(p0);
		return v.cross(u).normalize();
	}
	
	private void resetBasisAndPoints()
	{
		right  .set(1, 0,  0);
		up     .set(0, 1,  0);
		forward.set(0, 0, -1);
		
		for (int i = 0; i < 8; i++)
			frustumPoints[i / 4][(i / 2) % 2][i % 2].set(0);
		for (int i = 0; i < 6; i++)
		{
			frustumNormals[i].set(0);
			frustumDvals[i] = 0;
		}
	}
	
	/**
	 * Tests if a given sphere is inside of the frustum
	 * @param x The centre x position of the sphere
	 * @param y The centre y position of the sphere
	 * @param z The centre z position of the sphere
	 * @param radius The radius of the sphere
	 * @return True if the sphere is inside the frustum
	 */
	public boolean isSphereInside(float x, float y, float z, float radius)
	{
		for (int i = 0; i < frustumNormals.length; i++)
		{
			float dist = frustumNormals[i].dot(x, y, z) + frustumDvals[i];
			if (dist < -radius)
				return false;
		}
		
		return true;
	}
	
}
