package ddb.io.voxelnet.client.render;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * Renders text strings to the screen
 */
public class FontRenderer
{
	private Texture fontTexture;
	private TextureAtlas fontAtlas;
	
	// Internal buffer for quads
	private ByteBuffer quadBuffer;
	
	// TEMPORARY: Use model/sprite batcher for handling the buffers
	// GL *BO Handles
	private int vboHandle;
	private int iboHandle;
	
	final float FontScale = 2f;
	final float Riser = 12;
	final float Spacer = 6;
	
	/**
	 * Creates a new text builder
	 * @param fontPath The path to the font texture
	 */
	public FontRenderer(String fontPath)
	{
		fontTexture = new Texture(fontPath);
		fontAtlas = new TextureAtlas(fontTexture, 16, 16);
		// Enqueue up to 512 characters
		quadBuffer = ByteBuffer.allocateDirect(BufferLayout.QUAD_LAYOUT.getStride() * 6 * 512);
		quadBuffer.order(ByteOrder.nativeOrder());
		quadBuffer.clear();
		
		vboHandle = glGenBuffers();
		iboHandle = glGenBuffers();
		
		GLContext.INSTANCE.addBuffer(vboHandle);
		GLContext.INSTANCE.addBuffer(iboHandle);
	}
	
	/**
	 * Flushes the remaining characters to the screen
	 */
	public void flush()
	{
		quadBuffer.flip();
		if (quadBuffer.hasRemaining())
		{
			fontTexture.bind(1);
			glBindBuffer(GL_ARRAY_BUFFER, vboHandle);
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
			BufferLayout layout = BufferLayout.QUAD_LAYOUT;
			for (BufferLayout.BufferAttrib attrib : layout.getLayout())
			{
				glEnableVertexAttribArray(attrib.index);
				glVertexAttribPointer(attrib.index, attrib.count, attrib.type.toGLType(), attrib.normalized, layout.getStride(), attrib.offset);
			}
			
			glBufferData(GL_ARRAY_BUFFER, quadBuffer, GL_STATIC_DRAW);
			glDrawArrays(GL_TRIANGLES, 0, (quadBuffer.remaining() / BufferLayout.QUAD_LAYOUT.getStride()));
		}
		quadBuffer.clear();
	}
	
	/**
	 * Puts a single character onto the screen
	 * @param chr The character to draw
	 * @param x The x position of the char
	 * @param y The y position of the char
	 */
	public void putChar(char chr, float x, float y)
	{
		// Replace bad characters with the empty char
		if (chr >= 256)
			chr = 255;
		
		short[] texCoords = fontAtlas.getPixelPositions(chr);
		// Add the quad
		// 0-3
		// |\|
		// 1-2
		quadBuffer.putFloat(x+ 0f*FontScale).putFloat(y+ 0f*FontScale).putShort(texCoords[0]).putShort(texCoords[3]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 0
		quadBuffer.putFloat(x+ 0f*FontScale).putFloat(y+16f*FontScale).putShort(texCoords[0]).putShort(texCoords[1]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 1
		quadBuffer.putFloat(x+16f*FontScale).putFloat(y+16f*FontScale).putShort(texCoords[2]).putShort(texCoords[1]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 2
		
		quadBuffer.putFloat(x+16f*FontScale).putFloat(y+16f*FontScale).putShort(texCoords[2]).putShort(texCoords[1]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 2
		quadBuffer.putFloat(x+16f*FontScale).putFloat(y+ 0f*FontScale).putShort(texCoords[2]).putShort(texCoords[3]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 3
		quadBuffer.putFloat(x+ 0f*FontScale).putFloat(y+ 0f*FontScale).putShort(texCoords[0]).putShort(texCoords[3]).putFloat(1).putFloat(1).putFloat(1).putFloat(1); // 0
		
		if (!quadBuffer.hasRemaining())
			flush();
	}
	
	/**
	 * Puts a string onto the screen
	 * @param text The text to draw
	 * @param x The x position of the text
	 * @param y The y position of the text
	 */
	public void putString(String text, float x, float y)
	{
		float posX = x;
		float posY = y;
		
		for (char c : text.toCharArray())
		{
			putChar(c, posX, posY);
			// Since the font is monospace, add constval to x
			posX += Spacer * FontScale;
			
			if (c == '\n')
			{
				posX = x;
				posY += Riser * FontScale;
			}
		}
	}
	
}
