package ddb.io.voxelnet.client.input;

import org.lwjgl.glfw.GLFW;

public class GameKeyBindings
{
	public static final InputBinding MOVE_FORWARD;
	public static final InputBinding MOVE_BACKWARD;
	public static final InputBinding MOVE_LEFT;
	public static final InputBinding MOVE_RIGHT;
	
	public static final InputBinding SPRINT;
	public static final InputBinding SNEAK;
	
	public static final InputBinding BREAK_BLOCK;
	public static final InputBinding PLACE_BLOCK;
	
	static
	{
		MOVE_FORWARD    = InputBindingRegistry.registerInputBinding("move_forward", InputSource.KEYBOARD, GLFW.GLFW_KEY_W);
		MOVE_BACKWARD   = InputBindingRegistry.registerInputBinding("move_backward", InputSource.KEYBOARD, GLFW.GLFW_KEY_S);
		MOVE_LEFT       = InputBindingRegistry.registerInputBinding("move_left", InputSource.KEYBOARD, GLFW.GLFW_KEY_A);
		MOVE_RIGHT      = InputBindingRegistry.registerInputBinding("move_right", InputSource.KEYBOARD, GLFW.GLFW_KEY_D);
		
		SPRINT          = InputBindingRegistry.registerInputBinding("sprint", InputSource.KEYBOARD, GLFW.GLFW_KEY_LEFT_CONTROL);
		SNEAK           = InputBindingRegistry.registerInputBinding("sneak", InputSource.KEYBOARD, GLFW.GLFW_KEY_LEFT_SHIFT);
		
		BREAK_BLOCK     = InputBindingRegistry.registerInputBinding("break_block", InputSource.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT);
		PLACE_BLOCK     = InputBindingRegistry.registerInputBinding("break_block", InputSource.MOUSE, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	}
	
	/**
	 * Registers the input bindings
	 */
	public static void initBindings()
	{
		// Boops the static initializer
	}
	
}
