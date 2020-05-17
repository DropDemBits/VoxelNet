package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.network.packet.PCSBreakBlock;
import ddb.io.voxelnet.network.packet.PCSPlaceBlock;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.MathUtil;
import ddb.io.voxelnet.util.RaycastResult;
import org.joml.Vector3d;

public class EntityPlayer extends Entity
{
	// Horizontal speed
	public final float speed = 3f;
	
	// Speed Coefficient
	public float speedCoef = 1f;
	public float accelCoef = 1f;
	
	// Ramp Up/Down Coefficient
	public final float rampUpCoeff = 10.5f;
	public final float slipperiness = 10.5f;
	
	// Jump related
	private final float targetHeight = 1.25f;
	private final float targetTime = 3f;
	
	private final float jumpVelocity = ((2.0f * targetHeight) * targetTime);
	
	// If the player is sneaking
	public boolean isSneaking = false;
	
	// If the player is sprinting
	public boolean isSprinting = false;
	
	// If the player is flying
	public boolean isFlying = false;
	
	// The block to place down
	private Block placeBlock = Blocks.GRASS;
	
	// Last hit result
	public RaycastResult lastHit = RaycastResult.NO_RESULT;
	
	// Related to placing & breaking
	private float breakTimer = 0.0f;
	private float placeTimer = 0.0f;
	private boolean isBreaking = false;
	private boolean isPlacing = false;
	
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
			
