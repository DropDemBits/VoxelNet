package ddb.io.voxelnet.client.input;

import org.lwjgl.glfw.GLFW;

public class GameKeyBindings
{
	// Player movement
	public static final InputBinding MOVE_FORWARD;
	public static final InputBinding MOVE_BACKWARD;
	public static final InputBinding MOVE_LEFT;
	public static final InputBinding MOVE_RIGHT;
	public static final InputBinding SPRINT;
	public static final InputBinding SNEAK;
	public static final InputBinding JUMP;
	
	// Place & break
	public static final InputBinding BREAK_BLOCK;
	public static final InputBinding PLACE_BLOCK;
	
	// Debug
	public static final InputBinding TOGGLE_DEBUG;
	public static final InputBinding TOGGLE_WIREFRAME;
	
	static
	{
		MOVE_FORWARD    = InputBindingRegistry.registerInputBinding("move_forward", InputSource.KEYBOARD, GLFW.GLFW_KEY_W);
		MOVE_BACKWARD   = InputBindingRegistry.registerInputBinding("move_backward", InputSource.KEYBOARD, GLFW.GLFW_KEY_S);
		MOVE_LEFT       = InputBindingRegistry.registerInputBinding("move_left", InputSource.KEYBOARD, GLFW.GLFW_KEY_A);
		MOVE_RIGHT      = InputBindingRegistry.registerInputBinding("move_right", InputSource.KEYBOARD, GLFW.GLFW_KEY_D);
		
		SPRINT          = InputBindingRegistry.registerInputBinding("sprint", InputSource.KEYBOARD, GLFW.GLFW_KEY_LEFT_CONTROL);
		SNEAK           = InputBindingRegistry.registerInputBinding("sneak", InputSource.KEYBOARD, GLFW.GLFW_KEY_LEFT_SHIFT);
		JUMP            = InputBindingRegistry.registerInputBinding("jump", InputSource.KEYBOARD, GLFW.GLFW_KEY_SPACE);
		
		BREAK_BLOCK     = InputBindingRegistry.registerInputBinding("break_block", InputSource.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT);
		PLACE_BLOCK     = InputBindingRegistry.registerInputBinding("place_block", InputSource.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		
		TOGGLE_DEBUG    = InputBindingRegistry.registerInputBinding("toggle_debug", InputSource.KEYBOARD, GLFW.GLFW_KEY_F3);
		TOGGLE_WIREFRAME = InputBindingRegistry.registerInputBinding("toggle_wire", InputSource.KEYBOARD, GLFW.GLFW_KEY_F6);
	}
	
}
