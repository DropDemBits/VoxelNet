package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

/**
 * Builds a mesh using the specified buffer layout and draw mode
 */
public class ModelBuilder
{
	private static final int INDEX_SIZE = 4;
	
	private final BufferLayout layout;
	private final EnumDrawMode mode;
	
	ByteBuffer vertexBuffer;
	ByteBuffer indexBuffer;
	
	// Current polygon
	// Size of the current polygon
	private int polySize = 0;
	// Total vertices in the current polygon
	private int polyCount = 0;
	// Starting index of the polygon
	private int polyStart = 0;
	// Index offset for the current polygon
	private int indexOffset = 0;
	
	private int vertexCount = 0;
	int indexCount = 0;
	
	// Only grows in endVertex
	private int maxVertexCount = 0;
	private int maxIndexCount = 0;
	
	// Next size to resize to
	private int nextVertexResize = 0;
	private int nextIndexResize = 0;
	
	/**
	 * Creates a new model builder
	 * @param layout The layout for the model builder to use
	 * @param mode The drawing mode to use during construction
	 */
	public ModelBuilder(BufferLayout layout, EnumDrawMode mode)
	{
		this(layout, mode, 4);
	}
	
	/**
	 * Creates a new model builder
	 * @param layout The layout for the model builder to use
	 * @param mode The drawing mode to use during construction
	 * @param initialVertexCount The number of vertices to preallocate
	 */
	public ModelBuilder(BufferLayout layout, EnumDrawMode mode, int initialVertexCount)
	{
		this.layout = layout;
		this.mode = mode;
		
		// Create the builder buffers
		vertexBuffer = BufferUtils.createByteBuffer(initialVertexCount * layout.getStride());
		indexBuffer = BufferUtils.createByteBuffer((initialVertexCount < 3 ? initialVertexCount : 3 * (initialVertexCount - 2)) * INDEX_SIZE);
	}
	
	/**
	 * Resets the model builder to draw the new model
	 */
	public void reset()
	{
		indexOffset = 0;
		
		vertexCount = 0;
		indexCount = 0;
		
		polyStart = 0;
		polySize = 0;
		polyCount = 0;
		
		// Overwrite old data
		vertexBuffer.clear();
		indexBuffer.clear();
	}
	
	/**
	 * Compacts the builder data to use less space
	 */
	public void compact()
	{
		maxVertexCount = vertexCount;
		maxIndexCount = indexCount;
	}
	
	/**
	 * Sets the offset for the index of the provoking / starting vertex
	 * Only applies to polygons constructed after this call
	 * @param offset The new offset for all indices
	 */
	public void setIndexOffset(int offset)
	{
		indexOffset = offset;
	}
	
	/**
	 * Adds a polygon
	 * Automatically calculates the real number of vertices to allocate
	 * @param vertices The number of vertices for the polygon
	 */
	public void addPoly(int vertices)
	{
		// Don't add empty polygons
		if (vertices == 0)
			return;
		
		// Setup everything for building
		polyCount = vertices;
		polySize = 0;
		polyStart = vertexCount;
		
		// Compute the number of indices to allocate
		int indices;
		
		switch (mode)
		{
			case LINES:
				indices = 2 * vertices;
				break;
			case TRIANGLES:
			default:
				indices = vertices < 3 ? vertices : 3 * (vertices - 2);
				break;
		}
		
		vertexCount += vertices;
		indexCount += indices;
		
		if (maxVertexCount < vertexCount) maxVertexCount = vertexCount;
		if (maxIndexCount < indexCount) maxIndexCount = indexCount;
		
		if (needsResize())
		{
			// Reallocate if there's no space
			resizeTo(maxVertexCount * layout.getStride(), maxIndexCount * INDEX_SIZE);
		}
	}
	
