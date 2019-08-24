package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.world.World;

public class Entity
{
	// The position of the entity
	public float xPos, yPos, zPos;
	
	// The velocity of the entity
	public float xVel = 0.0f, yVel = 0.0f, zVel = 0.0f;
	
	// The acceleration of the entity
	public float xAccel = 0.0f, yAccel = 0.0f, zAccel = 0.0f;
	
	// The gravity of the entity
	public float gravity;
	
	// The orientation of the entity
	public float pitch = -90.0f, yaw = 0.0f;
	
	// Eye height of the entity
	public float eyeHeight = 1.45f;
	
	// If the entity is on the ground
	public boolean onGround = false;
	
	public AABBCollider collisionBox;
	
	/**
	 * The world the entity is currently in
	 */
	public World world;
	
	public Entity() {}
	
	/// Helpers ///
	public void setWorld(World world)
	{
		this.world = world;
	}
	
	public void setPos(float x, float y, float z)
	{
		this.xPos = x;
		this.yPos = y;
		this.zPos = z;
	}
	
	public void setOrientation(float pitch, float yaw)
	{
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	public void rotate(float deltaPitch, float deltaYaw)
	{
		this.pitch += deltaPitch;
		this.yaw += deltaYaw;
		
		// Clamp the pitch to [-90, 90]
		this.pitch = clamp(this.pitch, -90.0f, 90.0f);
		
		// Keep the yaw between [0, 360]
		this.yaw %= 360.0f;
		
		if (this.yaw < 0.0f)
			this.yaw += 360.0f;
	}
	
	/// Override-ables ///
	public void update(float delta) {}
	
	/**
	 * Clamps the value to the specified range
	 * @param value The value to clamp
	 * @param min The minimum bound for the value
	 * @param max The maximum bound for the value
	 * @return The clamped value
	 */
	public float clamp(float value, float min, float max)
	{
		if (value > max)
			return max;
		return Math.max(value, min);
	}
	
	public float decay(float value, float decayFactor)
	{
		if (value == 0.0f)
			return value;
		
		if (Math.abs(value) > 0.2f)
			return value * decayFactor;
		else
			return 0.0f;
	}
	
}
