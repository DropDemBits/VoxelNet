package ddb.io.voxelnet.client.render;

public class TextureAtlas
{
	private final Texture texture;
	private final int rows, columns;
	private final int width, height;
	private final float spriteWidth, spriteHeight;
	private static final float[] EMPTY_COORDS = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
	private static final int[] EMPTY_PIX_COORDS = new int[] {0, 0, 0, 0};
	
	public TextureAtlas(Texture texture, int rows, int columns)
	{
		this.texture = texture;
		this.rows = rows;
		this.columns = columns;
		this.width = texture.getWidth();
		this.height = texture.getHeight();
		
		// Calculate the sprite dimensions
		this.spriteWidth = (float) width / (float)this.columns;
		this.spriteHeight = (float) height / (float)this.rows;
	}
	
	public void bind(int slot)
	{
		texture.bind(slot);
	}
	
	public void unbind()
	{
		texture.unbind();
	}
	
	public float[] getPositions(int index)
	{
		if (index == -1)
			return EMPTY_COORDS;
		
		return getPositions(index % columns, index / columns);
	}
	
	public float[] getPositions(int x, int y)
	{
		if (x < 0 || y < 0 || x >= columns || y >= rows)
			return EMPTY_COORDS;
		
		final float spritesX = 1f / spriteWidth;
		final float spritesY = 1f / spriteHeight;
		
		// Add & sub by 0.0001f to keep the texture coordinate inside of the atlas slice
		return new float[] {
				    spritesX * (float) x + 0.0001f,
				1 - spritesY * (float) (y + 1) + 0.0001f,
				    spritesX * (float) (x + 1) - 0.0001f,
				1 - spritesY * (float) y - 0.0001f,
		};
	}
	
	public int[] getPixelPositions(int index)
	{
		if (index == -1)
			return EMPTY_PIX_COORDS;
		
		return getPixelPositions(index % columns, index / columns);
	}
	
	public int[] getPixelPositions(int x, int y)
	{
		if (x < 0 || y < 0 || x >= columns || y >= rows)
			return EMPTY_PIX_COORDS;
		
		float[] base = getPositions(x, y);
		
		return new int[] {
				(int)(base[0] * 0xFFFF),
				(int)(base[1] * 0xFFFF),
				(int)(base[2] * 0xFFFF),
				(int)(base[3] * 0xFFFF),
		};
	}
	
	public float getPixelScaleX()
	{
		return ((1f / spriteWidth) * 0xFFFF);
	}
	
	public float getPixelScaleY()
	{
		return ((1f / spriteHeight) * 0xFFFF);
	}
	
}
