package ddb.io.voxelnet;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.client.GameWindow;
import ddb.io.voxelnet.client.render.entity.EntityRendererFalling;
import ddb.io.voxelnet.client.render.entity.EntityRendererPlayer;
import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.client.render.gl.GLContext;
import ddb.io.voxelnet.client.render.util.Camera;
import ddb.io.voxelnet.entity.EntityFallingBlock;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.client.input.PlayerController;
import ddb.io.voxelnet.client.render.*;
import ddb.io.voxelnet.event.EventBus;
import ddb.io.voxelnet.event.input.KeyEvent;
import ddb.io.voxelnet.event.input.MouseEvent;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.util.RaycastResult;
import ddb.io.voxelnet.world.World;
import ddb.io.voxelnet.world.WorldSave;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Game {
	
	private static final int INITIAL_WIDTH = 854;
	private static final int INITIAL_HEIGHT = 480;
	
	public static final boolean ENABLE_VSYNC = true;
	
	private static final float FOV   = 90.0f;
	private static final float ZNEAR = 0.1f;
	private static final float ZFAR  = 1000.0f;
	
	public static boolean showThings = false;
	public static boolean showWireframe = false;
	public static boolean debugFluid = false;
	
	/** Current window associated with this game instance */
	GameWindow window;
	WorldSave worldSave;
	World world;
	
	EntityPlayer otherPlayer;
	EntityPlayer player;
	PlayerController controller;
	
	Model hitBox;
	
	// Rendering things
	Shader chunkShader;
	Shader blackShader;
	Shader quadShader;
	
	// The main texture atlas
	Texture texture;
	
	ModelBuilder quadrator;
	Model quads;
	Texture hudTexture;
	
	public Camera camera;
	public Camera guiCamera;
	WorldRenderer worldRenderer;
	GameRenderer renderer;
	FontRenderer fontRenderer;
	
	double elapsed = 0.0d;
	
	double frameTime = 0;
	double updTime = 0;
	int currentFPS = 0;
	double currentUPD = 0;
	
	// Global Event Bus
	public static final EventBus GLOBAL_BUS = new EventBus();
	
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
		
		window = new GameWindow(this, INITIAL_WIDTH, INITIAL_HEIGHT);
		window.init();
		
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
		Fluid.init();
		
		// Setup the game renderer
		renderer = new GameRenderer(atlas);
		
		// Register the entity renderers
		renderer.registerEntityRenderer(EntityFallingBlock.class, new EntityRendererFalling());
		renderer.registerEntityRenderer(EntityPlayer.class, new EntityRendererPlayer());
		
		///////////////////////////////////////////////
		// Initialize the gui & hud things
		hudTexture = new Texture("assets/textures/hud.png");
		quads = new Model(BufferLayout.QUAD_LAYOUT);
		quadrator = new ModelBuilder(BufferLayout.QUAD_LAYOUT, EnumDrawMode.TRIANGLES, 4);
		//quads.setTransform(new Matrix4f().identity());
		
		// Setup the camera
		camera = new Camera(FOV, ZNEAR, ZFAR);
		renderer.useCamera(camera);
		
		{
			// Setup the hitbox & shader
			blackShader = new Shader("assets/shaders/blackShader.glsl");
			
			BufferLayout simpleLayout = new BufferLayout();
			simpleLayout.addAttribute(BufferLayout.EnumAttribType.FLOAT, 3, false);
			ModelBuilder builder = new ModelBuilder(simpleLayout, EnumDrawMode.LINES, 4);
			
			hitBox = new Model(simpleLayout);
			hitBox.setDrawMode(EnumDrawMode.LINES);
			
			builder.addPoly(4);
			builder.pos3f(0f, 1f, 0f).endVertex();
			builder.pos3f(1f, 1f, 0f).endVertex();
			builder.pos3f(1f, 0f, 0f).endVertex();
			builder.pos3f(0f, 0f, 0f).endVertex();
			
			builder.addPoly(4);
			builder.pos3f(0f, 1f, 1f).endVertex();
			builder.pos3f(1f, 1f, 1f).endVertex();
			builder.pos3f(1f, 0f, 1f).endVertex();
			builder.pos3f(0f, 0f, 1f).endVertex();
			
			builder.addPoly(4);
			builder.pos3f(0f, 0f, 0f).endVertex();
			builder.pos3f(0f, 1f, 0f).endVertex();
			builder.pos3f(0f, 1f, 1f).endVertex();
			builder.pos3f(0f, 0f, 1f).endVertex();
			
			builder.addPoly(4);
			builder.pos3f(1f, 0f, 1f).endVertex();
			builder.pos3f(1f, 1f, 1f).endVertex();
			builder.pos3f(1f, 1f, 0f).endVertex();
			builder.pos3f(1f, 0f, 0f).endVertex();
			
			builder.addPoly(4);
			builder.pos3f(0f, 0f, 0f).endVertex();
			builder.pos3f(0f, 0f, 1f).endVertex();
			builder.pos3f(1f, 0f, 1f).endVertex();
			builder.pos3f(1f, 0f, 0f).endVertex();
			
			builder.addPoly(4);
			builder.pos3f(0f, 1f, 0f).endVertex();
			builder.pos3f(0f, 1f, 1f).endVertex();
			builder.pos3f(1f, 1f, 1f).endVertex();
			builder.pos3f(1f, 1f, 0f).endVertex();
			
			hitBox.bind();
			hitBox.updateVertices(builder);
			hitBox.unbind();
			
			// Reset the builder buffer
			builder.reset();
			builder.compact();
		}
		
		// Setup the initial projection matrix
		camera.updatePerspective((float) INITIAL_WIDTH / (float) INITIAL_HEIGHT);
		
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
		
		// Setup the world, world save/loader, and world renderer
		// "world-allthings" is main world
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
		worldRenderer.setClientPlayer(player);
		
		// Spawn the player at the surface
		int spawnY = 256;
		
		for (; spawnY >= 0; spawnY--)
		{
			boolean isSolid = world.getBlock(0, spawnY - 1, 0).isSolid();
			if (isSolid)
				break;
		}
		
		player.setPos(0.5f, spawnY + 0.5F, 0.5f);
		
		// Setup the controller
		controller = new PlayerController(window, player);
		
		// Add another player
		otherPlayer = new EntityPlayer();
		world.addEntity(otherPlayer);
		otherPlayer.setPos(0.5f, spawnY + 0.5f, 0.5f);
		
		// Setup the camera view matrix
		camera.setOffset(0, player.eyeHeight, 0);
		camera.asPlayer(player, 0);
		camera.updateView();
	}
	
	private void loop()
	{
		int fps = 0, ups = 0;
		double last = glfwGetTime();
		double lag = 0;
		
		double secondTimer = glfwGetTime();
		final double MS_PER_PHYSICS_TICK = 1.0 / 60.0;
		
		while(window.isWindowOpen())
		{
			double now = glfwGetTime();
			double elapsed = now - last;
			last = now;
			lag += elapsed;
			this.elapsed += elapsed;
			
			// Event Stage
			GLOBAL_BUS.processEvents();
			
			// Update Stage
			// Catchup loop
			boolean didUpdate = false;
			double updTick = glfwGetTime();
			
			// Cap the lag amount
			if (lag > 1f)
				lag = 1f;
			
			while(lag >= MS_PER_PHYSICS_TICK)
			{
				update((float)MS_PER_PHYSICS_TICK);
				ups++;
				lag -= MS_PER_PHYSICS_TICK;
				didUpdate = true;
			}
			
			if(didUpdate)
				updTime += glfwGetTime() - updTick;
			
			// Render Stage
			double renderTick = glfwGetTime();
			render(lag * MS_PER_PHYSICS_TICK);
			frameTime = glfwGetTime() - renderTick;
			fps++;
			
			if (now - secondTimer > 1)
			{
				// Update the things
				currentFPS = fps;
				currentUPD = updTime;
				
				updTime = 0;
				
				ups = 0;
				fps = 0;
				secondTimer = now;
			}
			
			// Update the window
			window.update();
		}
	}
	
	private void cleanup()
	{
		// Free the context
		GLContext.INSTANCE.free();
		
		// Free GLFW things
		window.destroy();
		glfwTerminate();
		glfwSetErrorCallback(null).free();
		
		// Stop the generator threads
		worldRenderer.stop();
		
		// Save the world
		//worldSave.save();
	}
	
	private void update(float delta)
	{
		otherPlayer.rotate(0f, 45.0f * delta);
		controller.update(delta);
		world.update(delta);
		
		worldRenderer.update();
	}
	
	private void render(double partialTicks)
	{
		camera.asPlayer(player, partialTicks);
		camera.updateView();
		
		renderer.begin();
		
		if (showWireframe)
			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		else
			glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		
		// Draw the world
		texture.bind(0);
		renderer.useCamera(camera);
		renderer.useShader(chunkShader);
		renderer.prepareShader();
		renderer.getCurrentShader().setUniform1f("iTime", (float) elapsed);
		worldRenderer.render(renderer);
		renderer.finishShader();
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		// ???: Is the hit box technically part of the HUD?
		// - Yep, also the in-world item display
		// TODO: Pull out of main game class into HUD rendering
		if (player.lastHit != RaycastResult.NO_RESULT)
		{
			// ???: Should the model matrix be part of the model?
			final float scale = 1/256f;
			Matrix4f modelMatrix = hitBox.getTransform();
			modelMatrix.identity();
			modelMatrix.translate(
					player.lastHit.blockX,
					player.lastHit.blockY,
					player.lastHit.blockZ);
			// Translate by the hit face in order to get a higher z-order
			modelMatrix.translate(
					player.lastHit.face.getOffsetX() * scale,
					player.lastHit.face.getOffsetY() * scale,
					player.lastHit.face.getOffsetZ() * scale);
			
			renderer.useShader(blackShader);
			renderer.prepareShader();
			renderer.drawModel(hitBox);
			renderer.finishShader();
		}
		
		renderer.useCamera(guiCamera);
		renderer.useShader(quadShader);
		renderer.prepareShader();
		
		// Draw gui & hud things here
		drawGuiLayer(partialTicks);
		
		renderer.finishShader();
	}
	
	private void drawTexturedQuad(int x, int y,
	                              int width, int height,
	                              int u, int v,
	                              int sliceWidth, int sliceHeight,
	                              int colour,
	                              Texture texture)
	{
		// Pre-calculate the dimensions & texture coordinates
		int xMin, yMin;
		int xMax, yMax;
		short uMin, vMin;
		short uMax, vMax;
		
		xMin = x;
		yMin = y;
		xMax = x + width;
		yMax = y + height;
		
		float uScale = (1.f / texture.getWidth()) * 65535.f;
		float vScale = (1.f / texture.getHeight()) * 65535.f;
		
		uMin = (short)((int) (u  * uScale) & 0xFFFF);
		vMin = (short)((int)((v * vScale)) & 0xFFFF);
		uMax = (short)((int)((u + sliceWidth ) * uScale) & 0xFFFF);
		vMax = (short)((int)((v + sliceHeight) * vScale) & 0xFFFF);
		
		// Split up the four components of the colour
		float r, g, b, a;
		
		r = ((colour >> (0*8)) & 0xFF) / 255.f;
		g = ((colour >> (1*8)) & 0xFF) / 255.f;
		b = ((colour >> (2*8)) & 0xFF) / 255.f;
		a = ((colour >> (3*8)) & 0xFF) / 255.f;
		
		quadrator.addPoly(4);
		quadrator.pos2f(xMin, yMin).tex2i(uMin, vMax).colour4(r,g,b,a).endVertex();
		quadrator.pos2f(xMin, yMax).tex2i(uMin, vMin).colour4(r,g,b,a).endVertex();
		quadrator.pos2f(xMax, yMax).tex2i(uMax, vMin).colour4(r,g,b,a).endVertex();
		quadrator.pos2f(xMax, yMin).tex2i(uMax, vMax).colour4(r,g,b,a).endVertex();
	}
	
	private void drawGuiLayer(double partialTicks)
	{
		hudTexture.bind(1);
		
		// Draw select cursor
		quadrator.reset();
		
		final int crosshairSize = 16;
		drawTexturedQuad((window.getWidth() - crosshairSize) / 2, (window.getHeight() - crosshairSize) / 2,
					      crosshairSize, crosshairSize,
				          0, (16-4)*16, 16, 16,
		                  0xBEFFFFFF, hudTexture);
		
		quads.bind();
		quads.updateVertices(quadrator);
		renderer.drawModel(quads);
		quads.unbind();
		
		// Draw debug strings
		int blockX = (int)Math.floor(player.xPos);
		int blockY = (int)Math.floor(player.yPos);
		int blockZ = (int)Math.floor(player.zPos);
		
		String nameVersion = "VoxelNet ?.?.?-net_test\n";
		String timeStr = String.format("FT %-5.2f (%d | %.3f) / UT %-5.2f\n", frameTime * 1000d, currentFPS, partialTicks, currentUPD * 1000d);
		String posStr = String.format("Pos %.2f / %.2f / %.2f\n", player.xPos, player.yPos, player.zPos);
		String lokStr = String.format("Rot %.2f / %.2f \n", player.yaw, player.pitch);
		String blkStr = String.format("I %02x M %s\n", world.getBlock(blockX, blockY, blockZ).getId(), Integer.toBinaryString(Byte.toUnsignedInt(world.getBlockMeta(blockX, blockY, blockZ))));
		String lyrStr = String.format("L %04d\n", world.getChunk(blockX >> 4, blockY >> 4, blockZ >> 4).getLayerData()[blockY & 0xF]);
		String ligStr = String.format("B %2d S %2d\n", world.getBlockLight(blockX, blockY, blockZ), world.getSkyLight(blockX, blockY, blockZ));
		String colStr = String.format("H %2d\n", world.getColumnHeight(blockX, blockY, blockZ));
		
		if (showThings || debugFluid)
			fontRenderer.putString(nameVersion+timeStr+posStr+lokStr+blkStr+ligStr+colStr, 0, 0);
		else
			fontRenderer.putString(nameVersion, 0, 0);
		fontRenderer.flush();
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
