package ddb.io.voxelnet;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.input.PlayerController;
import ddb.io.voxelnet.render.*;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Frustum;
import ddb.io.voxelnet.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import sun.security.provider.certpath.Vertex;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Game {
	
	private static final int INITIAL_WIDTH = 854;
	private static final int INITIAL_HEIGHT = 480;
	
	private static final float FOV   = 70.0f;
	private static final float ZNEAR = 0.05f;
	private static final float ZFAR  = 1000.0f;
	
	public static boolean showThings = false;
	
	/** Current window associated with this game instance */
	long window;
	/** Current shader program */
	Shader chunkShader;
	Shader blackShader;
	/** List of chunks to render */
	Texture texture;
	World world;
	WorldRenderer worldRenderer;
	
	PlayerController controller;
	EntityPlayer player;
	Camera camera;
	
	Model hitBox;
	
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
		
		// Setup input modes
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_TRUE);
		
		// Create the shader
		chunkShader = new Shader("assets/shaders/default.glsl");
		
		// Load the texture
		texture = new Texture("assets/textures/atlas.png");
		texture.bind(0);
		
		// Create the texture atlas
		TextureAtlas atlas = new TextureAtlas(texture, 16, 16);
		
		// Initialize the blocks
		Block.init();
		
		// Setup the world and world renderer
		world = new World();
		worldRenderer = new WorldRenderer(world, atlas);
		
		// Setup the player
		player = new EntityPlayer();
		player.setPos(0.0f, 256.0f, 0.0f);
		player.setWorld(world);
		
		// Setup the controller
		controller = new PlayerController(window, player);
		
		// Setup the camera
		camera = new Camera(FOV, ZNEAR, ZFAR);
		camera.setOffset(0, player.eyeHeight, 0);
		
		// Setup the hitbox & shader
		blackShader = new Shader("assets/shaders/blackShader.glsl");
		hitBox = new Model();
		hitBox.drawLines = true;
		hitBox.beginPoly();
		hitBox.addVertex(0f, 0f, 0f);
		hitBox.addVertex(1f, 0f, 0f);
		hitBox.addVertex(1f, 1f, 0f);
		hitBox.addVertex(0f, 1f, 0f);
		hitBox.endPoly();
		
		hitBox.beginPoly();
		hitBox.addVertex(0f, 1f, 1f);
		hitBox.addVertex(1f, 1f, 1f);
		hitBox.addVertex(1f, 0f, 1f);
		hitBox.addVertex(0f, 0f, 1f);
		hitBox.endPoly();
		
		hitBox.beginPoly();
		hitBox.addVertex(0f, 0f, 0f);
		hitBox.addVertex(0f, 1f, 0f);
		hitBox.addVertex(0f, 1f, 1f);
		hitBox.addVertex(0f, 0f, 1f);
		hitBox.endPoly();
		
		hitBox.beginPoly();
		hitBox.addVertex(1f, 0f, 1f);
		hitBox.addVertex(1f, 1f, 1f);
		hitBox.addVertex(1f, 1f, 0f);
		hitBox.addVertex(1f, 0f, 0f);
		hitBox.endPoly();
		
		hitBox.beginPoly();
		hitBox.addVertex(0f, 0f, 0f);
		hitBox.addVertex(0f, 0f, 1f);
		hitBox.addVertex(1f, 0f, 1f);
		hitBox.addVertex(1f, 0f, 0f);
		hitBox.endPoly();
		
		hitBox.beginPoly();
		hitBox.addVertex(1f, 1f, 0f);
		hitBox.addVertex(1f, 1f, 1f);
		hitBox.addVertex(0f, 1f, 1f);
		hitBox.addVertex(0f, 1f, 0f);
		hitBox.endPoly();
		
		hitBox.bind();
		hitBox.updateVertices();
		hitBox.unbind();
		
		// Setup the initial projection matrix
		camera.updatePerspective((float) INITIAL_WIDTH / (float) INITIAL_HEIGHT);
		
		// Setup the view matrix
		camera.asPlayer(player);
		camera.updateView();
		
		chunkShader.bind();
		chunkShader.setUniform1i("texture0", 0);
		chunkShader.unbind();
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
				update((float)MS_PER_UPDATE);
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
		
		// Stop the generator threads
		worldRenderer.stop();
	}
	
	private void update(float delta)
	{
		controller.update(delta);
		player.update(delta);
		
		camera.asPlayer(player);
		camera.updateView();
		
		world.update();
		worldRenderer.update();
	}
	
	private void render(double partialTicks)
	{
		// Create the pvm matrix
		Matrix4f pvm = camera.getTransform();
		final Matrix4f modelMatrix = new Matrix4f();
		final float[] mat = new float[4 * 4];
		chunkShader.setUniformMatrix4fv("pvm", false, pvm.get(mat));
		
		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// Draw the world
		chunkShader.bind();
		worldRenderer.render(camera);
		chunkShader.unbind();
		
		if (controller.showHit)
		{
			final float scale = 0.00390625f;
			modelMatrix.translate(controller.blockX - scale / 2, controller.blockY - scale / 2, controller.blockZ - scale / 2);
			modelMatrix.scale(1.0f + scale);
			
			blackShader.setUniformMatrix4fv("PVMatrix", false, pvm.get(mat));
			blackShader.setUniformMatrix4fv("ModelMatrix", false, modelMatrix.get(mat));
			blackShader.bind();
			hitBox.bind();
			glDrawElements(GL_LINES, hitBox.getIndexCount(), GL_UNSIGNED_INT, 0L);
			hitBox.unbind();
			blackShader.unbind();
		}
		
		glPointSize(5.0f);
		glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
		glBegin(GL_POINTS);
		glVertex2f(0, 0);
		glEnd();
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
