package ddb.io.voxelnet.client.render.gl;

import static org.lwjgl.opengl.GL11.*;

public enum EnumDrawMode
{
	TRIANGLES,
	TRIANGLE_FAN,
	TRIANGLE_STRIP,
	LINES,
	LINE_LOOP,
	LINE_STRIP,
	;
	
	public int toGLEnum()
	{
		switch (this)
		{
			case TRIANGLES:         return GL_TRIANGLES;
			case TRIANGLE_FAN:      return GL_TRIANGLE_FAN;
			case TRIANGLE_STRIP:    return GL_TRIANGLE_STRIP;
			case LINES:             return GL_LINES;
			case LINE_LOOP:         return GL_LINE_LOOP;
			case LINE_STRIP:        return GL_LINE_STRIP;
		}
		
		return GL_TRIANGLES;
	}
}
