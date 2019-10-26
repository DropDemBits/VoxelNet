package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.GLContext;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture
{
	private int texHandle;
	private int width, height;
	
	public Texture(String path)
	{
		try (MemoryStack stack = MemoryStack.stackPush())
		{
			IntBuffer x = stack.callocInt(1);
			IntBuffer y = stack.callocInt(1);
			IntBuffer bpp = stack.callocInt(1);
			
			// Fetch the real path
			String realPath = Objects.requireNonNull(Texture.class.getClassLoader().getResource(path)).getPath();
			
			// Load the image
			stbi_set_flip_vertically_on_load(true);
			ByteBuffer localBuffer = stbi_load(realPath, x, y, bpp, 4);
			if (localBuffer == null)
			{
				System.out.println(stbi_failure_reason());
				throw new RuntimeException("Unable to load texture");
			}
			
			// Store info
			width = x.get();
			height = y.get();
			
			texHandle = glGenTextures();
			GLContext.INSTANCE.addTexture(texHandle);
			
			glBindTexture(GL_TEXTURE_2D, texHandle);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, localBuffer);
			glBindTexture(GL_TEXTURE_2D, 0);
			
			// Done with the buffer, do things with it
			stbi_image_free(localBuffer);
		}
	}
	
	public void bind(int textureSlot)
	{
		glActiveTexture(GL_TEXTURE0 + textureSlot);
		glBindTexture(GL_TEXTURE_2D, texHandle);
	}
	
	public void unbind()
	{
		glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
}
