package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.AABBCollider;

public class EntityPlayer extends Entity
{
	// Horizontal speed
	public float speed = 3f;
	
	// Speed Coefficient
	public float speedCoef = 1f;
	
	// Ramp Up/Down Coefficient
	public float rampUpCoeff = 8.5f;
	public float rampDownCoeff = 8.5f;
	
	// Horizontal Acceleration
	public float horizontalAccel = speed * speed;
	// Horizontal Coeff
	public float horizontalCoeff = speedCoef * speedCoef;
	
	// Jump related
	private float targetHeight = 1.25f;
	private float targetTime = 3f;
	
	private float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	
	// If the player is currently jumping
	public boolean isJumping = false;
	
	// If the player is sneaking
	public boolean isSneaking = false;
	
	// If the player is flying
	public boolean isFlying = false;
	
	public EntityPlayer()
	{
		super();
		gravity = ((2.0f * targetHeight) * (targetTime * targetTime));
		
		// By default, the y acceleration is just gravity
		yAccel = -gravity;
		
		collisionBox = new AABBCollider(0.0f, 0.0f, 0.0f, 0.5f, 1.7f, 0.5f);
	}
	
	
	
	/**
	 * Pushes the player to the specified direction
	 * The acceleration is relative to the current direction
	 * @param xAccel The acceleration in the x direction
	 * @param zAccel The acceleration in the z direction
	 */
	public void move(float xAccel, float zAccel)
	{
		if (xAccel == 0 && zAccel == 0)
		{
			this.xAccel = 0;
			this.zAccel = 0;
			return;
		}
		
		double phi = Math.toRadians(yaw);
		double xDir =  xAccel * Math.cos(phi) + zAccel * Math.sin(phi);
		double zDir = -xAccel * Math.sin(phi) + zAccel * Math.cos(phi);
		
		// Normalize the horizontal movement vector
		double mag = Math.sqrt(Math.pow(xDir, 2) + Math.pow(zDir, 2));
		
		xDir /= mag;
		zDir /= mag;
		
		this.xAccel = (float) (xDir * speed * speedCoef) * rampUpCoeff;
		this.zAccel = (float) (zDir * speed * speedCoef) * rampUpCoeff;
	}
	
	public void jump()
	{
		if(!onGround && !isFlying)
			return;
		
		this.yVel = jumpVelocity;
		onGround = false;
		isJumping = true;
	}
	
	public void update(float delta)
	{
		// Change the yAccel if the player is flying or not
		if (isFlying)
			yAccel = 0f;
		else
			yAccel = -gravity;
		
		// Apply the acceleration
		xVel += xAccel * delta;
		yVel += yAccel * delta;
		zVel += zAccel * delta;
		
		// Clamp the horizontal velocities
		xVel = clamp(xVel, -speed * speedCoef, speed * speedCoef);
		zVel = clamp(zVel, -speed * speedCoef, speed * speedCoef);
		
		// Clamp the vertical velocity if flying
		if (isFlying)
			yVel = clamp(yVel, -jumpVelocity, jumpVelocity);
		else
			yVel = clamp(yVel, -gravity * 2, yVel);
		
		if (isFlying && isSneaking)
			yVel = -jumpVelocity;
		
		// Update the collision status
		// Apply the response in the event of a collision
		int zResponse, xResponse;
		if ((zResponse = testForCollisionZ(delta)) != 0)
		{
			zPos += ((Math.abs(zResponse) - 2) / 16f) * zVel * delta;
			zVel = 0;
			zAccel = 0;
		}
		
		if ((xResponse = testForCollisionX(delta)) != 0)
		{
			xPos += ((Math.abs(xResponse) - 2) / 16f) * xVel * delta;
			xVel = 0;
			xAccel = 0;
		}
		float yResponse = testForCollisionY(delta);
		
		onGround = yResponse < 0;
		
		if((onGround && yVel < 0) || (yResponse > 0 && yVel > 0))
		{
			// Apply the response
			if ((Math.abs(yResponse) - 2) == -1)
			{
				// Snap to the nearest Y position, plus a bit
				yPos = Math.round(yPos) - (1 / 16f) * yVel * delta;
			}
			else
			{
				// Do the normal response
				yPos += ((Math.abs(yResponse) - 2) / 16f) * yVel * delta;
			}
			
			yVel = 0;
			
			// Reset jumping status
			isJumping = false;
		}
		
		// Apply the velocity
		xPos += xVel * delta;
		yPos += yVel * delta;
		zPos += zVel * delta;
		
		if (xVel != 0.0f || yVel != 0.0f || zVel != 0.0f)
		{
			int blockX = Math.round(xPos - 0.5f);
			int blockY = Math.round(yPos);
			int blockZ = Math.round(zPos - 0.5f);
			
			//System.out.println("P (" + xPos + ", " + yPos + ", " + zPos + ")");
			//System.out.println("B (" + blockX + ", " + blockY + ", " + blockZ + ")");
			//System.out.println("V (" + xVel + ", " + yVel + ", " + zVel + ")");
			//System.out.println("-----------------------------------");
		}
		
		// Apply decay to the velocity
		if(Math.abs(xVel) > delta)
			xVel = lerpf(xVel, 0, delta * rampDownCoeff);
		else
			xVel = 0;
		
		if(Math.abs(zVel) > delta)
			zVel = lerpf(zVel, 0, delta * rampDownCoeff);
		else
			zVel = 0;
		
		if (isFlying)
			yVel = decay(yVel, 0.5f);
	}
	
	private float lerpf(float start, float end, float step)
	{
		return (start * (1 - step)) + (end * step);
	}
	
	private int testForCollisionY(float delta)
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
				
				// Test for a rough collision
				collisionBox.add(0, yVel * delta, 0);
				boolean collides = collisionBox.relativeIntersectionWith(block.getCollisionBox(), blockX + xOff, blockY + blockDelta, blockZ + zOff);
				collisionBox.add(0, -(yVel * delta), 0);
				
				// If no collision will happen, don't check for it
				if (!collides)
					continue;
				
				// Setup the collision box for fine stepping
				AABBCollider blockCollider = new AABBCollider(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + blockDelta, blockZ + zOff);
				
				// Do fine stepping
				float stepY = (yVel * delta) / 16f;
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
	private int testForCollisionZ(float delta)
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
				
				collisionBox.add(0, 0, zVel * delta);
				boolean collidesZ = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(0, 0, -zVel * delta);
				
				if (!collidesZ)
					continue;
				
				// Collision will happen, do fine stepping
				float stepZ = (zVel * delta) / 16f;
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
	private int testForCollisionX(float delta)
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
				
				collisionBox.add(xVel * delta, 0, 0);
				boolean collidesX = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(-xVel * delta, 0, 0);
				
				if (!collidesX)
					continue;
				
				// Collision will happen, do fine stepping
				float stepX = (xVel * delta) / 16f;
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
	
}
