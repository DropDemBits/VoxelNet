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
		Game.GLOBAL_BUS.addHandler(MouseEvent.Move.class, this::onMouseMove);
		Game.GLOBAL_BUS.addHandler(KeyEvent.Button.class, this::onKeyButton);
	}
	
	//// Event Handlers \\\\
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
		
		// TODO: Change to a 10 hotbar inventory selection
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
	}
	
	//// Main interface \\\\
	public void update(float delta)
	{
		float xDir = 0.0f, zDir = 0.0f;
		
		if (GameKeyBindings.MOVE_FORWARD.isInputActive())
			zDir += -1.0f;
		if (GameKeyBindings.MOVE_BACKWARD.isInputActive())
			zDir +=  1.0f;
		if (GameKeyBindings.MOVE_LEFT.isInputActive())
			xDir += -1.0f;
		if (GameKeyBindings.MOVE_RIGHT.isInputActive())
			xDir +=  1.0f;
		
		if (GameKeyBindings.JUMP.isInputActive())
			player.jump();
		
		// Update break & place state
		player.setPlacing(GameKeyBindings.PLACE_BLOCK.isInputActive());
		player.setBreaking(GameKeyBindings.BREAK_BLOCK.isInputActive());
		
		// Update sneaking & sprinting
		player.isSneaking = GameKeyBindings.SNEAK.isInputActive();
		// If a player was already sprinting, keep sprinting
		player.isSprinting |= GameKeyBindings.SPRINT.isInputActive();
		
		// Update movement
		player.move(xDir, zDir);
	}
	
}
