package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.AABBCollider;

public class EntityFallingBlock extends Entity
{
	// TODO: Preserve meta
	// The block that this entity represents
	public final Block falling;
	
	private int lifetime = 0;
	
	public EntityFallingBlock(Block block)
	{
		this.falling = block;
		// Shrink to prevent clipping onto an edge
		this.collisionBox = new AABBCollider(block.getCollisionBox()).grow(-0.25f/16f, -0.25f/16f, -0.25f/16f);
	}
	
	@Override
	public void update(float delta)
	{
		int blockX = Math.round(xPos - 0.5f);
		int blockY = Math.round(yPos);
		int blockZ = Math.round(zPos - 0.5f);
		
		if (lifetime == 0)
		{
			// Block removal was deferred to here
			
			// Check if the current block is the same as the falling block
			if (world.getBlock(blockX, blockY, blockZ) != falling)
			{
				// Not the same, kill it
				setDead();
				return;
			}
			else
			{
				// Current block is the same as the falling one, remove it
				world.setBlock(blockX, blockY, blockZ, Blocks.AIR);
			}
		}
		lifetime++;
		
		if (yPos < -64)
		{
			// Kill all falling blocks that are below y-level -64
			setDead();
			return;
		}
		
		// Apply the acceleration
		xVel += xAccel * delta;
		yVel += yAccel * delta;
		zVel += zAccel * delta;
		
		yVel = clamp(yVel, -gravity * 2, yVel);
		
		// Update the collision status
		updateCollision(delta);
		
		if (onGround)
		{
			// Final position found, place the block
			int yOff = 0;
			Block block = world.getBlock(blockX, blockY, blockZ);
			
			if (block != Blocks.AIR && !block.canBeReplacedBy(world, falling, (byte) 0, blockX, blockY, blockZ))
			{
				// Search for the next air block position if the current one is
				// occupied
				for (; yOff + blockY < 256; yOff++)
				{
					if (world.getBlock(blockX, blockY + yOff, blockZ) == Blocks.AIR)
						break;
				}
				
				if (yOff + blockX >= 256)
				{
					// Shouldn't be here, but still die
					System.out.println("SHOULDN'T BE HERE!");
					setDead();
					return;
				}
			}
			
			world.setBlock(blockX, blockY + yOff, blockZ, falling);
			
			// No longer needed
			setDead();
			return;
		}
		
		// Apply the velocity
		xPos += xVel * delta;
		yPos += yVel * delta;
		zPos += zVel * delta;
	}
	
}
