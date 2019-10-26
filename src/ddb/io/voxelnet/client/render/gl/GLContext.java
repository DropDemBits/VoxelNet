package ddb.io.voxelnet.client.render.gl;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;
import java.util.List;

public class GLContext
{
	public static final GLContext INSTANCE = new GLContext();
	
	private final List<Integer> buffers;
	private final List<Integer> shaders;
	private final List<Integer> textures;
	
	private GLContext()
	{
		buffers = new ArrayList<>();
		shaders = new ArrayList<>();
		textures = new ArrayList<>();
	}
	
	public void addShader(int programId)
	{
		shaders.add(programId);
	}
	
	public void addBuffer(int bufferID)
	{
		buffers.add(bufferID);
	}
	
	public void addTexture(int texID)
	{
		textures.add(texID);
	}
	
	public void free()
	{
		shaders.forEach(GL20::glDeleteProgram);
		buffers.forEach(GL15::glDeleteBuffers);
		textures.forEach(GL11::glDeleteTextures);
		
		shaders.clear();
		buffers.clear();
		textures.clear();
	}
}
