package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.world.World;

public class Entity
{
	// The position of the entity
	public float xPos, yPos, zPos;
	
	// The velocity of the entity
	public float xVel = 0.0f, yVel = 0.0f, zVel = 0.0f;
	
	// The acceleration of the entity
	public float xAccel = 0.0f, yAccel, zAccel = 0.0f;
	
	// The gravity of the entity
	// Derived from the player's jump height
	// g = 2 * h * t^2
	public final float gravity = ((2.0f * 1.25f) * (3f * 3f));
	
	// The orientation of the entity
	public float pitch = 0.0f, yaw = 0.0f;
	
	// Eye height of the entity
	public final float eyeHeight = 1.45f;
	
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
	
	public boolean isInWater()
	{
		Block blockIn = world.getBlock((int)Math.floor(xPos), (int)Math.floor(yPos + eyeHeight), (int)Math.floor(zPos));
		return blockIn == Blocks.WATER || blockIn == Blocks.UPDATING_WATER;
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
	
	protected void updateCollision (float delta)
	{
		// Move the collision box to the current entity position
		collisionBox.setPosition(xPos, yPos, zPos);
		collisionBox.add(-collisionBox.width / 2f, 0, -collisionBox.depth / 2f);
		
		float xResponse = testForCollisionX(delta);
		float yResponse = testForCollisionY(delta);
		float zResponse = testForCollisionZ(delta);
		
		// Fix an entity being able to stick on a ceiling
		// (check for yVel < 0)
		onGround = yVel < 0 && yResponse >= 0 && yVel != yResponse;
		
		if (yResponse != yVel)
		{
			if (Math.abs(yResponse - yVel) > 1/64f)
				yPos += yResponse;
			yVel = 0;
		}
		
		if (zResponse != zVel)
		{
			if (Math.abs(zResponse - zVel) > 1/64f)
				zPos += zResponse;
			zVel = 0;
		}
		
		if (xResponse != xVel)
		{
			if (Math.abs(xResponse - xVel) > 1/64f)
				xPos += xResponse;
			xVel = 0;
		}
	}
	
	protected float testForCollisionY(float delta)
	{
		int yDir = (int)Math.signum(yVel);
		int blockDelta = 0;
		
		// If the entity is not moving, no collision will happen
		if (yDir == 0)
			return yVel;
		
		// Setup the block delta
		if (yDir == -1)
			blockDelta = -1;
		else if (yDir == 1)
			blockDelta = Math.round(collisionBox.height);
		
		int blockX, blockY, blockZ;
		blockX = (int) Math.floor(xPos);
		blockY = (int) Math.round(yPos);
		blockZ = (int) Math.floor(zPos);
		
		// Have a dummy collision box setup
		AABBCollider blockCollider = new AABBCollider(0, 0, 0, 0, 0, 0);
		
		// Check the 3x3 area around the entity
		for (int xOff = -1; xOff <= 1; xOff++)
		{
			for (int zOff = -1; zOff <= 1; zOff++)
			{
				Block block = world.getBlock(blockX + xOff, blockY + blockDelta, blockZ + zOff);
				
				// Check if the colliding block is solid
				if (!block.isSolid())
					continue;
				
				// Setup the collision box for the colliding block
				blockCollider.asAABB(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + blockDelta, blockZ + zOff);
				
				float yOff = collisionBox.offsetOnY(blockCollider, yVel * delta);
				if (yOff != yVel * delta)
					return yOff;
			}
		}
		
		// No collisions found, return same yVel
		return yVel;
	}
	
	// Return is an offset to correct the collision
	protected float testForCollisionZ(float delta)
	{
		int zDir = (int)Math.signum(zVel);
		
		if (zDir == 0)
			return zVel;
		
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Have a dummy collision box setup
		AABBCollider blockCollider = new AABBCollider(0, 0, 0, 0, 0, 0);
		
		for(int yOff = -1; yOff <= 1; yOff++)
		{
			for (int xOff = -1; xOff <= 1; xOff++)
			{
				Block block = world.getBlock(blockX + xOff, blockY + yOff, blockZ + zDir);
				
				if (!block.isSolid())
					continue;
				
				// Setup the collision box for the colliding block
				blockCollider.asAABB(block.getCollisionBox());
				blockCollider.setPosition(blockX + xOff, blockY + yOff, blockZ + zDir);
				
				float zOff = collisionBox.offsetOnZ(blockCollider, zVel * delta);
				if (zOff != zVel * delta)
					return zOff;
			}
		}
		
		// No collision detected
		return zVel;
	}
	
	// Return is a signed number of steps to the collision
	protected float testForCollisionX(float delta)
	{
		int xDir = (int)Math.signum(xVel);
		
		if (xDir == 0)
			return xVel;
		
		int blockX, blockY, blockZ;
		blockX = Math.round(xPos - 0.5f);
		blockY = Math.round(yPos);
		blockZ = Math.round(zPos - 0.5f);
		
		// Have a dummy collision box setup
		AABBCollider blockCollider = new AABBCollider(0, 0, 0, 0, 0, 0);
		
		for(int yOff = -1; yOff <= 1; yOff++)
		{
			for (int zOff = -1; zOff <= 1; zOff++)
			{
				Block block = world.getBlock(blockX + xDir, blockY + yOff, blockZ + zOff);
				
				if (!block.isSolid())
					continue;
				
				// Setup the collision box for the colliding block
				blockCollider.asAABB(block.getCollisionBox());
				blockCollider.setPosition(blockX + xDir, blockY + yOff, blockZ + zOff);
				
				float xOff = collisionBox.offsetOnX(blockCollider, xVel * delta);
				if (xOff != xVel * delta)
					return xOff;
			}
		}
		
		// No collision detected
		return xVel;
	}
	
}