	/**
	 * Ends the current vertex
	 * Automatically handles the chaining of vertices
	 */
	public void endVertex()
	{
		// Index offset is used to change the provoking vertex position
		// Original order
		// 0, 1, 2, 3
		// Offset + 1
		// 1, 2, 3, 0
		int vertexID = polySize;
		polySize++;
		
		// Verify the size
		if (polySize > polyCount)
		{
			System.err.println("Too many polygon vertices!");
			throw new RuntimeException("Polygon vertex count exceeded (" + polySize + " > " + polyCount);
		}
		
		if (mode == EnumDrawMode.TRIANGLES)
		{
			if (polySize <= 3)
			{
				// Less than 3 vertices, just add the index
				indexBuffer.putInt( ((vertexID + 0) + indexOffset) % polyCount + polyStart);
			} else
			{
				// Link the previous, current, and first vertex into a triangle
				indexBuffer.putInt( ((vertexID - 1) + indexOffset) % polyCount + polyStart);
				indexBuffer.putInt( ((vertexID + 0) + indexOffset) % polyCount + polyStart);
				indexBuffer.putInt( (             0 + indexOffset) % polyCount + polyStart);
			}
		}
		else if (mode == EnumDrawMode.LINES)
		{
			indexBuffer.putInt( ((vertexID + 0) + indexOffset) % polyCount + polyStart);
			
			// Add the double indices
			if (polySize < polyCount)
				indexBuffer.putInt( ((vertexID + 1) + indexOffset) % polyCount + polyStart);
		}
		
		// Handle end behavior
		if (polySize == polyCount)
		{
			if(mode == EnumDrawMode.LINES)
			{
				// Close the final line
				indexBuffer.putInt( (             0 + indexOffset) % polyCount + polyStart);
			}
		}
	}
	
	/**
	 * Checks if the builder has some data
	 * @return True if there is still some vertex data
	 */
	public boolean hasData()
	{
		return indexCount > 0;
	}
	
	/**
	 * Checks if the internal builder buffers need a resize
	 * @return True if the buffer needs to be resized
	 */
	public boolean needsResize()
	{
		return maxVertexCount * layout.getStride() > vertexBuffer.capacity()
				||  maxIndexCount * INDEX_SIZE > indexBuffer.capacity();
	}
	
	////////////// Common Puts \\\\\\\\\\\\\\
	public ModelBuilder pos2f(float x, float y)
	{
		return vec2f(x, y);
	}
	
	public ModelBuilder pos3f(float x, float y, float z)
	{
		//return vec3s((short)(x * 255.f), (short)(y * 255.f), (short)(z * 255.f));
		return vec3f(x, y, z);
	}
	
	public ModelBuilder tex2i(int u, int v)
	{
		return vec2s((short)u, (short)v);
	}
	
	public ModelBuilder light3b(int skyLight, int blockLight, int aoLight)
	{
		return vec3b((byte)skyLight, (byte)blockLight, (byte)aoLight);
	}
	
	public ModelBuilder colour4(float r, float g, float b, float a)
	{
		return vec4f(r, g, b, a);
	}
	
	////////////// Raw Puts \\\\\\\\\\\\\\
	// Don't resize the buffer here, allow hard failure to determine missing endVertex()'s
	public ModelBuilder vec2f(float v0, float v1)
	{
		vertexBuffer.putFloat(v0);
		vertexBuffer.putFloat(v1);
		return this;
	}
	
	public ModelBuilder vec3f(float v0, float v1, float v2)
	{
		vertexBuffer.putFloat(v0);
		vertexBuffer.putFloat(v1);
		vertexBuffer.putFloat(v2);
		return this;
	}
	
	public ModelBuilder vec4f(float v0, float v1, float v2, float v3)
	{
		vertexBuffer.putFloat(v0);
		vertexBuffer.putFloat(v1);
		vertexBuffer.putFloat(v2);
		vertexBuffer.putFloat(v3);
		return this;
	}
	
	public ModelBuilder vec2s(short v0, short v1)
	{
		vertexBuffer.putShort(v0);
		vertexBuffer.putShort(v1);
		return this;
	}
	
	public ModelBuilder vec3s(short v0, short v1, short v2)
	{
		vertexBuffer.putShort(v0);
		vertexBuffer.putShort(v1);
		vertexBuffer.putShort(v2);
		return this;
	}
	
	public ModelBuilder vec3b(byte v0, byte v1, byte v2)
	{
		vertexBuffer.put(v0);
		vertexBuffer.put(v1);
		vertexBuffer.put(v2);
		return this;
	}
	
	////////////// Private utility \\\\\\\\\\\\\\
	private void resizeTo(int newVertexSize, int newIndexSize)
	{
		if (nextVertexResize < newVertexSize) nextVertexResize = newVertexSize;
		if (nextIndexResize < newIndexSize) nextIndexResize = newIndexSize;
		
		ByteBuffer newVertex = BufferUtils.createByteBuffer(nextVertexResize);
		ByteBuffer newIndex = BufferUtils.createByteBuffer(nextIndexResize);
		
		// Double the next size to reduce the number of resizes
		nextVertexResize <<= 1;
		nextIndexResize <<= 1;
		
		vertexBuffer.flip();
		indexBuffer.flip();
		
		newVertex.put(vertexBuffer);
		newIndex.put(indexBuffer);
		
		// Kill off the old buffers
		vertexBuffer = null;
		indexBuffer = null;
		
		vertexBuffer = newVertex;
		indexBuffer = newIndex;
	}
	
}
