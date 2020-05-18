package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.GLContext;
import org.lwjgl.system.MemoryStack;
import sun.misc.IOUtils;

import java.io.IOException;
import java.io.InputStream;
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
		try (MemoryStack stack = MemoryStack.stackPush();
		     InputStream texStream = Texture.class.getClassLoader().getResourceAsStream(path))
		{
			IntBuffer x = stack.callocInt(1);
			IntBuffer y = stack.callocInt(1);
			IntBuffer file_channels = stack.callocInt(1);
			
			// Load the image data into a bytebuf
			ByteBuffer imgBuf = stack.bytes(IOUtils.readAllBytes(Objects.requireNonNull(texStream)));
			
			// Load the image from memory
			stbi_set_flip_vertically_on_load(true);
			ByteBuffer localBuffer = stbi_load_from_memory(imgBuf, x, y, file_channels, 4);
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
		} catch (IOException e)
		{
			e.printStackTrace();
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