			// Stop sprinting
			this.isSprinting = false;
			return;
		}
		
		double phi = Math.toRadians(yaw);
		double xDir =  xAccel * Math.cos(phi) + zAccel * Math.sin(phi);
		double zDir = -xAccel * Math.sin(phi) + zAccel * Math.cos(phi);
		
		// Normalize the horizontal movement vector
		double mag = Math.sqrt(Math.pow(xDir, 2) + Math.pow(zDir, 2));
		
		xDir /= mag;
		zDir /= mag;
		
		this.xAccel = (float) (xDir * speed);
		this.zAccel = (float) (zDir * speed);
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
		updateBreakAndPlace(delta);
		
		// Update speed coef
		if (isSprinting)
			speedCoef = 1.75f;
		if (isSneaking)
			speedCoef = 0.25f;
		if (!isSneaking && !isSprinting)
			speedCoef = 1f;
		
		// Do movement update
		if (world.getBlock((int)Math.floor(xPos), (int)Math.floor(yPos), (int)Math.floor(zPos)) == Blocks.WATER)
			accelCoef = 0.375f;
		else
			accelCoef = 1f;
		
		// Change the yAccel if the player is flying or not
		if (isFlying)
			yAccel = 0f;
		else
			yAccel = -gravity;
		
		// Apply the acceleration
		xVel += accelCoef * speedCoef * rampUpCoeff * xAccel * delta;
		zVel += accelCoef * speedCoef * rampUpCoeff * zAccel * delta;
		
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
		updateVerticalCollision(delta);
		
		// Stop sprinting if a wall was hit
		if (updateHorizontalCollision(delta))
			isSprinting = false;
		
		// Apply the velocity
		xPos += xVel * delta;
		yPos += yVel * delta;
		zPos += zVel * delta;
		
		// Apply exponential decay to the velocity
		if(Math.abs(xVel) > 1/256f)
			xVel = MathUtil.lerp(xVel, 0, slipperiness * accelCoef * delta);
		else
			xVel = 0;
		
		if(Math.abs(zVel) > 1/256f)
			zVel = MathUtil.lerp(zVel, 0, slipperiness * accelCoef * delta);
		else
			zVel = 0;
		
		if (isFlying)
			yVel = decay(yVel, 0.5f);
	}
	
	private void updateBreakAndPlace(float delta)
	{
		if (breakTimer > 0)
		{
			breakTimer -= delta;
		}
		else
		{
			breakTimer = 0;
			
			if (isBreaking && (lastHit = raycast()) != RaycastResult.NO_RESULT)
			{
				if (world.isClient)
				{
					// TODO: Replace with NetworkManager packet sending
					Game.getInstance().clientChannel.write(new PCSBreakBlock(Game.getInstance().clientID, lastHit));
				}
				
				// Break the block, with the appropriate block callbacks being called
				Block block = world.getBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ);
				block.onBlockBroken(world, lastHit.blockX, lastHit.blockY, lastHit.blockZ);
				world.setBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ, Blocks.AIR);
				
				// 0.25s between block breaks
				breakTimer = 0.25f;
			}
		}
		
		if (placeTimer > 0)
		{
			placeTimer -= delta;
		}
		else
		{
			placeTimer = 0;
			
			if (isPlacing && (lastHit = raycast()) != RaycastResult.NO_RESULT)
			{
				isPlacing = true;
				
				Block block = placeBlock;
				
				if (world.isClient)
				{
					// TODO: Broadcast place event on appropriate event bus
					// TODO: Replace with NetworkManager packet sending
					Game.getInstance().clientChannel.write(new PCSPlaceBlock(Game.getInstance().clientID, lastHit, placeBlock));
				}
				
				// If the block can't be placed, don't place it
				if(!block.canPlaceBlock(
						world,
						lastHit.blockX + lastHit.face.getOffsetX(),
						lastHit.blockY + lastHit.face.getOffsetY(),
						lastHit.blockZ + lastHit.face.getOffsetZ()))
					return;
				
				world.setBlock(
						lastHit.blockX + lastHit.face.getOffsetX(),
						lastHit.blockY + lastHit.face.getOffsetY(),
						lastHit.blockZ + lastHit.face.getOffsetZ(),
						block);
				block.onBlockPlaced(
						world,
						lastHit.blockX + lastHit.face.getOffsetX(),
						lastHit.blockY + lastHit.face.getOffsetY(),
						lastHit.blockZ + lastHit.face.getOffsetZ());
				
				// 0.25s between block breaks
				placeTimer = 0.25f;
			}
		}
		
		// Update the hit selection
		lastHit = raycast();
	}
	
	// Finds the next hit
	private RaycastResult raycast()
	{
		// Calculate the pointing vector
		Vector3d pointing = new Vector3d(0.0f, 0.0f, -1.0f);
		pointing.rotateAxis(Math.toRadians(pitch), 1f, 0f, 0f);
		pointing.rotateAxis(Math.toRadians(yaw),   0f, 1f, 0f);
		
		// Range of 7 blocks
		final int range = 7;
		
		// Inputs
		return world.blockRaycast(pointing, xPos, yPos + eyeHeight, zPos, range);
	}
	
	public void setBreaking(boolean isBreaking)
	{
		this.isBreaking = isBreaking;
		
		// Reset the break timer, if needed
		if(!isBreaking)
			breakTimer = 0;
	}
	
	public void setPlacing(boolean isPlacing)
	{
		this.isPlacing = isPlacing;
		
		// Reset the break timer, if needed
		if(!isPlacing)
			placeTimer = 0;
	}
	
	public void changeSelectedBlock(int selection)
	{
		// TODO: Add HUD
		
		// Select block to place
		final Block[] placeBlocks = new Block[] {
				Blocks.GRASS, Blocks.DIRT, Blocks.STONE,
				Blocks.PLANKS, Blocks.STONE_BRICKS, Blocks.CLAY_BRICKS,
				Blocks.DOOR_LOWER, Blocks.GLASS, Blocks.SAND, Blocks.UPDATING_WATER,
				Blocks.UPDATING_LAVA,
				// Extended
				Blocks.TORCH, Blocks.GRAVEL
		};
		
		if (selection < 0 || selection > placeBlocks.length)
			return;
		
		placeBlock = placeBlocks[selection];
	}
	
}
