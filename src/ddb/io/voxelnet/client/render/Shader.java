package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.GLContext;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.*;

public class Shader
{
	// Whether the shader is valid or not
	private final boolean isValid;
	// Holds the program handle
	private int programHandle;
	// Whether the shader is bound or not
	private boolean isBound = false;
	private Map<String, Integer> locationCache;
	
	public Shader (String path)
	{
		System.out.println("Loading shader from " + path);
		boolean validShader;
		
		// TODO: Separate out resource loading
		try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
		     BufferedReader in = new BufferedReader(new InputStreamReader(stream)))
		{
			final int READING_VERTEX = 1;
			final int READING_FRAGMENT = 2;
			final int READING_LAYOUT = 3;
			
			// File Reading
			int readingState = -1;
			String line;
			
			// Shader sources
			StringBuilder vertexSource = new StringBuilder();
			StringBuilder fragmentSource = new StringBuilder();
			List<String> vertexLayout = new ArrayList<>();
			
			while((line = in.readLine()) != null)
			{
				// Wait for either #vertex, #fragment, or #layout
				switch (line)
				{
					case "#vertex":
						readingState = READING_VERTEX;
						continue;
					case "#fragment":
						readingState = READING_FRAGMENT;
						continue;
					case "#vertexlayout":
						readingState = READING_LAYOUT;
						continue;
				}
				
				switch (readingState)
				{
					case READING_VERTEX:
						vertexSource.append(line).append('\n');
						break;
					case READING_FRAGMENT:
						fragmentSource.append(line).append('\n');
						break;
					case READING_LAYOUT:
						// Remove the trailing comment
						if (line.contains("//"))
							line = line.substring(line.indexOf("//") + 2);
						
						// Remove leading whitespace
						line = line.trim();
						
						vertexLayout.add(line);
						break;
				}
			}
			
			// Compile each shader
			int vertexShader, fragmentShader;
			vertexShader = compileAndVerifyShader(GL_VERTEX_SHADER, vertexSource.toString());
			
			if (vertexShader == -1)
			{
				glDeleteShader(vertexShader);
				throw new RuntimeException("Unable to compile vertex shader");
			}
			
			fragmentShader = compileAndVerifyShader(GL_FRAGMENT_SHADER, fragmentSource.toString());
			if (fragmentShader == -1)
			{
				glDeleteShader(vertexShader);
				glDeleteShader(fragmentShader);
				throw new RuntimeException("Unable to compile fragment shader");
			}
			
			// Create the program
			programHandle = glCreateProgram();
			GLContext.INSTANCE.addShader(programHandle);
			
			// Apply the vertex layout
			if (vertexLayout.size() > 0)
			{
				vertexLayout.forEach((layout) -> {
					String[] components = layout.split(" ");
					glBindAttribLocation(programHandle, Integer.parseInt(components[0]), components[1]);
				});
			}
			
			// Link it all together
			glAttachShader(programHandle, vertexShader);
			glAttachShader(programHandle, fragmentShader);
			glLinkProgram(programHandle);
			glDeleteShader(vertexShader);
			glDeleteShader(fragmentShader);
			
			if (glGetProgrami(programHandle, GL_LINK_STATUS) == GL_FALSE)
			{
				System.out.println(glGetProgramInfoLog(programHandle));
				
				// Delete unused program handle
				glDeleteShader(programHandle);
				throw new IllegalStateException("Failed to link the shader");
			}
			
			locationCache = new LinkedHashMap<>();
			validShader = true;
		}
		catch (Exception e) {
			validShader = false;
			e.printStackTrace();
		}
		
		isValid = validShader;
	}
	
	private int compileAndVerifyShader(int shaderType, String source)
	{
		int shader = glCreateShader(shaderType);
		glShaderSource(shader, source);
		glCompileShader(shader);
		
		if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE)
		{
			System.out.println(glGetShaderInfoLog(shader));
			// -1 indicates a compile failure
			return -1;
		}
		
		return shader;
	}
	
	public void bind()
	{
		if(isValid)
			glUseProgram(programHandle);
	}
	
	public void unbind()
	{
		if(isValid)
			glUseProgram(0);
	}
	
	/*** Uniform Related ***/
	private int getUniform(String name)
	{
		// No uniforms if the shader isn't valid
		if (!isValid)
			return -1;
		
		int location;
		if ((location = locationCache.getOrDefault(name, -2)) != -2)
			return location;
		
		// Add a new entry to the cache
		location = glGetUniformLocation(programHandle, name);
		locationCache.put(name, location);
		return location;
	}
	
	/**
	 * Sets the uniform's value for the current shader
	 * The shader must already be bound
	 * @param name The name of the uniform to set
	 * @param value The new value of the uniform
	 */
	public void setUniform1i(String name, int value)
	{
		if (!isValid)
			return;
		
		int location = getUniform(name);
		glUniform1i(location, value);
	}
	
	/**
	 * Sets the uniform's value for the current shader
	 * The shader must already be bound
	 * @param name The name of the uniform to set
	 * @param value The new value of the uniform
	 */
	public void setUniform1f(String name, float value)
	{
		if (!isValid)
			return;
		
		int location = getUniform(name);
		glUniform1f(location, value);
	}
	
	/**
	 * Sets the uniform's value for the current shader
	 * The shader must already be bound
	 * @param name The name of the uniform to set
	 * @param value The new value of the uniform
	 */
	public void setUniformMatrix4fv(String name, boolean transpose, float[] value)
	{
		if (!isValid)
			return;
		
		int location = getUniform(name);
		glUniformMatrix4fv(location, transpose, value);
	}
	
}
