package ddb.io.voxelnet.util;

public class AABBCollider
{
	public float x, y, z;
	public float width, height, depth;
	
	/**
	 * Constructs an AABBCollider using another one
	 * @param other The other AABBCollider to setup from
	 */
	public AABBCollider(AABBCollider other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.width = other.width;
		this.height = other.height;
		this.depth = other.depth;
	}
	
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
	
	/**
	 * Checks if this box intersects with the other box
	 * @param other The other box to check for intersection
	 * @return True if the boxes intersect
	 */
	public boolean intersectsWith(AABBCollider other)
	{
		return     x <= other.x + other.width  && other.x <= x + width
				&& y <= other.y + other.height && other.y <= y + height
				&& z <= other.z + other.depth  && other.z <= z + depth;
	}
	
	public boolean relativeIntersectionWith(AABBCollider other, float xOther, float yOther, float zOther)
	{
		return     (x - xOther) <= other.x + other.width  && other.x <= (x - xOther) + width
				&& (y - yOther) <= other.y + other.height && other.y <= (y - yOther) + height
				&& (z - zOther) <= other.z + other.depth  && other.z <= (z - zOther) + depth;
	}
	
	/**
	 * Checks if this box intersects with the other box, accounting for
	 * velocity.
	 * The other box must be in a fixed position
	 * @param other The other, static box to check for intersection
	 * @param xVel The x velocity of this box
	 * @param yVel The y velocity of this box
	 * @param zVel The z velocity of this box
	 * @return True if an intersection occurred, with the response in teh
	 */
	/*public boolean preciseIntersection(AABBCollider other, float xVel, float yVel, float zVel, float[] response)
	{
		// Intersection currently, stop
		if (intersectsWith(other))
			return false;
		
		// Quick Test: Will there be an intersection?
		add(xVel, yVel, zVel);
		boolean doesIntersect = intersectsWith(other);
		add(-xVel, -yVel, -zVel);
		
		if (!doesIntersect)
			return false;
		
		float initialX = x;
		float initialY = y;
		float initialZ = z;
		
		// Testing: Use the y axis
		// Step by 16/th of the velocity until an intersection is found
		float stepX = xVel / 16f;
		float stepY = yVel / 16f;
		float stepZ = zVel / 16f;
		
		for(int steps = 1; steps <= 16; steps++)
		{
			add(stepX, stepY, stepZ);
			if (intersectsWith(other))
			{
				setPosition(initialX, initialY, initialZ);
				return true;
			}
		}
		
		setPosition(initialX, initialY, initialZ);
		// No intersection found
		return false;
	}*/
	
	/**
	 * Adds an offset to the position
	 * @param xOff The x offset to add
	 * @param yOff The y offset to add
	 * @param zOff The z offset to add
	 */
	public void add(float xOff, float yOff, float zOff)
	{
		x += xOff;
		y += yOff;
		z += zOff;
	}
	
	/**
	 * Grows the collision box in the specified axis
	 * @param xOff The amount to grow in the x direction
	 * @param yOff The amount to grow in the y direction
	 * @param zOff The amount to grow in the z direction
	 */
	public void grow(float xOff, float yOff, float zOff)
	{
		width  += xOff;
		height += yOff;
		depth  += zOff;
	}
}
