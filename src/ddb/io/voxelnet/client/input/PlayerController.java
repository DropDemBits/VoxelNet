package ddb.io.voxelnet.client.input;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.client.GameWindow;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.event.Event;
import ddb.io.voxelnet.event.input.KeyEvent;
import ddb.io.voxelnet.event.input.MouseEvent;

import static ddb.io.voxelnet.Game.showThings;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Class that controls a player entity
 */
public class PlayerController
{
	private static final float MOUSE_SENSITIVITY = 0.45f;
	private final EntityPlayer player;
	private final GameWindow window;
	
	public PlayerController(GameWindow window, EntityPlayer player)
	{
		this.window = window;
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
			if (e.button == GLFW_MOUSE_BUTTON_RIGHT)
				player.setPlacing(true);
			else if (e.button == GLFW_MOUSE_BUTTON_LEFT)
				player.setBreaking(true);
		}
		else if (e.state == MouseEvent.Button.RELEASED)
		{
			// Reset status
			if (e.button == GLFW_MOUSE_BUTTON_RIGHT)
				player.setPlacing(false);
			else if (e.button == GLFW_MOUSE_BUTTON_LEFT)
				player.setBreaking(false);
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
		
		if (e.keycode >= GLFW_KEY_1 && e.keycode <= GLFW_KEY_9)
			player.changeSelectedBlock(e.keycode - GLFW_KEY_1);
		else if(e.keycode == GLFW_KEY_0)
			player.changeSelectedBlock(9);
		else if(e.keycode == GLFW_KEY_MINUS)
			player.changeSelectedBlock(10);
		
		// Toggle flying
		if (e.keycode == GLFW_KEY_F)
			player.isFlying = !player.isFlying;
		
		if (e.keycode == GLFW_KEY_B)
		{
			// BOOM!
			player.world.explode((int)player.xPos, (int)player.yPos, (int)player.zPos, 20);
		}
		
		// TODO: Pull out of PlayerController (Not appropriate here) or rename class
		if (e.keycode == GLFW_KEY_F3)
		{
			showThings = !showThings;
			
			if(showThings)
				System.out.println("DBG!");
		}
	}
	
	//// Main interface \\\\
	public void update(float delta)
	{
		// TODO: Move some of these things into EntityPlayer
		float xDir = 0.0f, zDir = 0.0f;
		
		player.speedCoef = 1f;
		if (window.isKeyDown(GLFW_KEY_LEFT_SHIFT))
			player.speedCoef = 0.25f;
		if (window.isKeyDown(GLFW_KEY_LEFT_CONTROL))
			player.speedCoef = 1.75f;
		
		if (window.isKeyDown(GLFW_KEY_W))
			zDir += -1.0f;
		if (window.isKeyDown(GLFW_KEY_S))
			zDir +=  1.0f;
		if (window.isKeyDown(GLFW_KEY_A))
			xDir += -1.0f;
		if (window.isKeyDown(GLFW_KEY_D))
			xDir +=  1.0f;
		
		if (window.isKeyDown(GLFW_KEY_SPACE))
			player.jump();
		
		// Update sneaking
		player.isSneaking = window.isKeyDown(GLFW_KEY_LEFT_SHIFT);
		
		// Update things
		player.move(xDir, zDir);
	}
	
}
