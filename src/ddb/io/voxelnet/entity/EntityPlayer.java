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
	public float speed = 6.0f / 60.0f;
	// Speed Coefficient
	public float speedCoef = 1f;
	
	// The orientation of the player
	public float pitch = -90.0f, yaw = 0.0f;
	
	private float targetHeight = 1.25f;
	private float targetTime = 3f / 60.0f;
	
	private float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	public float gravity = ((2.0f * targetHeight) * (targetTime * targetTime));
	
	// Eye height of the player
	public float eyeHeight = 1.45f;
	
	// If the player is on the ground
	public boolean onGround = false;
	
	// If the player is currently jumping
	public boolean isJumping = false;
	
	// If the player is sneaking
	public boolean isSneaking = false;
	
	// If the player is flying
	public boolean isFlying = false;
	
	/**
	 * The world the player is currently in
	 */
	public World world;
	
	public AABBCollider collisionBox = new AABBCollider(0.0f, 0.0f, 0.0f, 0.5f, 1.7f, 0.5f);
	
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
		
		this.xAccel = (float) ((speed * speedCoef) * xDir) / 2.0f;
		this.zAccel = (float) ((speed * speedCoef) * zDir) / 2.0f;
	}
	
	public void jump()
	{
		if(!onGround && !isFlying)
			return;
		
		this.yVel = jumpVelocity;
		onGround = false;
		isJumping = true;
	}
	
	public void update()
	{
		// Change the yAccel if the player is flying or not
		if (isFlying)
			yAccel = 0f;
		else
			yAccel = -gravity;
		
		// Apply the acceleration
		xVel += xAccel;
		yVel += yAccel;
		zVel += zAccel;
		
		// Clamp the horizontal velocities
		xVel = clamp(xVel, -(speed * speedCoef), (speed * speedCoef));
		zVel = clamp(zVel, -(speed * speedCoef), (speed * speedCoef));
		
		// Clamp the vertical velocity if flying
		if (isFlying)
			yVel = clamp(yVel, -jumpVelocity, jumpVelocity);
		
		if (isFlying && isSneaking)
			yVel = -jumpVelocity;
		
		// Update the collision status
		// Apply the response in the event of a collision
		int zResponse, xResponse;
		if ((zResponse = testForCollisionZ()) != 0)
		{
			zPos += ((Math.abs(zResponse) - 2) / 16f) * zVel;
			zVel = 0;
			zAccel = 0;
		}
		
		if ((xResponse = testForCollisionX()) != 0)
		{
			xPos += ((Math.abs(xResponse) - 2) / 16f) * xVel;
			xVel = 0;
			xAccel = 0;
		}
		float yResponse = testForCollisionY();
		
		onGround = yResponse < 0;
		
		if((onGround && yVel < 0) || (yResponse > 0 && yVel > 0))
		{
			// Apply the response
			if ((Math.abs(yResponse) - 2) == -1)
			{
				// Snap to the nearest Y position, plus a bit
				yPos = Math.round(yPos) - (1 / 16f) * yVel;
			}
			else
			{
				// Do the normal response
				yPos += ((Math.abs(yResponse) - 2) / 16f) * yVel;
			}
			
			yVel = 0;
			
			// Reset jumping status
			isJumping = false;
		}
		
		// Apply the velocity
		xPos += xVel;
		yPos += yVel;
		zPos += zVel;
		
		if (xVel != 0.0f || yVel != 0.0f || zVel != 0.0f)
		{
			int blockX = Math.round(xPos - 0.5f);
			int blockY = Math.round(yPos);
			int blockZ = Math.round(zPos - 0.5f);
			
			//System.out.println("P (" + xPos + ", " + yPos + ", " + zPos + ")");
			System.out.println("B (" + blockX + ", " + blockY + ", " + blockZ + ")");
			//System.out.println("V (" + xVel + ", " + yVel + ", " + zVel + ")");
			//System.out.println("-----------------------------------");
		}
		
		// Apply decay to the velocity
		xVel = decay(xVel, 0.5f);
		zVel = decay(zVel, 0.5f);
		
		if (isFlying)
			yVel = decay(yVel, 0.5f);
	}
	
	private int testForCollisionY()
	{
		int yDir = (int)Math.signum(yVel);
		int blockDelta = 0;
		
		// If the player is not moving, no collision will happen
		if (yDir == 0)
			return 0;
		
		// Setup the block delta
		if (yDir == -1)
			blockDelta = -1;
		else if (yDir == 1)
			blockDelta = Math.round(collisionBox.height);
		
		// Was: Working on collision
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Move the collision box relative to the block position
		collisionBox.setPosition(xPos, yPos, zPos);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		// Check the 3x3 area around the player
		for (int xOff = -1; xOff <= 1; xOff++)
		{
			for (int zOff = -1; zOff <= 1; zOff++)
			{
				Block block = Block.idToBlock(world.getBlock(blockX + xOff, blockY + blockDelta, blockZ + zOff));
				
				// Check if the colliding block is solid
				if (!block.isSolid())
					continue;
				
				// Setup the collision box
				AABBCollider blockCollider = new AABBCollider(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + blockDelta, blockZ + zOff);
				
				collisionBox.add(0, yVel, 0);
				boolean collides = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(0, -yVel, 0);
				
				// If no collision will happen, don't check for it
				if (!collides)
					continue;
				
				// Do fine stepping
				float stepY = yVel / 16f;
				for (int step = 0; step <= 16; step++)
				{
					if (collisionBox.intersectsWith(blockCollider))
						return yDir * (step + 1);
					
					collisionBox.add(0, stepY, 0);
					
					if (step == 16)
						// No collision, even with optimizations
						System.out.println("HOYY! Something's wrong w/ collision");
				}
			}
		}
		
		// No collisions found
		return 0;
	}
	
	// Return is a signed number of steps to the collision
	private int testForCollisionZ()
	{
		int zDir = (int)Math.signum(zVel);
		
		if (zDir == 0)
			return 0;
		
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Move the collision box relative to the block position
		collisionBox.setPosition(xPos, yPos, zPos);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		for(int yOff = -1; yOff <= 1; yOff++)
		{
			for (int xOff = -1; xOff <= 1; xOff++)
			{
				Block block = Block.idToBlock(world.getBlock(blockX + xOff, blockY + yOff, blockZ + zDir));
				
				if (!block.isSolid())
					continue;
				
				AABBCollider blockCollider = new AABBCollider(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + yOff, blockZ + zDir);
				
				collisionBox.add(0, 0, zVel);
				boolean collidesZ = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(0, 0, -zVel);
				
				if (!collidesZ)
					continue;
				
				// Collision will happen, do fine stepping
				float stepZ = zVel / 16f;
				for (int step = 0; step <= 16; step++)
				{
					// Check for collision
					if (collisionBox.intersectsWith(blockCollider))
						return zDir * (step + 1);
					
					// Only step on the colliding axis
					collisionBox.add(0, 0, stepZ);
					
					if (step == 16)
						System.out.println("HOYZ! Somethings wrong here!");
				}
			}
		}
		
		// No collision detected
		return 0;
	}
	
	// Return is a signed number of steps to the collision
	private int testForCollisionX()
	{
		int xDir = (int)Math.signum(xVel);
		
		if (xDir == 0)
			return 0;
		
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Move the collision box relative to the block position
		collisionBox.setPosition(xPos, yPos, zPos);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		for(int yOff = -1; yOff <= 1; yOff++)
		{
			for (int zOff = -1; zOff <= 1; zOff++)
			{
				Block block = Block.idToBlock(world.getBlock(blockX + xDir, blockY + yOff, blockZ + zOff));
				
				if (!block.isSolid())
					continue;
				
				AABBCollider blockCollider = new AABBCollider(block.getCollisionBox());
				blockCollider.setPosition(blockX + xDir, blockY + yOff, blockZ + zOff);
				
				collisionBox.add(xVel, 0, 0);
				boolean collidesX = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(-xVel, 0, 0);
				
				if (!collidesX)
					continue;
				
				// Collision will happen, do fine stepping
				float stepX = xVel / 16f;
				for (int step = 0; step <= 16; step++)
				{
					// Check for collision
					if (collisionBox.intersectsWith(blockCollider))
						return xDir * (step + 1);
					
					// Only step on the colliding axis
					collisionBox.add(stepX, 0, 0);
					
					if (step == 16)
						System.out.println("HOYX! Somethings wrong here!");
				}
			}
		}
		
		// No collision detected
		return 0;
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
