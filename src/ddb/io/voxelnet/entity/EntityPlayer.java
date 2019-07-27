package ddb.io.voxelnet.entity;

public class EntityPlayer
{
	// The position of the player
	public float xPos, yPos, zPos;
	
	// The velocity of the player
	public float xVel = 0.0f, yVel = 0.0f, zVel = 0.0f;
	
	// Movement speed
	public float speed = 4.0f / 60.0f;
	
	// The orientation of the player
	public float pitch = 0.0f, yaw = 0.0f;
	
	public EntityPlayer()
	{
	
	}
	
	/// Helpers ///
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
		this.pitch = clampf(this.pitch, -90.0f, 90.0f);
		
		// Keep the yaw between [0, 360]
		this.yaw %= 360.0f;
		
		if (this.yaw < 0.0f)
			this.yaw += 360.0f;
	}
	
	/**
	 * Pushes the player to the specified direction
	 * The acceleration is relative to the current direction
	 * @param xAccel The acceleration in the x direction
	 * @param yAccel The acceleration in the y direction
	 * @param zAccel The acceleration in the z direction
	 */
	public void move(float xAccel, float yAccel, float zAccel)
	{
		double phi = Math.toRadians(yaw);
		double xDir =  xAccel * Math.cos(phi) + zAccel * Math.sin(phi);
		double zDir = -xAccel * Math.sin(phi) + zAccel * Math.cos(phi);
		
		// Normalize the horizontal movement vector
		double mag = Math.sqrt(Math.pow(xDir, 2) + Math.pow(zDir, 2));
		
		if (mag <= 0.0f)
			mag = 1.0f;
		
		xDir /= mag;
		zDir /= mag;
		
		this.xVel += speed * (float) xDir;
		this.yVel += speed * yAccel;
		this.zVel += speed * (float) zDir;
		
		// Clamp the velocities
		this.xVel += clampf(this.xVel, -speed, speed);
		this.yVel += clampf(this.yVel, -speed, speed);
		this.zVel += clampf(this.zVel, -speed, speed);
	}
	
	public void update()
	{
		// Apply the velocity
		xPos += xVel;
		yPos += yVel;
		zPos += zVel;
		System.out.println(yaw);
		
		// Apply decay to the velocity
		xVel = 0.0f;//decay(xVel, 0.5f);
		yVel = 0.0f;//decay(yVel, 0.5f);
		zVel = 0.0f;//decay(zVel, 0.5f);
	}
	
	/**
	 * Clamps the value to the specified range
	 * @param value The value to clamp
	 * @param min The minimum bound for the value
	 * @param max The maximum bound for the value
	 * @return The clamped value
	 */
	private float clampf(float value, float min, float max)
	{
		if (value > max)
			return max;
		return Math.max(value, min);
	}
	
	private float decay(float value, float decayFactor)
	{
		if (Math.abs(value) > speed)
			return value * decayFactor;
		else
			return decayFactor;
	}
}