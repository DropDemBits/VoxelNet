package ddb.io.voxelnet.render;

public class TextureAtlas
{
	private final Texture texture;
	private final int rows, columns;
	private final int width, height;
	private final float spriteWidth, spriteHeight;
	private static final float[] EMPTY_COORDS = new float[] {0.0f, 0.0f, 0.0f, 0.0f};
	private static final short[] EMPTY_SHORT_COORDS = new short[] {0, 0, 0, 0};
	
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
		
		return new float[] {
				spritesX * (float) x,
				1 - spritesY * (float) (y + 1),
				spritesX * (float) (x + 1),
				1 - spritesY * (float) y,
		};
	}
	
	public short[] getPixelPositions(int index)
	{
		if (index == -1)
			return EMPTY_SHORT_COORDS;
		
		return getPixelPositions(index % columns, index / columns);
	}
	
	public short[] getPixelPositions(int x, int y)
	{
		if (x < 0 || y < 0 || x >= columns || y >= rows)
			return EMPTY_SHORT_COORDS;
		
		float[] base = getPositions(x, y);
		
		return new short[] {
				(short)(base[0] * 0xFFFF),
				(short)(base[1] * 0xFFFF),
				(short)(base[2] * 0xFFFF),
				(short)(base[3] * 0xFFFF),
		};
	}
	
}
