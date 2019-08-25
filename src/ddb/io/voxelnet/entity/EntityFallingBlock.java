package ddb.io.voxelnet.entity;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.AABBCollider;

public class EntityFallingBlock extends Entity
{
	// The block that this entity represents
	public Block falling;
	
	public EntityFallingBlock(Block block)
	{
		this.falling = block;
		// Shrink to prevent clipping onto an edge
		this.collisionBox = new AABBCollider(block.getCollisionBox()).grow(-0.25f/16f, -0.25f/16f, -0.25f/16f);
	}
	
	@Override
	public void update(float delta)
	{
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
		// Apply the response in the event of a collision
		int zResponse, xResponse, yResponse;
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
		
		yResponse = testForCollisionY(delta);
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
		}
		
		if (onGround)
		{
			// Final position found, place the block
			int blockX = Math.round(xPos - 0.5f);
			int blockY = Math.round(yPos);
			int blockZ = Math.round(zPos - 0.5f);
			
			int yOff = 0;
			if (world.getBlock(blockX, blockY, blockZ) != Blocks.AIR.getId())
			{
				// Search for the next air block position if the current one is
				// occupied
				for (; yOff + blockY < 256; yOff++)
				{
					if (world.getBlock(blockX, blockY + yOff, blockZ) == Blocks.AIR.getId())
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
			
			world.setBlock(blockX, blockY + yOff, blockZ, falling.getId());
			
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
