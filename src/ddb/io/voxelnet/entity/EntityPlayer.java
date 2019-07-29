package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.world.World;

public class EntityPlayer
{
	// The position of the player
	public float xPos, yPos, zPos;
	
	// The velocity of the player
	public float xVel = 0.0f, yVel = 0.0f, zVel = 0.0f;
	
	// The acceleration of the player
	public float xAccel = 0.0f, yAccel = 0.0f, zAccel = 0.0f;
	
	// Movement speed
	public float speed = 4.0f / 60.0f;
	
	// The orientation of the player
	public float pitch = 0.0f, yaw = 0.0f;
	
	private float targetHeight = 1.5f;
	private float targetTime = 4f / 60.0f;
	
	private float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	private float jumpGravity  = ((2.0f * targetHeight) * (targetTime * targetTime));
	public float gravity = jumpGravity;
	
	// If the player is on the ground
	public boolean onGround = false;
	
	// If the player is currently jumping
	public boolean isJumping = false;
	
	/**
	 * The world the player is currently in
	 */
	public World world;
	
	public AABBCollider collisionBox = new AABBCollider(0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 1.0f);
	
	public EntityPlayer()
	{
		// By default, the y acceleration is just gravity
		yAccel = -gravity;
	}
	
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
	
	/**
	 * Pushes the player to the specified direction
	 * The acceleration is relative to the current direction
	 * @param xAccel The acceleration in the x direction
	 * @param zAccel The acceleration in the z direction
	 */
	public void move(float xAccel, float zAccel)
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
		
		this.xAccel = (float) (speed * xDir);
		this.zAccel = (float) (speed * zDir);
	}
	
	public void jump()
	{
		if(!onGround)
			return;
		
		this.yVel = jumpVelocity;
		onGround = false;
		isJumping = true;
	}
	
	public void update()
	{
		if (isJumping)
			yAccel = -jumpGravity;
		else
			yAccel = -gravity;
		
		// Apply the acceleration
		xVel += xAccel;
		yVel += yAccel;
		zVel += zAccel;
		
		// Clamp the velocities
		xVel = clamp(xVel, -speed, speed);
		//yVel = clamp(yVel, yAccel, yVel);
		zVel = clamp(zVel, -speed, speed);
		
		
		// Update the collision status
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos - 0.5f);
		blockZ = Math.round(zPos - 0.5f);
		
		// Move the collision box relative to the block position
		collisionBox.setPosition(xPos - blockX, yPos - blockY, zPos - blockZ);
		
		// Test for y-axis collision
		if((onGround = testForCollision(0, 0, 0)) && yVel < 0)
		{
			// Zero y velocity on collision
			yVel = 0;
			isJumping = false;
		}
		
		// Apply the velocity
		xPos += xVel;
		yPos += yVel;
		zPos += zVel;
		
		// Apply decay to the velocity
		xVel = decay(xVel, 0.5f);
		zVel = decay(zVel, 0.5f);
		
		  System.out.print("(" + xPos + ", " + yPos + ", " + zPos + ") - ");
		System.out.println("(" + xVel + ", " + yVel + ", " + zVel + ")");
	}
	
	private boolean testForCollision(int xOff, int yOff, int zOff)
	{
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f) + xOff;
		blockY = Math.round(yPos - 0.5f) + yOff;
		blockZ = Math.round(zPos - 0.5f) + zOff;
		
		Block block = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
		
		// Check if the colliding block is solid
		if (!block.isSolid())
			return false;
		
		collisionBox.add(xOff, yOff, zOff);
		boolean doesCollide = block.getCollisionBox().intersectsWith(collisionBox);
		collisionBox.add(-xOff, -yOff, -zOff);
		return doesCollide;
	}
	
	/**
	 * Clamps the value to the specified range
	 * @param value The value to clamp
	 * @param min The minimum bound for the value
	 * @param max The maximum bound for the value
	 * @return The clamped value
	 */
	private float clamp(float value, float min, float max)
	{
		if (value > max)
			return max;
		return Math.max(value, min);
	}
	
	private float decay(float value, float decayFactor)
	{
		if (value == 0.0f)
			return value;
		
		if (Math.abs(value) > 0.2f)
			return value * decayFactor;
		else
			return 0.0f;
	}
}
