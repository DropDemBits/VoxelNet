package ddb.io.voxelnet;

import ddb.io.voxelnet.render.Model;
import ddb.io.voxelnet.render.Shader;
import ddb.io.voxelnet.render.Texture;
import ddb.io.voxelnet.render.TextureAtlas;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;
import sun.security.provider.certpath.Vertex;

import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class Game {
	
	/** Current window associated with this game instance */
	long window;
	/** Current shader program */
	Shader shader;
	/** Currently rendered model */
	Model model;
	
	private void run()
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
		window = glfwCreateWindow( 640, 480, "Hello World", 0, 0);
		
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
		
		// Add window resizing callback
		glfwSetWindowSizeCallback(window, (win, width, height) -> {
			// Update the GL viewport size
			glViewport(0, 0, width, height);
		});
		
		// Setup GL Context
		GLCapabilities caps = GL.createCapabilities();
		
		// Create the shader
		shader = new Shader("assets/shaders/default.glsl");
		
		// Load the texture
		Texture texture = new Texture("assets/textures/atlas.png");
		texture.bind(0);
		shader.setUniform("texture0", 0);
		
		// Create the texture atlas
		TextureAtlas atlas = new TextureAtlas(texture, 16, 16);
		float[] pos = atlas.getPositions(0);
		
		// Create the model
		model = new Model();
		
		model.beginPoly();
		model.addVertex(-0.5f, -0.5f, 0.0f, pos[0], pos[1]);
		model.addVertex( 0.5f, -0.5f, 0.0f, pos[2], pos[1]);
		model.addVertex( 0.5f,  0.5f, 0.0f, pos[2], pos[3]);
		model.addVertex(-0.5f,  0.5f, 0.0f, pos[0], pos[3]);
		model.endPoly();
		
		model.bind();
		model.updateVertices();
		model.unbind();
		
		shader.fixupModel(model);
		
		/// Done Init ///
		/// Enter Main Loop  ///
		
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
		//System.out.println("Hey");
	}
	
	private void render(double partialTicks)
	{
		glClearColor(0f, 0f, 0f, 1f);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		// Draw two triangles (VBO + Shaders)
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
