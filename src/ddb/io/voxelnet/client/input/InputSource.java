package ddb.io.voxelnet.client.input;

import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;

/**
 * All handled input sources
 *
 * Each input source handles how an input is determined to be active
 */
public enum InputSource
{
	MOUSE((window, code) -> glfwGetMouseButton(window, code) == GLFW_PRESS),
	KEYBOARD((window, code) -> glfwGetKey(window, code) == GLFW_PRESS),
	;
	
	private InputSourceHandler sourceHandler;
	
	InputSource(InputSourceHandler sourceHandler)
	{
		this.sourceHandler = sourceHandler;
	}
	
	public boolean isInputActive (long window, int code)
	{
		return this.sourceHandler.isInputActive(window, code);
	}
	
	private interface InputSourceHandler
	{
		boolean isInputActive (long window, int code);
	}
}
