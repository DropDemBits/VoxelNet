package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.AABBCollider;

public class EntityPlayer extends Entity
{
	// Horizontal speed
	public float speed = 3f;
	
	// Speed Coefficient
	public float speedCoef = 1f;
	public float accelCoef = 1f;
	
	// Ramp Up/Down Coefficient
	public float rampUpCoeff = 10.5f;
	public float slipperiness = 10.5f;
	
	// Jump related
	private float targetHeight = 1.25f;
	private float targetTime = 3f;
	
	private float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	
	// If the player is sneaking
	public boolean isSneaking = false;
	
	// If the player is flying
	public boolean isFlying = false;
	
	public EntityPlayer()
	{
		super();
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
	}
	
	public void update(float delta)
	{
		if (Block.idToBlock(world.getBlock((int)Math.floor(xPos), (int)Math.floor(yPos), (int)Math.floor(zPos))) == Blocks.WATER)
			accelCoef = 0.375f;
		else
			accelCoef = 1f;
		
		// Change the yAccel if the player is flying or not
		if (isFlying)
			yAccel = 0f;
		else
			yAccel = -gravity;
		
		// Apply the acceleration
		xVel += accelCoef * xAccel * delta;
		zVel += accelCoef * zAccel * delta;
		
		if (yVel > 0)
			yVel += 1f * yAccel * delta;
		else
			yVel += accelCoef * yAccel * delta;
		
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
		updateCollision(delta);
		
		// Apply the velocity
		xPos += xVel * delta;
		yPos += yVel * delta;
		zPos += zVel * delta;
		
		// Apply exponential decay to the velocity
		if(Math.abs(xVel) > 1/256f)
			xVel = lerpf(xVel, 0, slipperiness * accelCoef * delta);
		else
			xVel = 0;
		
		if(Math.abs(zVel) > 1/256f)
			zVel = lerpf(zVel, 0, slipperiness * accelCoef * delta);
		else
			zVel = 0;
		
		if (isFlying)
			yVel = decay(yVel, 0.5f);
	}
	
	private float lerpf(float start, float end, float step)
	{
		return (start * (1 - step)) + (end * step);
	}
	
}
