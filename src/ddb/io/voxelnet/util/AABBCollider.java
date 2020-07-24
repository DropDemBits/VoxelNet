package ddb.io.voxelnet.util;

public class AABBCollider
{
	// Used in computing collision response
	private static final float COLLISION_EPSILON = 1f/1024f;
	
	public float x, y, z;
	public float width, height, depth;
	// Offsets, only used in setPosition
	private float xOff, yOff, zOff;
	
	/**
	 * Constructs an AABBCollider using another one
	 * @param other The other AABBCollider to setup from
	 */
	public AABBCollider(AABBCollider other)
	{
		asAABB(other);
	}
	
	public AABBCollider(float x0, float y0, float z0, float w, float h, float d)
	{
		this.x = x0;
		this.y = y0;
		this.z = z0;
		this.xOff = x0;
		this.yOff = y0;
		this.zOff = z0;
		this.width  = w;
		this.height = h;
		this.depth  = d;
	}
	
	public void asAABB(AABBCollider other)
	{
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
		this.xOff = other.xOff;
		this.yOff = other.yOff;
		this.zOff = other.zOff;
		this.width = other.width;
		this.height = other.height;
		this.depth = other.depth;
	}
	
	public void setPosition(float x, float y, float z)
	{
		this.x = x + this.xOff;
		this.y = y + this.yOff;
		this.z = z + this.zOff;
	}
	
	/**
	 * Checks if the point intersects with this box
	 * @param x The x position to check for intersection
	 * @param y The y position to check for intersection
	 * @param z The z position to check for intersection
	 * @return True if the point intersects this box
	 */
	public boolean intersectsWith(float x, float y, float z)
	{
		return     this.x <= x  && x <= this.x + this.width
				&& this.y <= y  && y <= this.y + this.height
				&& this.z <= z  && z <= this.z + this.depth;
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
	
	public float offsetOnX(AABBCollider other, float xVel)
	{
		float velocity = xVel;
		
		// Quick fail: Outside of box on the y & z axis?
		if ((y > other.y + other.height || other.y > y + height)
				|| (z > other.z + other.depth || other.z > z + depth))
		{
			// Outside, keep xVel
			return velocity;
		}
		
		// positive velocity, may intersect w/ left
		if (xVel > 0 && (other.x + other.width) >= x)
		{
			float d = other.x - (x + width);
			
			// If distance is smaller, make it the offset
			if (d < velocity)
				velocity = d - COLLISION_EPSILON;
		}
		
		// negative velocity, may intersect w/ right
		if (xVel < 0 && (x + width) >= other.x)
		{
			float d = (other.x + other.width) - x;
			
			// If distance is smaller, make it the offset
			if (d > velocity)
				velocity = d + COLLISION_EPSILON;
		}
		
		return velocity;
	}
	
	public float offsetOnY(AABBCollider other, float yVel)
	{
		float velocity = yVel;
		
		// Quick fail: Outside of box on the x & z axis?
		if ((x > other.x + other.width || other.x > x + width)
				|| (z > other.z + other.depth || other.z > z + depth))
		{
			// Outside, keep yVel
			return velocity;
		}
		
		// positive velocity, may intersect w/ bottom
		if (yVel > 0 && (other.y + other.height) >= y)
		{
			float d = other.y - (y + height);
			
			// If distance is smaller, make it the offset
			if (d < velocity)
				velocity = d - COLLISION_EPSILON;
		}
		
		// negative velocity, may intersect w/ top
		if (yVel < 0 && (y + height) >= other.y)
		{
			float d = (other.y + other.height) - y;
			
			// If distance is smaller, make it the offset
			if (d > velocity)
				velocity = d + COLLISION_EPSILON;
		}
		
		return velocity;
	}
	
	public float offsetOnZ(AABBCollider other, float zVel)
	{
		float velocity = zVel;
		
		// Quick fail: Outside of box on the x & y axis?
		if ((y > other.y + other.height || other.y > y + height)
				|| (x > other.x + other.width || other.x > x + width))
		{
			// Outside, keep zVel
			return velocity;
		}
		
		// positive velocity, may intersect w/ left
		if (zVel > 0 && (other.z + other.depth) >= z)
		{
			float d = other.z - (z + depth);
			
			// If distance is smaller, make it the offset
			if (d < velocity)
				velocity = d - 1/256f;
		}
		
		// negative velocity, may intersect w/ right
		if (zVel < 0 && (z + depth) >= other.z)
		{
			float d = (other.z + other.depth) - z;
			
			// If distance is smaller, make it the offset
			if (d > velocity)
				velocity = d + 1/256f;
		}
		
		return velocity;
	}
	
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
	public AABBCollider grow(float xOff, float yOff, float zOff)
	{
		width  += xOff;
		height += yOff;
		depth  += zOff;
		
		return this;
	}
}
