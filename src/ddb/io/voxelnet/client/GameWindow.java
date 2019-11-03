package ddb.io.voxelnet.client;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.client.input.GameKeyBindings;
import ddb.io.voxelnet.client.input.InputBinding;
import ddb.io.voxelnet.client.input.InputBindingRegistry;
import ddb.io.voxelnet.event.input.KeyEvent;
import ddb.io.voxelnet.event.input.MouseEvent;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glViewport;

/**
 * Instance of the game window
 *
 * Handles input events & updates appropriately
 * Also handles the escape key & debug key inputs
 */
public class GameWindow
{
	// Handle to the GLFW window instance
	private long window;
	private Game gameInstance;
	private int initialWidth;
	private int initialHeight;
	
	// Last position of the mouse
	private double lastX = 0.0f, lastY = 0.0f;
	
	/**
	 * Constructs a new game window
	 * @param instance An instance of the game
	 * @param initialWidth The initial width of the window
	 * @param initialHeight The initial height of the window
	 */
	public GameWindow(Game instance, int initialWidth, int initialHeight)
	{
		this.gameInstance = instance;
		this.initialWidth = initialWidth;
		this.initialHeight = initialHeight;
	}
	
	/**
	 * Initializes the window
	 */
	public void init()
	{
		// Create the window (Minimum OpenGL version is 2.0, not resizable)
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		window = glfwCreateWindow(initialWidth, initialHeight, "VoxelNet", 0, 0);
		
		if (window == 0)
			throw new IllegalStateException("Failure creating the window");
		
		// Get monitor information
		// - Center the window
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			// Alloc temporary memory
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			
			glfwGetWindowSize(window, width, height);
			GLFWVidMode mode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			
			glfwSetWindowPos(window, (mode.width() - width.get()) / 2, (mode.height() - height.get()) / 2);
		}
		
		// Update the window context
		glfwMakeContextCurrent(window);
		// Setup vsync
		glfwSwapInterval(Game.ENABLE_VSYNC ? 1 : 0);
		// Show the window
		glfwShowWindow(window);
		
		// Setup GL Context
		GLCapabilities caps = GL.createCapabilities();
		
		// Setup input modes
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_TRUE);
		
		// Add window resizing callback
		glfwSetWindowSizeCallback(window, (win, width, height) -> {
			// Update the GL viewport size
			glViewport(0, 0, width, height);
			// Update the perspective matrices
			gameInstance.camera.updatePerspective((float) width / (float) height);
			gameInstance.guiCamera.updateOrtho(width, height);
		});
		
		// Setup event interfaces
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
	}
	
	/**
	 * Checks to see if the window is still open
	 * @return True if the window is still open
	 */
	public boolean isWindowOpen()
	{
		return !glfwWindowShouldClose(window);
	}
	
	/**
	 * Updates the states of the key bindings and swaps the window buffers
	 */
	public void update()
	{
		// Update the input bindings
		for (InputBinding binding : InputBindingRegistry.ALL_INPUT_BINDS)
		{
			boolean isInputActive = binding.getInputSource().isInputActive(window, binding.getInputCode());
			binding.setActive(isInputActive);
		}
			
		if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
			glfwSetWindowShouldClose(window, true);
		if (GameKeyBindings.TOGGLE_DEBUG.wasInputActivated())
			Game.showThings = !Game.showThings;
		
		glfwSwapBuffers(window);
		glfwPollEvents();
	}
	
	/**
	 * Destroys the window and associated resources
	 */
	public void destroy()
	{
		// Free the callbacks!
		glfwSetWindowSizeCallback(window, null).free();
		glfwSetCursorPosCallback(window, null).free();
		glfwSetMouseButtonCallback(window, null).free();
		glfwSetKeyCallback(window, null).free();
		
		glfwDestroyWindow(window);
	}
	
	public boolean isKeyDown(int keycode)
	{
		return glfwGetKey(window, keycode) == GLFW_PRESS;
	}
	
}
