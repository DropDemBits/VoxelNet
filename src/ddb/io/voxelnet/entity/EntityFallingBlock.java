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
		updateCollision(delta);
		
		if (onGround)
		{
			// Final position found, place the block
			int blockX = Math.round(xPos - 0.5f);
			int blockY = Math.round(yPos);
			int blockZ = Math.round(zPos - 0.5f);
			
			int yOff = 0;
			Block block = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
			
			if (block != Blocks.AIR && !block.canBeReplacedBy(world, falling, blockX, blockY, blockZ))
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
