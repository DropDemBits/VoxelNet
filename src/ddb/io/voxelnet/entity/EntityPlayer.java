package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.world.World;

import java.util.Arrays;

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
	private float targetTime = 3f / 60.0f;
	
	private float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	public float gravity = ((2.0f * targetHeight) * (targetTime * targetTime));
	
	// If the player is on the ground
	public boolean onGround = false;
	
	// If the player is currently jumping
	public boolean isJumping = false;
	
	/**
	 * The world the player is currently in
	 */
	public World world;
	
	public AABBCollider collisionBox = new AABBCollider(0.0f, 0.0f, 0.0f, 0.5f, 2.0f, 0.5f);
	
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
		
		this.xAccel = (float) (speed * xDir) / 2.0f;
		this.zAccel = (float) (speed * zDir) / 2.0f;
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
		// Apply the acceleration
		xVel += xAccel;
		yVel += yAccel;
		zVel += zAccel;
		
		// Clamp the horizontal velocities
		xVel = clamp(xVel, -speed, speed);
		zVel = clamp(zVel, -speed, speed);
		
		// Update the collision status
		// Test for y-axis collision
		int yCollision = testForCollisionY();
		
		onGround = yCollision == -1;
		
		if(onGround && yVel < 0)
		{
			yVel = 0;
			
			// Reset jumping status
			isJumping = false;
		}
		else if (!onGround && yCollision == 1 && yVel > 0)
		{
			yVel = 0;
			isJumping = false;
			System.out.println("heyhey");
		}
		
		int horizontalCollision = testForCollisionHorizontal();
		if ((horizontalCollision & 0x0F) != 0)
			xVel = 0;
		if ((horizontalCollision & 0xF0) != 0)
			zVel = 0;
		
		// Apply the velocity
		xPos += xVel;
		yPos += yVel;
		zPos += zVel;
		
		if (xVel != 0.0f || yVel != 0.0f || zVel != 0.0f)
		{
			int blockX = Math.round(xPos - 0.5f);
			int blockY = Math.round(yPos);
			int blockZ = Math.round(zPos - 0.5f);
			
			//System.out.println("(" + xPos + ", " + yPos + ", " + zPos + ") B ");
			System.out.println("(" + blockX + ", " + blockY + ", " + blockZ + ") V ");
			/*System.out.println("(" + xVel + ", " + yVel + ", " + zVel + ")");*/
			/*System.out.println(Block.idToBlock(world.getBlock(blockX, blockY - 1, blockZ)).isSolid());
			System.out.println(Block.idToBlock(world.getBlock(blockX, blockY + 1, blockZ)).isSolid());
			System.out.println(Block.idToBlock(world.getBlock(blockX, blockY + 2, blockZ)).isSolid());*/
		}
		
		// Apply decay to the velocity
		xVel = decay(xVel, 0.5f);
		zVel = decay(zVel, 0.5f);
		//System.out.println("NoMove " + noMove + ", NotSolid " + notSolid + ", NotBelow" + notBelow + ", EarlyExit " + earlyExit + ", NotFineBelow " + notFineBelow);
	}
	
	int noMove = 0;
	int notSolid = 0;
	int notBelow = 0;
	int earlyExit = 0;
	int notFineBelow = 0;
	private int testForCollisionY()
	{
		int yDir = (int)Math.signum(yVel);
		int blockDelta = 0;
		
		// If the player is not moving, no collision will happen
		if (yDir == 0)
		{
			noMove++;
			return 0;
		}
		
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
		collisionBox.setPosition(xPos - blockX, yPos - blockY, zPos - blockZ);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		Block block = Block.idToBlock(world.getBlock(blockX, blockY + blockDelta, blockZ));
		
		// Check if the colliding block is solid
		if (!block.isSolid())
		{
			notSolid++;
			return 0;
		}
		
		
		collisionBox.add(0, -blockDelta, 0);
		collisionBox.add(0, yVel, 0);
		boolean collides = collisionBox.intersectsWith(block.getCollisionBox());
		collisionBox.add(0, -yVel, 0);
		
		// If no collision will happen, don't check for it
		if(!collides)
		{
			notBelow++;
			return 0;
		}
		
		// Do fine stepping
		float stepY = yVel / 16f;
		for(int step = 0; step <= 16; step++)
		{
			if(collisionBox.intersectsWith(block.getCollisionBox()))
			{
				earlyExit++;
				return yDir;
			}
			collisionBox.add(0, stepY, 0);
			yPos += stepY;
		}
		
		notFineBelow++;
		
		// No collision, even with optimizations
		System.out.println("HOY! Something's wrong here");
		return 0;
	}
	
	// Was: Working on collision
	// Return is a bitmap of which ones to zero out
	// Bit 0-3 - XVel Direction
	// Bit 4-7 - ZVel Direction
	private int testForCollisionHorizontal()
	{
		int zDir = (int)Math.signum(zVel);
		
		if (zDir == 0)
			return 0;
		
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Move the collision box relative to the block position
		//collisionBox.setPosition(xPos - blockX, yPos - blockY, zPos - blockZ);
		collisionBox.setPosition(xPos, yPos, zPos);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		/*System.out.println("P (" + collisionBox.x + ", " + collisionBox.y + ", " + collisionBox.z + ")");
		System.out.println("B (" + collisionBox.x + ", " + collisionBox.y + ", " + collisionBox.z + ")");*/
		
		//collisionBox.add(0, 0, 0.0f);
		//System.out.println(zDir);
		
		for(int yOff = 0; yOff <= 1; yOff++)
		{
			for (int xOff = -1; xOff <= 1; xOff++)
			{
				Block block = Block.idToBlock(world.getBlock(blockX + xOff, blockY + yOff, blockZ + zDir));
				
				if (!block.isSolid())
					continue;
				
				AABBCollider blockCollider = new AABBCollider(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + yOff, blockZ + zDir);
				
				System.out.println(
						"P (" + collisionBox.x + ", " + collisionBox.y + ", " + collisionBox.z + ") - ("
								+ (collisionBox.x + collisionBox.width) + ", " + (collisionBox.y + collisionBox.height) + ", " + (collisionBox.z + collisionBox.depth) + ")");
				System.out.println("B (" + blockCollider.x + ", " + blockCollider.y + ", " + blockCollider.z + ") - ("
						+ (blockCollider.x + blockCollider.width) + ", " + (blockCollider.y + blockCollider.height) + ", " + (blockCollider.z + blockCollider.depth) + ")");
				
				//collisionBox.add(xOff, 0, 0);
				
				collisionBox.add(0, 0, zVel);
				System.out.println(
						"D (" + collisionBox.x + ", " + collisionBox.y + ", " + collisionBox.z + ") - ("
								+ (collisionBox.x + collisionBox.width) + ", " + (collisionBox.y + collisionBox.height) + ", " + (collisionBox.z + collisionBox.depth) + ")");
				
				boolean collidesZ = collisionBox.intersectsWith(blockCollider);
				collisionBox.add(0, 0, -zVel);
				
				if (!collidesZ)
					continue;
				
				// Collision will happen, do fine stepping
				float stepZ = zVel / 16f;
				for (int step = 0; step <= 16; step++)
				{
					if (collisionBox.intersectsWith(blockCollider))
					{
						int bitmap = 0;
						// Return the appropriate bitmap
						bitmap |= (zDir & 0xF) << 4;
						
						System.out.println("Hey!");
						return bitmap;
					}
					
					// Only step on the colliding axis
					if(collidesZ)
					{
						collisionBox.add(0, 0, stepZ);
						zPos += stepZ;
					}
					
					if (step == 16)
						System.out.println("HOYZ! Somethings wrong here!");
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
