package ddb.io.voxelnet.client.render;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class BufferLayout
{
	// Layout of the buffer
	private final List<BufferAttrib> layout = new ArrayList<>();
	// Stride of the buffer layout
	// Is equal to total bytes
	private int stride;
	// Next attribute index to use
	private int nextIndex = 0;
	
	/**
	 * Creates a new BufferLayout
	 */
	public BufferLayout() {}
	
	/**
	 * Adds an attribute to the vertex layout
	 * @param type The type of the attribute
	 * @param count The number of components for the attribute. Must be between
	 *              1 and 4, inclusive
	 * @param normalized If the data values will be normalized to [0,1]
	 */
	public void addAttribute(EnumAttribType type, int count, boolean normalized)
	{
		// Validate the attribute parameters
		if (count < 1 || count > 4)
		{
			System.out.println("Error: Bad number of buffer attribute components");
			return;
		}
		
		BufferAttrib attrib = new BufferAttrib(nextIndex++, count, type, normalized, stride);
		layout.add(attrib);
		
		// Calculate the new stride
		stride += type.bytes * count;
	}
	
	int getStride()
	{
		return stride;
	}
	
	List<BufferAttrib> getLayout()
	{
		return layout;
	}
	
	/**
	 * All valid types for a BufferAttrib
	 */
	public enum EnumAttribType
	{
		/** Byte */
		BYTE   (1),
		/** Short */
		SHORT  (2),
		/** Int */
		INT    (4),
		/** Unsigned Byte */
		UBYTE  (1),
		/** Unsigned Short */
		USHORT (2),
		/** Unsigned Int */
		UINT   (4),
		/** Float */
		FLOAT  (4),
		/** Double */
		DOUBLE (8);
		
		/** Size of the type in bytes */
		final int bytes;
		
		EnumAttribType(int bytes)
		{
			this.bytes = bytes;
		}
		
		public int toGLType()
		{
			switch (this)
			{
				case BYTE:   return GL_BYTE;
				case SHORT:  return GL_SHORT;
				case INT:    return GL_INT;
				case UBYTE:  return GL_UNSIGNED_BYTE;
				case USHORT: return GL_UNSIGNED_SHORT;
				case UINT:   return GL_UNSIGNED_INT;
				case FLOAT:  return GL_FLOAT;
				case DOUBLE: return GL_DOUBLE;
			}
			
			// Unknown type, default for glVertexAttribPointer is GL_FLOAT
			return GL_FLOAT;
		}
	}
	
	/**
	 * Class / Struct representing a buffer attribute
	 */
	static class BufferAttrib
	{
		/** Attribute Index */
		final int index;
		/** Count of the specified type */
		final int count;
		/** Type of the buffer attribute */
		final EnumAttribType type;
		/** If the attribute is normalized or not */
		final boolean normalized;
		/** Offset inside the buffer to the first attribute */
		final int offset;
		
		BufferAttrib(int index, int count, EnumAttribType type, boolean normalized, int offset)
		{
			this.index = index;
			this.count = count;
			this.type = type;
			this.normalized = normalized;
			this.offset = offset;
		}
	}
}
