package ddb.io.voxelnet;

import ddb.io.voxelnet.render.Model;
import ddb.io.voxelnet.render.Shader;
import ddb.io.voxelnet.render.Texture;
import ddb.io.voxelnet.render.TextureAtlas;
import org.joml.Matrix4f;
import org.lwjgl.glfw.*;
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
	
	/** Current window associated with this game instance */
	long window;
	/** Current shader program */
	Shader shader;
	/** Currently rendered model */
	Model model;
	Texture texture;
	
	Matrix4f perspective = new Matrix4f();
	float x = 0.0f;
	float y = 0.0f;
	float z = 1.0f;
	float pitch = 45.0f;
	float yaw = 0.0f;
	
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
			perspective.identity();
			perspective.perspective((float) Math.toRadians(FOV), (float) width / (float) height, ZNEAR, ZFAR);
		});
		
		// Add mouse movement callback
		glfwSetCursorPosCallback(window, (window, x, y) -> {
			double deltaX = x - lastX;
			double deltaY = y - lastY;
			lastX = x;
			lastY = y;
			
			if(Math.abs(deltaX) > 50.0 || Math.abs(deltaY) > 50.0)
				return;
			
			pitch += -deltaY * MOUSE_SENSITIVITY;
			yaw += -deltaX * MOUSE_SENSITIVITY;
		});
		
		// Disable the cursor
		glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
		
		// Create the shader
		shader = new Shader("assets/shaders/default.glsl");
		
		// Load the texture
		texture = new Texture("assets/textures/atlas.png");
		texture.bind(0);
		
		// Create the texture atlas
		TextureAtlas atlas = new TextureAtlas(texture, 16, 16);
		float[] grassTop = atlas.getPositions(0);
		float[] grassSide = atlas.getPositions(1);
		float[] dirt = atlas.getPositions(2);
		
		// Create the model
		model = new Model();
		
		for (int cx = 0; cx < 2; cx++)
		{
			for (int cz = 0; cz < 2; cz++)
			{
				for (int i = 0; i < 16 * 16 * 16; i++)
				{
					int x = cx * 16 + i % 16;
					int y = i / 256;
					int z = cz * 16 + (i / 16) % 16;
					
					pushCubeFace(model, (float) x, (float) y, (float) z, 0, grassSide);
					pushCubeFace(model, (float) x, (float) y, (float) z, 1, grassSide);
					pushCubeFace(model, (float) x, (float) y, (float) z, 2, grassSide);
					pushCubeFace(model, (float) x, (float) y, (float) z, 3, grassSide);
					pushCubeFace(model, (float) x, (float) y, (float) z, 4, grassTop);
					pushCubeFace(model, (float) x, (float) y, (float) z, 5, dirt);
				}
			}
		}
		
		System.out.println(model.getIndexCount());
		
		model.bind();
		model.updateVertices();
		model.unbind();

		// Create the initial projection matrix
		perspective.perspective((float) Math.toRadians(FOV), (float) INITIAL_WIDTH / (float) INITIAL_HEIGHT, ZNEAR, ZFAR);
		
		shader.bind();
		shader.fixupModel(model);
		shader.setUniform1i("texture0", 0);
		shader.unbind();
	}
	
	// NWSE TB
	private void pushCubeFace(Model model, float x, float y, float z, int face, float[] texCoords)
	{
		model.beginPoly();
		switch (face)
		{
			case 0:
				// North Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
			case 1:
				// West Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3]);
				break;
			case 2:
				// South Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				break;
			case 3:
				// East Face
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
			case 4:
				// Top Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				break;
			case 5:
				// Bottom Face
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
		}
		model.endPoly();
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
		// Free the model
		texture.free();
		model.free();
		shader.free();
		
		// Free GLFW things
		glfwDestroyWindow(window);
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}
	
	private void update()
	{
		//System.out.println(pitch);
		float xDir = 0.0f, yDir = 0.0f, zDir = 0.0f;
		float speed = 4.0f / 60.0f;
		
		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
			zDir += -1.0f * speed;
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
			zDir +=  1.0f * speed;
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
			xDir += -1.0f * speed;
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
			xDir +=  1.0f * speed;
		
		if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
			yDir +=  1.0f * speed;
		if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS)
			yDir -=  1.0f * speed;
		
		double phi = Math.toRadians(yaw);
		x += xDir * Math.cos(phi) + zDir * Math.sin(phi);
		y += yDir;
		z -= xDir * Math.sin(phi) - zDir * Math.cos(phi);
		
		if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
			glfwSetWindowShouldClose(window, true);
		
		// Clamp the pitch
		if (pitch < -90.0f)
			pitch = -90.0f;
		if (pitch > 90.0f)
			pitch = 90.0f;
		
	}
	
	private void render(double partialTicks)
	{
		// Create the pvm matrix
		Matrix4f pvm = new Matrix4f();
		perspective.get(pvm);
		pvm.rotate((float) -Math.toRadians(pitch), 1.0f, 0.0f, 0.0f);
		pvm.rotate((float) -Math.toRadians(yaw), 0.0f, 1.0f, 0.0f);
		pvm.translate(-x, -y, -z);
		float[] mat = new float[4 * 4];
		
		shader.setUniformMatrix4fv("pvm", false, pvm.get(mat));
		
		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glCullFace(GL_BACK);
		glDepthFunc(GL_LEQUAL);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// Draw a textured cube
		shader.bind();
		model.bind();
		glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
		model.unbind();
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
