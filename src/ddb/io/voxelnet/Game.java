package ddb.io.voxelnet;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.client.render.entity.EntityRendererFalling;
import ddb.io.voxelnet.entity.EntityFallingBlock;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.client.input.PlayerController;
import ddb.io.voxelnet.client.render.*;
import ddb.io.voxelnet.event.EventBus;
import ddb.io.voxelnet.event.input.KeyEvent;
import ddb.io.voxelnet.event.input.MouseEvent;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;
import ddb.io.voxelnet.world.WorldSave;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

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
	WorldSave worldSave;
	World world;
	
	EntityPlayer player;
	PlayerController controller;
	
	Model hitBox;
	
	// Rendering things
	Shader chunkShader;
	Shader blackShader;
	Shader quadShader;
	
	Texture texture;
	
	Camera camera;
	Camera guiCamera;
	WorldRenderer worldRenderer;
	GameRenderer renderer;
	FontRenderer fontRenderer;
	
	double frameTime = 0;
	double updTime = 0;
	
	// Global Event Bus
	public static final EventBus GLOBAL_BUS = new EventBus();
	
	// Might add:
	// Sand, Gravel (Falling blocks)
	//  - Entities
	//  - Generalized Collision
	//  - Neighbor Block Updates
	
	private void run()
	{
		/// Init ///
		init();
		
		// Catch all the bad exceptions
		try
		{
			/// Main Loop ///
			loop();
		}
		catch (Exception e)
		{
			System.err.println("Oops, a bad thing happened!");
			e.printStackTrace();
		}
		finally
		{
			/// Cleanup ///
			cleanup();
		}
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
			// Update the perspective matrices
			camera.updatePerspective((float) width / (float) height);
			guiCamera.updateOrtho(width, height);
		});
		
		// Setup input modes
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		glfwSetInputMode(window, GLFW_STICKY_KEYS, GLFW_TRUE);
		
		// Setup the input events
		GLOBAL_BUS.registerEvent(MouseEvent.Move.class);
		GLOBAL_BUS.registerEvent(MouseEvent.Button.class);
		GLOBAL_BUS.registerEvent(KeyEvent.Button.class);
		
		///////////////////////////////////////////////
		// Create the shader
		chunkShader = new Shader("assets/shaders/default.glsl");
		
		// Load the texture
		texture = new Texture("assets/textures/atlas.png");
		texture.bind(0);
		
		// Create the texture atlas
		TextureAtlas atlas = new TextureAtlas(texture, 16, 16);
		
		// Initialize the blocks
		Block.init();
		
		// Setup the game renderer
		renderer = new GameRenderer(atlas);
		
		// Register the entity renderers
		renderer.registerEntityRenderer(EntityFallingBlock.class, new EntityRendererFalling());
		
		// Setup the world, world save/loader, and world renderer
		world = new World();
		worldSave = new WorldSave(world, "world.dat");
		worldRenderer = new WorldRenderer(world, atlas);
		
		// Load / Generate the world
		if (worldSave.canLoad())
			worldSave.load();
		else
			world.generate();
		
		// Setup the player
		player = new EntityPlayer();
		world.addEntity(player);
		worldRenderer.setPlayer(player);
		
		// Spawn the player at the surface
		int spawnY = 0;
		
		for (; spawnY < 256; spawnY++)
		{
			if (!Block.idToBlock(world.getBlock(0, spawnY, 0)).isSolid())
				break;
		}
		
		player.setPos(0.5f, spawnY, 0.5f);
		
		// Setup the controller
		controller = new PlayerController(window, player);
		
		// Setup the camera
		camera = new Camera(FOV, ZNEAR, ZFAR);
		camera.setOffset(0, player.eyeHeight, 0);
		renderer.useCamera(camera);
		
		// Setup the hitbox & shader
		blackShader = new Shader("assets/shaders/blackShader.glsl");
		
		BufferLayout simpleLayout = new BufferLayout();
		simpleLayout.addAttribute(BufferLayout.EnumAttribType.FLOAT, 3, false);
		
		hitBox = new Model(simpleLayout);
		hitBox.setTransform(new Matrix4f());
		hitBox.setDrawMode(EnumDrawMode.TRIANGLES);
		
		/*
		hitBox.beginPoly();
		hitBox.addVertex(0f, 1f, 0f);
		hitBox.addVertex(1f, 1f, 0f);
		hitBox.addVertex(1f, 0f, 0f);
		hitBox.addVertex(0f, 0f, 0f);
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
		//*/
		
		hitBox.beginPoly();
		hitBox.addVertex(0f, 1f, 0f);
		hitBox.addVertex(0f, 1f, 1f);
		hitBox.addVertex(1f, 1f, 1f);
		hitBox.addVertex(1f, 1f, 0f);
		hitBox.endPoly();
		
		hitBox.bind();
		hitBox.updateVertices();
		hitBox.unbind();
		
		// Setup the initial projection matrix
		camera.updatePerspective((float) INITIAL_WIDTH / (float) INITIAL_HEIGHT);
		
		// Setup the view matrix
		camera.asPlayer(player);
		camera.updateView();
		
		// Setup the main texture
		chunkShader.bind();
		chunkShader.setUniform1i("texture0", 0);
		chunkShader.unbind();
		
		// Setup the 2D view
		guiCamera = new Camera(INITIAL_WIDTH, INITIAL_HEIGHT);
		quadShader = new Shader("assets/shaders/2dlayer.glsl");
		fontRenderer = new FontRenderer("assets/textures/font.png");
		quadShader.bind();
		quadShader.setUniform1i("texture0", 1);
		quadShader.unbind();
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
			
			// Event Stage
			GLOBAL_BUS.processEvents();
			
			// Update Stage
			// Catchup loop
			boolean didUpdate = false;
			double updTick = glfwGetTime();
			while(lag >= MS_PER_UPDATE)
			{
				update((float)MS_PER_UPDATE);
				ups++;
				lag -= MS_PER_UPDATE;
				didUpdate = true;
			}
			
			if(didUpdate)
				updTime = glfwGetTime() - updTick;
			
			// Render Stage
			double renderTick = glfwGetTime();
			render(lag / MS_PER_UPDATE);
			frameTime = glfwGetTime() - renderTick;
			fps++;
			
			if (now - secondTimer > 1)
			{
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
		
		// Save the world
		//worldSave.save();
	}
	
	private void update(float delta)
	{
		controller.update(delta);
		world.update(delta);
		
		camera.asPlayer(player);
		camera.updateView();
		
		worldRenderer.update();
	}
	
	private void render(double partialTicks)
	{
		renderer.begin();
		
		// Draw the world
		texture.bind(0);
		renderer.useCamera(camera);
		renderer.useShader(chunkShader);
		renderer.prepareShader();
		worldRenderer.render(renderer);
		renderer.finishShader();
		
		// ???: Is the hit box technically part of the HUD?
		if (controller.showHit)
		{
			// ???: Should the model matrix be part of the model?
			final float scale = 0.00390625f;
			Matrix4f modelMatrix = hitBox.getTransform();
			modelMatrix.identity();
			modelMatrix.translate(controller.blockX, controller.blockY, controller.blockZ);
			modelMatrix.translate(-scale / 2f, -scale / 2f, -scale / 2f);
			modelMatrix.scale(1.0f + scale);
			
			modelMatrix.translate(0.5f, 0.5f, 0.5f);
			
			switch (controller.hitFace)
			{
				case NORTH: modelMatrix.rotate((float) Math.toRadians(-90), 1, 0, 0); break;
				case SOUTH: modelMatrix.rotate((float) Math.toRadians( 90), 1, 0, 0); break;
				case EAST:  modelMatrix.rotate((float) Math.toRadians(-90), 0, 0, 1); break;
				case WEST:  modelMatrix.rotate((float) Math.toRadians( 90), 0, 0, 1); break;
				case UP:    modelMatrix.translate(0, 0.01f, 0); break;
				case DOWN:  modelMatrix.rotate((float) Math.toRadians(180), 1, 0, 0); break;
			}
			
			modelMatrix.translate(-0.5f, -0.5f, -0.5f);
			
			renderer.useShader(blackShader);
			renderer.prepareShader();
			renderer.drawModel(hitBox);
			renderer.finishShader();
		}
		
		// TODO: Draw HUD & GUI elements here
		glPointSize(5.0f);
		glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
		glBegin(GL_POINTS);
		glVertex2f(0, 0);
		glEnd();
		
		// Test string
		renderer.useCamera(guiCamera);
		renderer.useShader(quadShader);
		renderer.prepareShader();
		String timeStr = String.format("FT %-5.2f / UT %-5.2f\n", frameTime * 1000d, updTime * 1000d);
		String posStr = String.format("Pos %.2f / %.2f / %.2f\n", player.xPos, player.yPos, player.zPos);
		String lokStr = String.format("Rot %.2f / %.2f \n", player.yaw, player.pitch);
		fontRenderer.putString("VoxelNet\n"+timeStr+posStr+lokStr, 0, 0);
		
		String builtStr =
				/*"According to all known laws of aviation,\n" +
				"it is impossible for a bee to fly.\n" +
				"public static void main(String[] args) {\n" +
				"    System.out.println(\"Hello, world!\");\n" +
				"}";*/
				"The quick brown fox jumped over the lazy dogs\n"+
				"THE QUICK BROWN FOX JUMPED OVER THE LAZY DOGS\n"+
				"the quick brown fox jumped over the lazy dogs\n"+
				"qpwmnleqjp_ block_grass";
		fontRenderer.putString(builtStr, 300, 0);
		
		fontRenderer.flush();
		renderer.finishShader();
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
