package ddb.io.voxelnet.client.render;

import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public enum EnumDrawMode
{
	TRIANGLES,
	LINES;
	
	public int toGLEnum()
	{
		switch (this)
		{
			case TRIANGLES: return GL_TRIANGLES;
			case LINES: return GL_LINES;
		}
		
		return GL_TRIANGLES;
	}
}
