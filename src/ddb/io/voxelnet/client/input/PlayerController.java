package ddb.io.voxelnet.client.input;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.event.Event;
import ddb.io.voxelnet.event.input.KeyEvent;
import ddb.io.voxelnet.event.input.MouseEvent;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static ddb.io.voxelnet.Game.showThings;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Class that controls a player entity
 */
public class PlayerController
{
	private static final float MOUSE_SENSITIVITY = 0.45f;
	
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
	
	private final float boxRad = 1.375f / 16f;
	private AABBCollider rayBox = new AABBCollider(0, 0, 0, boxRad, boxRad, boxRad);
	
	public PlayerController(long win, EntityPlayer player)
	{
		// TODO: Move these things into a separate window class
		this.window = win;
		glfwSetCursorPosCallback(window, (window, x, y) -> {
			double dx = x - lastX;
			double dy = y - lastY;
			lastX = x;
			lastY = y;
			
			Game.GLOBAL_BUS.postEvent(new MouseEvent.Move(x, y, dx, dy, false));
		});
		
		glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
			Game.GLOBAL_BUS.postEvent(new MouseEvent.Button(button, action, mods, System.currentTimeMillis()));
		});
		
		glfwSetKeyCallback(window, (window, keycode, scancode, action, mods) -> {
			Game.GLOBAL_BUS.postEvent(new KeyEvent.Button(keycode, scancode, action, mods));
		});
		
		this.player = player;
		// Register the input handlers
		Game.GLOBAL_BUS.addHandler(MouseEvent.Button.class, this::onMouseButton);
		Game.GLOBAL_BUS.addHandler(MouseEvent.Move.class, this::onMouseMove);
		Game.GLOBAL_BUS.addHandler(KeyEvent.Button.class, this::onKeyButton);
	}
	
	//// Event Handlers \\\\
	private void onMouseButton(Event evt)
	{
		MouseEvent.Button e = (MouseEvent.Button)evt;
		
		if (e.state == MouseEvent.Button.PRESSED)
		{
			// Ignore all other mouse buttons
			if (e.button != GLFW_MOUSE_BUTTON_LEFT && e.button != GLFW_MOUSE_BUTTON_RIGHT)
				return;
			
			if (e.button == GLFW_MOUSE_BUTTON_RIGHT)
				isPlacing = true;
			else if (e.button == GLFW_MOUSE_BUTTON_LEFT)
				isBreaking = true;
		}
		else if (e.state == MouseEvent.Button.RELEASED)
		{
			if (e.button == GLFW_MOUSE_BUTTON_RIGHT)
			{
				// Reset status & timer
				isPlacing = false;
				placeTimer = 0;
			}
			else if (e.button == GLFW_MOUSE_BUTTON_LEFT)
			{
				// Reset status & timer
				isBreaking = false;
				breakTimer = 0;
			}
		}
	}
	
	private void onMouseMove(Event evt)
	{
		MouseEvent.Move e = (MouseEvent.Move)evt;
		
		player.rotate(
				(float) -e.dy * MOUSE_SENSITIVITY,
				(float) -e.dx * MOUSE_SENSITIVITY
		);
	}
	
	private void onKeyButton(Event evt)
	{
		KeyEvent.Button e = (KeyEvent.Button) evt;
		
		if (e.state != KeyEvent.Button.PRESSED)
			return;
		
		// Select block to place
		final Block[] placeBlocks = new Block[] {
				Blocks.GRASS, Blocks.DIRT, Blocks.STONE,
				Blocks.PLANKS, Blocks.STONE_BRICKS, Blocks.CLAY_BRICKS,
				Blocks.DOOR_LOWER, Blocks.GLASS, Blocks.SAND, Blocks.GRAVEL
		};
		
		if (e.keycode >= GLFW_KEY_1 && e.keycode <= (GLFW_KEY_0 + placeBlocks.length))
			placeID = placeBlocks[e.keycode - GLFW_KEY_1].getId();
		else if(e.keycode == GLFW_KEY_0)
			placeID = placeBlocks[9].getId();
		
		// Toggle flying
		if (e.keycode == GLFW_KEY_F)
			player.isFlying = !player.isFlying;
		
		if (e.keycode == GLFW_KEY_F3)
			showThings = !showThings;
		
		if (e.keycode == GLFW_KEY_B)
		{
			// BOOM!
			player.world.explode((int)player.xPos, (int)player.yPos, (int)player.zPos, 20);
		}
	}
	
	//// Main interface \\\\
	public void update(float delta)
	{
		// TODO: Move some of these things into EntityPlayer
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
		
		float xDir = 0.0f, zDir = 0.0f;
		
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
		
		showHit = raycast();
	}
	
	private boolean raycast()
	{
		// TODO: Only get the block closest to the player
		
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
		List<Vector3f> potentialHits = new ArrayList<>();
		List<Vec3i> seen = new ArrayList<>();
		
		// Step for 5 blocks
		for(int i = 0; i < 7 * 8; i++)
		{
			// Move the ray box to the current position
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
				
				// Fetch the block at the current corner
				byte id = player.world.getBlock(x, y, z);
				
				// TODO:
				// Fetch collision box
				// Grow collision box to add some bias
				// Test for relative collision
				
				if (id != Blocks.AIR.getId())
				{
					Block block = Block.idToBlock(id);
					Vec3i pos = new Vec3i(x, y, z);
					if(seen.contains(pos))
						continue;
					seen.add(pos);
					
					// Don't select blocks without a hitbox
					if (block.getHitBox() == null)
						continue;
					
					// Test the hit box for collision with the ray
					AABBCollider box = new AABBCollider(block.getHitBox());
					box.setPosition(x, y, z);
					box.add(-0.0625f, -0.0625f, -0.0625f);
					box.grow(0.125f, 0.125f, 0.125f);
					
					if (box.intersectsWith(rayBB))
					{
						// Add the hit to the potential list
						Vector3f hit = new Vector3f((rayBB.x + offX) - 0.5f, (rayBB.y + offY) - 0.5f, (rayBB.z + offZ) - 0.5f);
						potentialHits.add(hit);
					}
				}
			}
			
			if (potentialHits.size() > 0)
			{
				//System.out.println("Hits: (" + potentialHits.size() + ")");
				
				if (potentialHits.size() > 1)
				{
					// Sort the hit list by distance to the player
					// Have player be in the middle of the block for consistency
					Vector3f origin = new Vector3f(Math.round(player.xPos - 0.5), player.yPos + player.eyeHeight, Math.round(player.zPos - 0.5));
					potentialHits.sort((vA, vB) -> Float.compare(taxicabDistance(vA, origin), taxicabDistance(vB, origin)));
					//System.out.println("FINDING! (" + potentialHits.size() + ")");
				}
				
				// Select the first hit
				Vector3f hit = potentialHits.get(0);
				
				// Move the hit back by 1 position
				rayX -= point.x;
				rayY -= point.y;
				rayZ -= point.z;
				
				// Get the block hit
				blockX = Math.round(hit.x);
				blockY = Math.round(hit.y);
				blockZ = Math.round(hit.z);
				
				// Move the hit point relative to the block position
				// ray - hit - (0.5f, 0.5f, 0.5f) -> -hit + ray - (0.5f, 0.5f, 0.5f)
				hit.set(rayX, rayY, rayZ).sub(blockX, blockY, blockZ).sub(0.5f, 0.5f, 0.5f);
				hitX = hit.x;
				hitY = hit.y;
				hitZ = hit.z;
				
				// Get the face hit
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
				return true;
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
	
	/**
	 * Finds the taxicab distance between two points
	 * || a - b || = d
	 *
	 * @param a Point a
	 * @param b Point b
	 * @return The taxicab distance between the points
	 */
	private float taxicabDistance(Vector3f a, Vector3f b)
	{
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
	}
	
}
