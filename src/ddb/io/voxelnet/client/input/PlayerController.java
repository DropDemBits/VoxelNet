package ddb.io.voxelnet.client.input;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.Facing;
import org.joml.Vector3f;

import static ddb.io.voxelnet.Game.showThings;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Class that controls a player entity
 */
public class PlayerController
{
	private static final float MOUSE_SENSITIVITY = 0.5f;
	
	long window = 0;
	EntityPlayer player;
	
	double lastX = 0.0f, lastY = 0.0f;
	
	float hitX = 0.0f;
	float hitY = 0.0f;
	float hitZ = 0.0f;
	public int blockX = 0;
	public int blockY = 0;
	public int blockZ = 0;
	Facing hitFace = Facing.NORTH;
	byte placeID = 1;
	public boolean showHit = false;
	
	float breakTimer = 0.0f;
	boolean isBreaking = false;
	float placeTimer = 0.0f;
	boolean isPlacing = false;
	
	private final float boxRad = 1.5f / 16f;
	private AABBCollider rayBox = new AABBCollider(0, 0, 0, boxRad, boxRad, boxRad);
	
	public PlayerController(long win, EntityPlayer player)
	{
		this.window = win;
		this.player = player;
		
		glfwSetCursorPosCallback(window, (window, x, y) -> {
			double deltaX = x - lastX;
			double deltaY = y - lastY;
			lastX = x;
			lastY = y;
			
			player.rotate(
					(float) -deltaY * MOUSE_SENSITIVITY,
					(float) -deltaX * MOUSE_SENSITIVITY
			);
			
			showHit = raycast();
		});
		
		glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
			if (action == GLFW_PRESS)
			{
				// Ignore all other mouse buttons
				if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT)
					return;
				
				if (button == GLFW_MOUSE_BUTTON_RIGHT)
					isPlacing = true;
				else if (button == GLFW_MOUSE_BUTTON_LEFT)
					isBreaking = true;
			}
			else if (action == GLFW_RELEASE)
			{
				if (button == GLFW_MOUSE_BUTTON_RIGHT)
				{
					// Reset status & timer
					isPlacing = false;
					placeTimer = 0;
				}
				else if (button == GLFW_MOUSE_BUTTON_LEFT)
				{
					// Reset status & timer
					isBreaking = false;
					breakTimer = 0;
				}
			}
		});
		
		glfwSetKeyCallback(window, (window, keycode, scancode, action, mods) -> {
			if (action != GLFW_PRESS)
				return;
			
			// Select block to place
			final Block[] placeBlocks = new Block[] {
					Blocks.GRASS, Blocks.DIRT, Blocks.STONE,
					Blocks.PLANKS, Blocks.STONE_BRICKS, Blocks.CLAY_BRICKS,
					Blocks.DOOR_LOWER, Blocks.GLASS,
			};
			
			if (keycode >= GLFW_KEY_1 && keycode <= (GLFW_KEY_0 + placeBlocks.length))
				placeID = placeBlocks[keycode - GLFW_KEY_1].getId();
			
			// Toggle flying
			if (keycode == GLFW_KEY_F)
				player.isFlying = !player.isFlying;
			
			if (keycode == GLFW_KEY_F3)
				showThings = !showThings;
			
			if (keycode == GLFW_KEY_B)
			{
				// BOOM!
				player.world.explode((int)player.xPos, (int)player.yPos, (int)player.zPos, 20);
			}
		});
	}
	
	public void update(float delta)
	{
		if (breakTimer > 0)
		{
			breakTimer -= delta;
		}
		else
		{
			breakTimer = 0;
			
			if (isBreaking && raycast())
			{
				Block block = Block.idToBlock(player.world.getBlock(blockX, blockY, blockZ));
				block.onBlockBroken(player.world, blockX, blockY, blockZ);
				player.world.setBlock(blockX, blockY, blockZ, (byte) 0);
				
				// Update the hit selection
				showHit = raycast();
				
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
			
			if (isPlacing && raycast())
			{
				isPlacing = true;
				
				Block block = Block.idToBlock(placeID);
				
				// If the block can't be placed, don't place it
				if(!block.canPlaceBlock(
						player.world,
						blockX + hitFace.getOffsetX(),
						blockY + hitFace.getOffsetY(),
						blockZ + hitFace.getOffsetZ()))
					return;
				
				player.world.setBlock(
						blockX + hitFace.getOffsetX(),
						blockY + hitFace.getOffsetY(),
						blockZ + hitFace.getOffsetZ(),
						placeID);
				block.onBlockPlaced(
						player.world,
						blockX + hitFace.getOffsetX(),
						blockY + hitFace.getOffsetY(),
						blockZ + hitFace.getOffsetZ());
				
				// Update the hit selection
				showHit = raycast();
				
				// 0.25s between block breaks
				placeTimer = 0.25f;
			}
		}
		
		
		float xDir = 0.0f, yDir = 0.0f, zDir = 0.0f;
		
		player.speedCoef = 1f;
		if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
			player.speedCoef = 0.25f;
		if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS)
			player.speedCoef = 1.75f;
		
		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			zDir += -1.0f;
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			zDir +=  1.0f;
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			xDir += -1.0f;
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			xDir +=  1.0f;
		
		if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
			player.jump();
		
		if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
			glfwSetWindowShouldClose(window, true);
		
		player.isSneaking = (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS);
		
		player.move(xDir, zDir);
		
		if (xDir != 0.0f || yDir != 0.0f || zDir != 0.0f)
			showHit = raycast();
	}
	
	private boolean raycast()
	{
		// Calculate the pointing vector
		Vector3f point = new Vector3f(0.0f, 0.0f, -1.0f);
		point.rotateAxis((float) Math.toRadians(player.pitch), 1f, 0f, 0f);
		point.rotateAxis((float) Math.toRadians(player.yaw),   0f, 1f, 0f);
		
		point.mul(0.125f);
		
		float rayX = player.xPos;
		float rayY = player.yPos + player.eyeHeight;
		float rayZ = player.zPos;
		
		int x, y, z;
		
		AABBCollider rayBB = new AABBCollider(rayBox);
		
		// Step for 5 blocks
		for(int i = 0; i < 7 * 8; i++)
		{
			rayBB.setPosition(rayX - (rayBox.width / 2f), rayY - (rayBox.height / 2f), rayZ - (rayBox.depth / 2f));
			
			// Go through the 8 corners
			for (int c = 7; c >= 0; --c)
			{
				float offX = rayBB.width  * ((c >> 0) & 1);
				float offY = rayBB.height * ((c >> 2) & 1);
				float offZ = rayBB.depth  * ((c >> 1) & 1);
				
				x = Math.round((rayBB.x + offX) - 0.5f);
				y = Math.round((rayBB.y + offY) - 0.5f);
				z = Math.round((rayBB.z + offZ) - 0.5f);
				
				
				byte id = player.world.getBlock(x, y, z);
				
				if (id != 0)
				{
					rayX -= point.x;
					rayY -= point.y;
					rayZ -= point.z;
					
					hitX = rayX;
					hitY = rayY;
					hitZ = rayZ;
					
					// Block found, get the specific face
					Vector3f hit = new Vector3f(rayX - x - 0.5f, rayY - y - 0.5f, rayZ - z - 0.5f);
					final Vector3f xAxis = new Vector3f(0.5f, 0.0f, 0.0f);
					final Vector3f yAxis = new Vector3f(0.0f, 0.5f, 0.0f);
					final Vector3f zAxis = new Vector3f(0.0f, 0.0f, 0.5f);
					
					float dotX = hit.dot(xAxis);
					float dotY = hit.dot(yAxis);
					float dotZ = hit.dot(zAxis);
					
					if (Math.abs(dotZ) > Math.abs(dotY) && Math.abs(dotZ) > Math.abs(dotX))
					{
						if (dotZ > 0) hitFace = Facing.SOUTH;
						else hitFace = Facing.NORTH;
					} else if (Math.abs(dotX) > Math.abs(dotY) && Math.abs(dotX) > Math.abs(dotZ))
					{
						if (dotX > 0) hitFace = Facing.EAST;
						else hitFace = Facing.WEST;
					} else if (Math.abs(dotY) >= Math.abs(dotX) && Math.abs(dotY) >= Math.abs(dotZ))
					{
						if (dotY > 0) hitFace = Facing.UP;
						else hitFace = Facing.DOWN;
					}
					
					blockX = x;
					blockY = y;
					blockZ = z;
					return true;
				}
			}
			
			rayX += point.x;
			rayY += point.y;
			rayZ += point.z;
		}
		
		blockX = -1;
		blockY = -1;
		blockZ = -1;
		return false;
	}
	
}
