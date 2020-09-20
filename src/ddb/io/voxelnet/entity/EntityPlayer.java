package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.client.ClientNetworkManager;
import ddb.io.voxelnet.network.packet.PCSBreakBlock;
import ddb.io.voxelnet.network.packet.PCSPlaceBlock;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.RaycastResult;
import org.joml.Vector3d;

public class EntityPlayer extends Entity
{
	
	// Horizontal speed (blocks / second)
	public final float speed = 3.5f;
	// Time for the speed to accelerate to the base movement velocity (in seconds)
	public final float accelTime = 0.125f;
	// Base acceleration, based on the normal movement speed
	public final float baseAccel = speed / accelTime;
	
	// Modifiers
	
	// Modifier for the max top speed, relative to the base walking speed
	public float topSpeedModifier = 1f;
	// Modifier for the acceleration to the top speed, relative to the base acceleration
	public float accelModifier = 1f;
	// How much friction a surface has, and is only applied when not moving (in percentage to reduce / second)
	public final float friction = 10.5f;
	
	// Jump related
	// Maximum jump height, in blocks
	private final float targetHeight = 1.25f;
	// Time to reach peak, in seconds
	private final float targetTime = 0.35f;
	
	private final float jumpVelocity = ((2 * targetHeight) / targetTime);
	
	// The gravity of the entity
	// Derived from the player's jump height
	// g = 2 * h / t^2
	public final float gravity = ((2 * targetHeight) / (targetTime * targetTime));
	
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
	 * Moves the entity to the specified direction
	 * The acceleration is relative to the current yaw orientation
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
		
		// Normalize the horizontal movement vector if it larger than a unit vector
		double mag = Math.sqrt(xDir * xDir + zDir * zDir);
		mag = Math.max(mag, 1.0d);
		
		xDir /= mag;
		zDir /= mag;
		
		this.xAccel = (float) (xDir * baseAccel);
		this.zAccel = (float) (zDir * baseAccel);
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
		
		if (xAccel == 0 && zAccel == 0) {
			// Stop sprinting
			this.isSprinting = false;
		}
		
		// Update speed coeff
		if (isSprinting)
			topSpeedModifier = 1.75f;
		if (isSneaking)
			topSpeedModifier = 0.33f;
		if (!isSneaking && !isSprinting)
			topSpeedModifier = 1f;
		
		// Update acceleration coeff
		if (world.getBlock((int)Math.floor(xPos), (int)Math.floor(yPos), (int)Math.floor(zPos)) == Blocks.WATER)
			accelModifier = 0.375f;
		else
			accelModifier = 1f;
		
		float max_speed = speed * topSpeedModifier;
		
		// Change the yAccel if the player is flying or not
		if (isFlying)
			yAccel = 0f;
		else
			yAccel = -gravity;
		
		// Do physics update
		
		// Apply the acceleration
		xVel += accelModifier * xAccel * delta;
		zVel += accelModifier * zAccel * delta;
		
		// If moving up, don't affect the acceleration
		if (isFlying)
			yVel += 1f * yAccel * delta;
		else
			yVel += accelModifier * yAccel * delta;
		
		double real_speed = Math.sqrt(xVel * xVel + zVel * zVel);
		if (real_speed > max_speed) {
			// Cap the horizontal velocities to the max effective speed
			xVel = (float) ((xVel / real_speed) * max_speed);
			zVel = (float) ((zVel / real_speed) * max_speed);
		}
		
		// Clamp the vertical velocity to jump velocity if flying
		if (isFlying)
			yVel = clamp(yVel, -jumpVelocity, jumpVelocity);
		else
			yVel = clamp(yVel, -gravity * 2, yVel);
		
		if (isFlying && isSneaking)
			yVel = -jumpVelocity;
		
		// Update the collision status
		// Update horizontal before vertical to prevent sticking on a corner
		double lastVelX = xVel, lastVelZ = zVel;
		updateHorizontalCollision(delta);
		updateVerticalCollision(delta);
		boolean hitWall = (xVel != lastVelX) || (zVel != lastVelZ);
				
		// Apply the velocity
		xPos += xVel * delta;
		yPos += yVel * delta;
		zPos += zVel * delta;
		
		// Apply exponential decay if not moving to the velocity
		if (Math.abs(xAccel) < 1 / 1024f)
		{
			if (Math.abs(xVel) > 1 / 1024f)
				xVel *= (1 - friction * delta);
			else
				xVel = 0;
		}
		
		if (Math.abs(zAccel) < 1 / 1024f)
		{
			if (Math.abs(zVel) > 1 / 1024f)
				zVel *= (1 - friction * delta);
			else
				zVel = 0;
		}
		
		// Stop sprinting if a wall was hit
		if (hitWall)
			isSprinting = false;
		
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
					// TODO: Broadcast place event on appropriate event bus
					// TODO: Remove necessity for sending over the client id
					ClientNetworkManager networkManager = Game.getInstance().getNetworkManager();
					networkManager.sendPacket(new PCSBreakBlock(networkManager.getClientID(), lastHit));
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
					// TODO: Remove necessity for sending over the client id
					ClientNetworkManager networkManager = Game.getInstance().getNetworkManager();
					networkManager.sendPacket(new PCSPlaceBlock(networkManager.getClientID(), lastHit, placeBlock));
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
