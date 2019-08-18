package ddb.io.voxelnet;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.render.*;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
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
	
	private static final float FOV   = 60.0f;
	private static final float ZNEAR = 0.01f;
	private static final float ZFAR  = 1000.0f;
	
	private static final float MOUSE_SENSITIVITY = 0.5f;
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
	
	EntityPlayer player;
	Camera camera;
	
	double lastX = 0.0f, lastY = 0.0f;
	
	// PlayerController variables
	float hitX = 0.0f;
	float hitY = 0.0f;
	float hitZ = 0.0f;
	int blockX = 0;
	int blockY = 0;
	int blockZ = 0;
	Facing hitFace = Facing.NORTH;
	boolean showHit = false;
	byte placeID = 1;
	
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
		
		// Add mouse movement callback
		// TODO: Shove into a player controller class \/
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
			
			showHit = raycast();
		});
		
		glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
			if (action == GLFW_PRESS)
			{
				// Ignore all other mouse buttons
				if (button != GLFW_MOUSE_BUTTON_LEFT && button != GLFW_MOUSE_BUTTON_RIGHT)
					return;
				
				// No block was found in range
				if(!raycast())
					return;
				
				if (button == GLFW_MOUSE_BUTTON_RIGHT)
				{
					Block block = Block.idToBlock(placeID);
					
					int[] off = hitFace.getOffset();
					// If the block can't be placed, don't place if
					if(!block.canPlaceBlock(world, blockX + off[0], blockY + off[1], blockZ + off[2]))
						return;
					
					world.setBlock(blockX + off[0], blockY + off[1], blockZ + off[2], placeID);
					block.onBlockPlaced(world, blockX + off[0], blockY + off[1], blockZ + off[2]);
				}
				else if (button == GLFW_MOUSE_BUTTON_LEFT)
				{
					Block block = Block.idToBlock(world.getBlock(blockX, blockY, blockZ));
					block.onBlockBroken(world, blockX, blockY, blockZ);
					world.setBlock(blockX, blockY, blockZ, (byte) 0);
				}
				
				// Update the hitbox position
				showHit = raycast();
			}
		});
		
		glfwSetKeyCallback(window, (window, keycode, scancode, action, mods) -> {
			if (action != GLFW_PRESS)
				return;
			
			// Select block to place
			final Block[] placeBlocks = new Block[] {
					Blocks.GRASS, Blocks.DIRT, Blocks.STONE,
					Blocks.PLANKS, Blocks.STONE_BRICKS, Blocks.CLAY_BRICKS,
					Blocks.DOOR_LOWER, Blocks.GLASS,
			};
			
			if (keycode >= GLFW_KEY_1 && keycode <= (GLFW_KEY_0 + placeBlocks.length))
				placeID = placeBlocks[keycode - GLFW_KEY_1].getId();
			
			// Toggle flying
			if (keycode == GLFW_KEY_F)
				player.isFlying = !player.isFlying;
			
			if (keycode == GLFW_KEY_F3)
				showThings = !showThings;
			
			if (keycode == GLFW_KEY_B)
			{
				// BOOM!
				world.explode((int)player.xPos, (int)player.yPos, (int)player.zPos, 20);
			}
		});
		// TODO: Shove into a player controller class /\
		
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
	
	private boolean raycast()
	{
		// Calculate the pointing vector
		Vector3f point = new Vector3f(0.0f, 0.0f, 1.0f);
		point.rotateAxis((float) -Math.toRadians(player.pitch), 1f, 0f, 0f);
		point.rotateAxis((float) -Math.toRadians(player.yaw),   0f, 1f, 0f);
		point.mul(0.25f);
		
		float rayX = player.xPos;
		float rayY = player.yPos + player.eyeHeight;
		float rayZ = player.zPos;
		
		int x, y, z;
		
		// Step for 5 blocks
		for(int i = 0; i < 7 * 4; i++)
		{
			x = Math.round(rayX - 0.5f);
			y = Math.round(rayY - 0.5f);
			z = Math.round(rayZ - 0.5f);
			
			if (world.getBlock(x, y, z) != 0)
			{
				rayX -= point.x;
				rayY -= point.y;
				rayZ += point.z;
				
				hitX = rayX;
				hitY = rayY;
				hitZ = rayZ;
				
				// Block found, get the specific face
				Vector3f hit = new Vector3f(rayX - x - 0.5f, rayY - y - 0.5f, rayZ - z - 0.5f);
				final Vector3f xAxis = new Vector3f(0.5f, 0.0f, 0.0f);
				final Vector3f yAxis = new Vector3f(0.0f, 0.5f, 0.0f);
				final Vector3f zAxis = new Vector3f(0.0f, 0.0f, 0.5f);
				
				float dotX = hit.dot(xAxis);
				float dotY = hit.dot(yAxis);
				float dotZ = hit.dot(zAxis);
				
				if (Math.abs(dotZ) > Math.abs(dotY) && Math.abs(dotZ) > Math.abs(dotX))
				{
					if (dotZ > 0) hitFace = Facing.SOUTH;
					else          hitFace = Facing.NORTH;
				}
				else if (Math.abs(dotX) > Math.abs(dotY) && Math.abs(dotX) > Math.abs(dotZ))
				{
					if (dotX > 0) hitFace = Facing.EAST;
					else          hitFace = Facing.WEST;
				}
				else if (Math.abs(dotY) >= Math.abs(dotX) && Math.abs(dotY) >= Math.abs(dotZ))
				{
					if (dotY > 0) hitFace = Facing.UP;
					else          hitFace = Facing.DOWN;
				}
				
				blockX = x;
				blockY = y;
				blockZ = z;
				return true;
			}
			
			rayX += point.x;
			rayY += point.y;
			rayZ -= point.z;
		}
		
		blockX = -1;
		blockY = -1;
		blockZ = -1;
		return false;
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
		
		// Stop the generator threads
		worldRenderer.stop();
	}
	
	private void update()
	{
		// TODO: Shove into a player controller class \/
		float xDir = 0.0f, yDir = 0.0f, zDir = 0.0f;
		//float speed = 6.0f / 60.0f;
		
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
		player.update();
		// TODO: Shove into a player controller class /\
		
		camera.asPlayer(player);
		camera.updateView();
		
		if (xDir != 0.0f || yDir != 0.0f || zDir != 0.0f)
			showHit = raycast();
		
		world.update();
		worldRenderer.update();
	}
	
	private void render(double partialTicks)
	{
		// Create the pvm matrix
		Matrix4f pvm = camera.getTransform();
		final Matrix4f modelMatrix = new Matrix4f();
		float[] mat = new float[4 * 4];
		chunkShader.setUniformMatrix4fv("pvm", false, pvm.get(mat));
		
		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// Draw the chunks
		chunkShader.bind();
		
		worldRenderer.render();
		
		chunkShader.unbind();
		
		if (showHit)
		{
			final float scale = 0.00390625f;
			modelMatrix.translate(blockX - scale / 2, blockY - scale / 2, blockZ - scale / 2);
			modelMatrix.scale(1.0f + scale);
			
			blackShader.setUniformMatrix4fv("PVMatrix", false, pvm.get(mat));
			blackShader.setUniformMatrix4fv("ModelMatrix", false, modelMatrix.get(mat));
			blackShader.bind();
			hitBox.bind();
			glDrawElements(GL_LINES, hitBox.getIndexCount(), GL_UNSIGNED_INT, 0L);
			hitBox.unbind();
			blackShader.unbind();
		}
		
		// Show hit point
		/*Vector4f toof = new Vector4f(hitX, hitY, hitZ, 1.0f);
		toof.mul(camera.getTransform());
		toof.mul(1.0f / toof.w);
		
		glPointSize(10.0f);
		glColor3f(1.0f, 0.0f, 0.0f);
		glBegin(GL_POINTS);
		glVertex2f(toof.x, toof.y);
		glEnd();*/
		
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
