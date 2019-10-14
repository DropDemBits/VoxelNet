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
import ddb.io.voxelnet.world.World;
import org.joml.Vector3d;

import static ddb.io.voxelnet.Game.showThings;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Class that controls a player entity
 */
public class PlayerController
{
	private static final float MOUSE_SENSITIVITY = 0.45f;
	
	long window;
	final EntityPlayer player;
	
	double lastX = 0.0f, lastY = 0.0f;
	
	float hitX = 0.0f;
	float hitY = 0.0f;
	float hitZ = 0.0f;
	public int blockX = 0;
	public int blockY = 0;
	public int blockZ = 0;
	public Facing hitFace = Facing.NORTH;
	Block placeBlock = Blocks.GRASS;
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
		
		glfwSetMouseButtonCallback(window, (window, button, action, mods) ->
				Game.GLOBAL_BUS.postEvent(new MouseEvent.Button(button, action, mods, System.currentTimeMillis()))
		);
		
		glfwSetKeyCallback(window, (window, keycode, scancode, action, mods) ->
			Game.GLOBAL_BUS.postEvent(new KeyEvent.Button(keycode, scancode, action, mods))
		);
		
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
				Blocks.DOOR_LOWER, Blocks.GLASS, Blocks.SAND, Blocks.UPDATING_WATER
		};
		
		if (e.keycode >= GLFW_KEY_1 && e.keycode <= (GLFW_KEY_0 + placeBlocks.length))
			placeBlock = placeBlocks[e.keycode - GLFW_KEY_1];
		else if(e.keycode == GLFW_KEY_0)
			placeBlock = placeBlocks[9];
		
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
				Block block = player.world.getBlock(blockX, blockY, blockZ);
				block.onBlockBroken(player.world, blockX, blockY, blockZ);
				player.world.setBlock(blockX, blockY, blockZ, Blocks.AIR);
				
				// Update the hit selection
				//showHit = raycast();
				
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
				
				Block block = placeBlock;
				
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
						placeBlock);
				block.onBlockPlaced(
						player.world,
						blockX + hitFace.getOffsetX(),
						blockY + hitFace.getOffsetY(),
						blockZ + hitFace.getOffsetZ());
				
				// Update the hit selection
				//showHit = raycast();
				
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
		// Calculate the pointing vector
		Vector3d point = new Vector3d(0.0f, 0.0f, -1.0f);
		point.rotateAxis(Math.toRadians(player.pitch), 1f, 0f, 0f);
		point.rotateAxis(Math.toRadians(player.yaw),   0f, 1f, 0f);
		
		World world = player.world;
		// Range of 7 blocks
		final int range = 7;
		
		// From https://gamedev.stackexchange.com/questions/47362/cast-ray-to-select-block-in-voxel-game/49423#49423
		
		double dx = point.x, dy = point.y, dz = point.z;
		double startX = player.xPos, startY = player.yPos + player.eyeHeight, startZ = player.zPos;
		int blockX = (int)Math.floor(startX), blockY = (int)Math.floor(startY), blockZ = (int)Math.floor(startZ);
		
		// Orthogonal step
		int stepX = (int)Math.signum(dx), stepY = (int)Math.signum(dy), stepZ = (int)Math.signum(dz);
		
		// Calculate the initial max t's
		// t = p_inv(p) = (p(t) - p_0) / dp
		double tMaxX = intBound(startX, dx);
		double tMaxY = intBound(startY, dy);
		double tMaxZ = intBound(startZ, dz);
		
		// Calculate the delta t's
		// dt = (1 - 0) / dp
		double tDeltaX = stepX / dx;
		double tDeltaY = stepY / dy;
		double tDeltaZ = stepZ / dz;
		
		// Scale range to allow direct comparison between 't' values
		double radius = range / Math.sqrt(dx*dx + dy*dy + dz*dz);
		
		Facing hitFace = Facing.UP;
		
		double rayOffX = 0;
		double rayOffY = 0;
		double rayOffZ = 0;
		
		// Step while in range
		while (true)
		{
			Block block = world.getBlock(blockX, blockY, blockZ);
			
			if (block != Blocks.AIR && block.getHitBox() != null)
			{
				// Perform fine stepping to detect if the hit really landed
				// Ray position (for intersection testing)
				double rayX = rayOffX + startX;
				double rayY = rayOffY + startY;
				double rayZ = rayOffZ + startZ;
				// Accumulated distance
				double dist = 0;
				final double MAX_DIST = 1.5d;
				
				Vector3d ray = new Vector3d(rayX, rayY, rayZ);
				Vector3d step = new Vector3d(point);
				// Step in 1/32nds (half a unit)
				step.mul(1d/32d);
				
				AABBCollider blockBox = new AABBCollider(block.getHitBox());
				blockBox.setPosition(blockX, blockY, blockZ);
				
				double deltaDist = step.length();
				
				// Step until the hitbox of the block is intersected
				while(dist < MAX_DIST)
				{
					if (blockBox.intersectsWith((float)ray.x, (float)ray.y, (float)ray.z))
					{
						// Found block, stop
						this.blockX = blockX;
						this.blockY = blockY;
						this.blockZ = blockZ;
						this.hitFace = hitFace;
						
						return true;
					}
					
					ray.add(step);
					dist += deltaDist;
				}
				
				// Block not found, continue
			}
			
			// Perform orthogonal step
			boolean doXStep = false, doYStep = false, doZStep = false;
			
			if (tMaxX < tMaxY)
			{
				if (tMaxX < tMaxZ)
					doXStep = true;
				else
					doZStep = true;
			}
			else
			{
				if (tMaxY < tMaxZ)
					doYStep = true;
				else
					doZStep = true;
			}
			
			if (doXStep)
			{
				if (tMaxX > radius)
					break;
				
				rayOffX = tMaxX * dx;
				rayOffY = tMaxX * dy;
				rayOffZ = tMaxX * dz;
				
				tMaxX += tDeltaX;
				blockX += stepX;
				
				// Keep track of face
				hitFace = Facing.WEST;
				if (stepX < 0)
					hitFace = Facing.EAST;
			}
			else if (doYStep)
			{
				if (tMaxY > radius)
					break;
				
				rayOffX = tMaxY * dx;
				rayOffY = tMaxY * dy;
				rayOffZ = tMaxY * dz;
				
				tMaxY += tDeltaY;
				blockY += stepY;
				
				// Keep track of face
				hitFace = Facing.DOWN;
				if (stepY < 0)
					hitFace = Facing.UP;
			}
			else if (doZStep)
			{
				if (tMaxZ > radius)
					break;
				
				rayOffX = tMaxZ * dx;
				rayOffY = tMaxZ * dy;
				rayOffZ = tMaxZ * dz;
				
				tMaxZ += tDeltaZ;
				blockZ += stepZ;
				
				// Keep track of face
				hitFace = Facing.NORTH;
				if (stepZ < 0)
					hitFace = Facing.SOUTH;
			}
		}
		
		this.blockX = -1;
		this.blockY = -1;
		this.blockZ = -1;
		return false;
	}
	
	// Finds integer boundary
	private double intBound(double p, double dp)
	{
		return (dp > 0? Math.ceil(p)-p: p-Math.floor(p)) / Math.abs(dp);
	}
	
}
