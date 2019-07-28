package ddb.io.voxelnet;

import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.render.*;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;
import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import javax.jws.WebParam;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Game {
	
	private static final int INITIAL_WIDTH = 854;
	private static final int INITIAL_HEIGHT = 480;
	
	private static final float FOV   = 60.0f;
	private static final float ZNEAR = 0.01f;
	private static final float ZFAR  = 1000.0f;
	
	private static final float MOUSE_SENSITIVITY = 0.5f;
	
	/** Current window associated with this game instance */
	long window;
	/** Current shader program */
	Shader shader;
	/** List of chunks to render */
	Texture texture;
	World world;
	WorldRenderer worldRenderer;
	
	EntityPlayer player;
	Camera camera;
	
	double lastX = 0.0f, lastY = 0.0f;
	
	private void run()
	{
		/// Init ///
		init();
		/// Main Loop ///
		loop();
		/// Cleanup ///
		cleanup();
	}
	
	private void init()
	{
		// Setup the error callback
		GLFWErrorCallback.createPrint(System.err).set();
		
		// Initialize GLFW
		if(!glfwInit())
			throw new IllegalStateException("Failure initializing GLFW");
		
		// Create the window (Minimum OpenGL version is 2.0, not resizable)
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		window = glfwCreateWindow( INITIAL_WIDTH, INITIAL_HEIGHT, "VoxelNet", 0, 0);
		
		if (window == 0)
			throw new IllegalStateException("Failure creating the window");
		
		// Get monitor information
		// - Center the window
		// - Fetch refresh rate
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
		glfwSwapInterval(1);
		// Show the window
		glfwShowWindow(window);
		
		// Setup GL Context
		GLCapabilities caps = GL.createCapabilities();
		
		// Add window resizing callback
		glfwSetWindowSizeCallback(window, (win, width, height) -> {
			// Update the GL viewport size
			glViewport(0, 0, width, height);
			// Update the perspective matrix
			camera.updatePerspective((float) width / (float) height);
		});
		
		// Add mouse movement callback
		glfwSetCursorPosCallback(window, (window, x, y) -> {
			double deltaX = x - lastX;
			double deltaY = y - lastY;
			lastX = x;
			lastY = y;
			
			if(Math.abs(deltaX) > 50.0 || Math.abs(deltaY) > 50.0)
				return;
			
			player.rotate(
					(float) -deltaY * MOUSE_SENSITIVITY,
					 (float) -deltaX * MOUSE_SENSITIVITY
			);
		});
		
		// Setup input modes
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_TRUE);
		
		// Create the shader
		shader = new Shader("assets/shaders/default.glsl");
		
		// Load the texture
		texture = new Texture("assets/textures/atlas.png");
		texture.bind(0);
		
		// Create the texture atlas
		TextureAtlas atlas = new TextureAtlas(texture, 16, 16);
		
		// Setup the world and world renderer
		world = new World();
		worldRenderer = new WorldRenderer(world, atlas);
		
		// Setup the player and the camera
		player = new EntityPlayer();
		player.setPos(0.0f, 68.0f, 0.0f);
		
		camera = new Camera(FOV, ZNEAR, ZFAR);
		
		// Setup the initial projection matrix
		camera.updatePerspective((float) INITIAL_WIDTH / (float) INITIAL_HEIGHT);
		
		// Setup the view matrix
		camera.asPlayer(player);
		camera.updateView();
		
		shader.bind();
		shader.setUniform1i("texture0", 0);
		shader.unbind();
	}
	
	private void loop()
	{
		int fps = 0, ups = 0;
		double last = glfwGetTime();
		double lag = 0;
		
		double secondTimer = glfwGetTime();
		final double MS_PER_UPDATE = 1.0 / 60.0;
		
		while(!glfwWindowShouldClose(window))
		{
			double now = glfwGetTime();
			double elapsed = now - last;
			last = now;
			lag += elapsed;
			
			// Input Stage
			
			// Update Stage
			// Catchup loop
			while(lag >= MS_PER_UPDATE)
			{
				update();
				ups++;
				lag -= MS_PER_UPDATE;
			}
			
			// Render Stage
			render(lag / MS_PER_UPDATE);
			fps++;
			
			if (now - secondTimer > 1)
			{
				System.out.println("FPS: " + fps + ", " + " UPS: " + ups);
				
				// Update the things
				ups = 0;
				fps = 0;
				secondTimer = now;
			}
			
			// Update GLFW
			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}
	
	private void cleanup()
	{
		// Free the context
		GLContext.INSTANCE.free();
		
		// Free GLFW things
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}
	
	private void update()
	{
		float xDir = 0.0f, yDir = 0.0f, zDir = 0.0f;
		float speed = 4.0f / 60.0f;
		
		if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS)
			speed *= 1.5f;
		
		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			zDir += -1.0f;
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			zDir +=  1.0f;
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			xDir += -1.0f;
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			xDir +=  1.0f;
		
		if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
			yDir +=  1.0f;
		if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
			yDir -=  1.0f;
		
		if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
			glfwSetWindowShouldClose(window, true);
		
		player.speed = speed;
		player.move(xDir / 4.0f, yDir, zDir / 4.0f);
		player.update();
		
		camera.asPlayer(player);
		camera.updateView();
		
		world.update();
		worldRenderer.update();
	}
	
	private void render(double partialTicks)
	{
		// Create the pvm matrix
		Matrix4f pvm = camera.getTransform();
		float[] mat = new float[4 * 4];
		shader.setUniformMatrix4fv("pvm", false, pvm.get(mat));
		
		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		glDepthFunc(GL_LEQUAL);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// Draw the chunks
		shader.bind();
		
		worldRenderer.render();
		
		shader.unbind();
	}
	
	private void parseArgs(String[] args) {}
	
	public static void main(String... args)
	{
		// Launch the game into a new thread
		final Game game = new Game();
		
		game.parseArgs(args);
		new Thread(game::run, "Client").start();
	}
}
