package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
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
	// Derived from the player's jump height
	// g = 2 * h * t^2
	public float gravity = ((2.0f * 1.25f) * (3f * 3f));
	
	// The orientation of the entity
	public float pitch = 0.0f, yaw = 0.0f;
	
	// Eye height of the entity
	public float eyeHeight = 1.45f;
	
	// If the entity is on the ground
	public boolean onGround = false;
	
	// If the entity is dead or not
	public boolean isDead = false;
	
	// If the entity will be removed from the world or not
	public boolean isRemoved = false;
	
	// The collision box of the entity
	public AABBCollider collisionBox;
	
	/**
	 * The world the entity is currently in
	 */
	public World world;
	
	public Entity()
	{
		// By default, the y acceleration is the gravity
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
	
	/// Override-ables ///
	public void update(float delta) {}
	
	/**
	 * Sets the entity to be dead
	 */
	public void setDead()
	{
		isDead = true;
		isRemoved = true;
	}
	
	/**
	 * Revives the entity from death
	 */
	public void setUndead()
	{
		isDead = false;
		isRemoved = false;
	}
	
	/// Utility (Move to separate util classes) ///
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
	
	protected int testForCollisionY(float delta)
	{
		int yDir = (int)Math.signum(yVel);
		int blockDelta = 0;
		
		// If the entity is not moving, no collision will happen
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
		
		// Check the 3x3 area around the entity
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
	protected int testForCollisionZ(float delta)
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
	protected int testForCollisionX(float delta)
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
