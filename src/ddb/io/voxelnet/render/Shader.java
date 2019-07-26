package ddb.io.voxelnet.render;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.*;

public class Shader
{
	// Whether the shader is valid or not
	private final boolean isValid;
	// Holds the program handle
	private int programHandle;
	
	public Shader (String path)
	{
		System.out.println("Loading shader from " + path);
		boolean validShader;
		
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
			
			System.out.println(vertexSource.toString());
			System.out.println(fragmentSource.toString());
			System.out.println(vertexLayout.toString());
			
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
	
	public void free()
	{
		glDeleteProgram(programHandle);
	}
	
	public void fixupModel(Model model)
	{
		model.bind();
		
		int clr = glGetAttribLocation(programHandle, "color");
		int pos = glGetAttribLocation(programHandle, "position");
		
		glEnableVertexAttribArray(pos);
		glVertexAttribPointer(pos, 2, GL_FLOAT, false, 6 * 4, 0);
		
		glEnableVertexAttribArray(clr);
		glVertexAttribPointer(clr, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
		
		model.unbind();
	}
	
}
