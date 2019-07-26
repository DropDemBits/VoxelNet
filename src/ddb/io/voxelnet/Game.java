package ddb.io.voxelnet;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import sun.security.provider.certpath.Vertex;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class Game {
	
	/** Current window associated with this game instance */
	long window;
	/** Current shader program */
	int shader;
	/** Handle for the vertex buffer */
	int model;
	
	final float[] vertData = new float[] {
			-0.5f, -0.5f, /**/ 1.0f, 0.0f, 0.0f,
			 0.0f,  0.5f, /**/ 0.0f, 1.0f, 0.0f,
			 0.5f, -0.5f, /**/ 0.0f, 0.0f, 1.0f,
	};
	
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
		glfwSetWindowSizeCallback(window, (GLFWWindowSizeCallbackI) (win, width, height) -> {
			// Update the GL viewport size
			glViewport(0, 0, width, height);
		});
		
		// Setup GL Context
		GL.createCapabilities();
		
		// Create the shader
		int vertexShader, fragShader;
		
		// Compile the vertex shader
		vertexShader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertexShader,
				"\n" +
						"#version 110\n" +
						"attribute vec4 position;\n" +
						"attribute vec3 vertex_color;\n" +
						"varying vec3 color;\n" +
						"void main (void) {\n" +
						"   gl_Position = vec4(position.xy, 0, 1);\n" +
						"   color = vertex_color;\n" +
						"}");
		glCompileShader(vertexShader);
		if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE)
			throw new IllegalStateException("Failed to compile vertex shader");
		
		// Compile the fragment shader
		fragShader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragShader,
				"\n" +
						"#version 110\n" +
						"varying vec3 color;\n" +
						"void main (void) {\n" +
						"   gl_FragColor = vec4(color.rgb, 1);\n" +
						"}");
		glCompileShader(fragShader);
		if (glGetShaderi(fragShader, GL_COMPILE_STATUS) == GL_FALSE)
		{
			System.out.println(glGetShaderInfoLog(fragShader));
			throw new IllegalStateException("Failed to compile fragment shader");
		}
		
		// Link it all together
		shader = glCreateProgram();
		glAttachShader(shader, vertexShader);
		glAttachShader(shader, fragShader);
		glLinkProgram(shader);
		
		if (glGetProgrami(shader, GL_LINK_STATUS) == GL_FALSE)
			throw new IllegalStateException("Failed to link the default shader");
		
		glDetachShader(shader, vertexShader);
		glDetachShader(shader, fragShader);
		// Delete shaders once everything is gone
		//glDeleteShader(vertexShader);
		//glDeleteShader(fragShader);
		
		// Create the vertex buffer
		model = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, model);
		glBufferData(GL_ARRAY_BUFFER, vertData, GL_STATIC_DRAW);
		
		// Attribute locations
		int pos = glGetAttribLocation(shader, "position");
		int clr = glGetAttribLocation(shader, "vertex_color");
		
		glEnableVertexAttribArray(pos);
		glEnableVertexAttribArray(clr);
		
		glVertexAttribPointer(pos, 2, GL_FLOAT, false, 5 * 4, 0);
		glVertexAttribPointer(clr, 3, GL_FLOAT, false, 5 * 4, 2 * 4);
		
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
		
		// Draw two triangles (immediate mode & VBO + Shaders)
		glBegin(GL_TRIANGLES);
		glColor3f(1.f, 0.f, 0.f);
		glVertex2d(1, 0.5);
		glColor3f(0.f, 1.f, 0.f);
		glVertex2d(0, 0.5);
		glColor3f(0.f, 0.f, 1.f);
		glVertex2d(0.5, -0.5);
		glEnd();
		
		glUseProgram(shader);
		glDrawArrays(GL_TRIANGLES, 0, 3);
		glUseProgram(0);
	}
	
	private void parseArgs(String[] args) {}
	
	public static void main(String... args)
	{
		// Launch the game into a new thread
		final Game game = new Game();
		
		game.parseArgs(args);
		new Thread((Runnable) game::run, "Client").start();
	}
}
