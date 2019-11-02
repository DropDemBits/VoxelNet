package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders text strings to the screen
 */
public class FontRenderer
{
	private final Texture fontTexture;
	private final TextureAtlas fontAtlas;
	
	// TODO: Group together into a sprite batcher
	private final ModelBuilder quadBuilder;
	private final Model quads;
	
	private final float FontScale = 2f;
	private final float Riser = 13;
	private final float Spacer = 6;
	
	/**
	 * Creates a new text builder
	 * @param fontPath The path to the font texture
	 */
	public FontRenderer(String fontPath)
	{
		fontTexture = new Texture(fontPath);
		fontAtlas = new TextureAtlas(fontTexture, 16, 16);
		
		quads = new Model(BufferLayout.QUAD_LAYOUT);
		// Enqueue up to 512 characters
		quadBuilder = new ModelBuilder(BufferLayout.QUAD_LAYOUT, EnumDrawMode.TRIANGLES, 4 * 512);
	}
	
	/**
	 * Flushes the remaining characters to the screen
	 */
	public void flush()
	{
		if (quadBuilder.hasData())
		{
			fontTexture.bind(1);
			quads.bind();
			quads.updateVertices(quadBuilder);
			glDrawElements(GL_TRIANGLES, quads.getIndexCount(), GL_UNSIGNED_INT, 0);
			quads.unbind();
		}
		quadBuilder.reset();
	}
	
	/**
	 * Puts a single character onto the screen
	 * @param chr The character to draw
	 * @param x The x position of the char
	 * @param y The y position of the char
	 */
	public void putChar(char chr, float x, float y)
	{
		// Replace bad characters with the replacement char
		if (chr >= 256)
			chr = 255;
		
		// Skip control characters
		if (Character.isISOControl(chr))
			return;
		
		// Snap the positions
		float xMin = (float)Math.round((x+ 0f*FontScale));
		float xMax = (float)Math.round((x+16f*FontScale));
		float yMin = (float)Math.round((y+ 0f*FontScale));
		float yMax = (float)Math.round((y+16f*FontScale));
		
		int[] texCoords = fontAtlas.getPixelPositions(chr);
		// Add the quad
		// 0-3
		// |\|
		// 1-2
		quadBuilder.addPoly(4);
		quadBuilder.pos2(xMin, yMin).tex2(texCoords[0], texCoords[3]).colour4(1, 1, 1, 1).endVertex(); // 0
		quadBuilder.pos2(xMin, yMax).tex2(texCoords[0], texCoords[1]).colour4(1, 1, 1, 1).endVertex(); // 1
		quadBuilder.pos2(xMax, yMax).tex2(texCoords[2], texCoords[1]).colour4(1, 1, 1, 1).endVertex(); // 2
		quadBuilder.pos2(xMax, yMin).tex2(texCoords[2], texCoords[3]).colour4(1, 1, 1, 1).endVertex(); // 3
		
		if (quadBuilder.needsResize())
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
			if (c == '\n')
			{
				posX = x;
				posY += Riser * FontScale;
			}
			else
			{
				putChar(c, posX, posY);
				// Since the font is monospace, add constval to x
				posX += Spacer * FontScale;
			}
		}
	}
	
}
