package ddb.io.voxelnet.client.render;

import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

public class Model
{
	/// Tesselator ///
	/// Polygon State ///
	// Current starting index for the polygon
	private int polyStart = 0;
	// Number of vertices for the current polygon
	private int polyCount = 0;
	// Whether a polygon is being constructed or not
	private boolean constructingPolygon = false;
	// Whether to create lines or not
	public boolean drawLines = false;
	
	/// Shared ///
	// Draw mode of the model. TRIANGLES by default
	private EnumDrawMode drawMode = EnumDrawMode.TRIANGLES;
	
	/// Model Data ///
	// Vertex Data
	// Vertex position (in byte[])
	private List<byte[]> vertexData;
	
	// Indices
	private List<Integer> indices;
	private int indiciesCount = 0;
	
	// GL *BO Handles
	private int vboHandle;
	private int iboHandle;
	
	// Binding state of the buffer
	private boolean isBound = false;
	
	// Layout of the vertex buffer
	private final BufferLayout layout;
	// Temporary buffer holding a single vertex's data, in bytes
	private final ByteBuffer vertex;
	
	// Transformation matrix of the model
	private Matrix4f modelMatrix;
	
	/**
	 * Creates a new model with the specified vertex layout
	 * @param layout The layout of the model's vertex buffer
	 */
	public Model(BufferLayout layout)
	{
		this.layout = layout;
		vertex = ByteBuffer.allocate(layout.getStride());
		vertex.order(ByteOrder.nativeOrder());
		
		vertexData = new ArrayList<>();
		indices = new ArrayList<>();
		
		// Create the buffers
		vboHandle = glGenBuffers();
		iboHandle = glGenBuffers();
		
		GLContext.INSTANCE.addBuffer(vboHandle);
		GLContext.INSTANCE.addBuffer(iboHandle);
	}
	
	private int[] getIndexData()
	{
		return indices.stream().mapToInt(f -> f == null ? -1 : f).toArray();
	}
	
	/**
	 * Sets the draw mode for the model
	 * @param mode The new draw mode for the model
	 */
	public void setDrawMode(EnumDrawMode mode)
	{
		this.drawMode = mode;
	}
	
	/**
	 * Gets the draw mode of the model
	 * @return The draw mode of the model
	 */
	public EnumDrawMode getDrawMode()
	{
		return drawMode;
	}
	
	/**
	 * Updates the data of the GL Buffers
	 */
	public void updateVertices()
	{
		// Fetch the vertex data
		ByteBuffer buf = ByteBuffer.allocateDirect(vertexData.size() * layout.getStride());
		buf.order(ByteOrder.nativeOrder());
		
		vertexData.iterator().forEachRemaining(buf::put);
		buf.flip();
		
		glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, getIndexData(), GL_STATIC_DRAW);
		indiciesCount = indices.size();
	}
	
	/**
	 * Binds the model's data to the current render context
	 */
	public void bind()
	{
		if(isBound)
			return;
		isBound = true;
		
		glBindBuffer(GL_ARRAY_BUFFER, vboHandle);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboHandle);
		
		// Load the buffer layout
		// TODO: Don't do this if we have VAOs
		for (BufferLayout.BufferAttrib attrib : layout.getLayout())
		{
			glEnableVertexAttribArray(attrib.index);
			glVertexAttribPointer(attrib.index, attrib.count, attrib.type.toGLType(), attrib.normalized, layout.getStride(), attrib.offset);
		}
	}
	
	/**
	 * Unbinds the model from the current render context
	 * Called automatically after every call to free()
	 */
	public void unbind()
	{
		if(!isBound)
			return;
		isBound = false;
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		// Unload the buffer layout
		// TODO: Don't do this if we have VAOs
		for (BufferLayout.BufferAttrib attrib : layout.getLayout())
		{
			glDisableVertexAttribArray(attrib.index);
		}
	}
	
	/**
	 * Get the number of indices
	 * Used in glDrawElements
 	 */
	public int getIndexCount()
	{
		return indiciesCount;
	}
	
	/**
	 * Sets the transform matrix of the model
	 * @param matrix The matrix to use as the transform
	 */
	public void setTransform(Matrix4f matrix)
	{
		this.modelMatrix = matrix;
	}
	
	/**
	 * Gets the models' associated transform
	 * @return The model's model matrix
	 */
	public Matrix4f getTransform()
	{
		return modelMatrix;
	}
	
	/// Tessellator Methods ///
	/**
	 * Reset the construction state
	 * Used to regenerate models
	 */
	public void reset()
	{
		polyCount = 0;
		polyStart = 0;
		constructingPolygon = false;
		vertexData.clear();
		indices.clear();
	}
	
	/**
	 * Frees the construction data
	 * Done after the vertices have been updated
	 */
	public void freeData()
	{
		vertexData.clear();
		indices.clear();
	}
	
	/**
	 * Begins constructing a polygon
	 */
	public void beginPoly()
	{
		if (constructingPolygon)
		{
			// Alert, but append on to current polygon
			// TODO: Add a logger
			System.out.println("Warning: No endPoly for model!");
			return;
		}
		
		// Update state
		constructingPolygon = true;
		polyCount = 0;
		polyStart = vertexData.size();
	}
	
	/**
	 * Finishes the construction of a polygon
	 */
	public void endPoly()
	{
		if (!constructingPolygon)
			// Do nothing
			return;
		
		// Terminate the current polygon
		constructingPolygon = false;
		
		if(drawLines)
		{
			// Close the final line
			indices.add(polyStart);
		}
	}
	
	/**
	 * Adds a single vertex, in the specified buffer layout
	 * @param values The values of the vertex
	 */
	public void addVertex(Number... values)
	{
		// Build the vertex data
		int valuePointer = 0;
		for (BufferLayout.BufferAttrib attrib : layout.getLayout())
		{
			for (int count = 1; count <= attrib.count; count++, valuePointer++)
			{
				switch (attrib.type)
				{
					case BYTE:
					case UBYTE:
						vertex.put(values[valuePointer].byteValue());
						break;
					case SHORT:
					case USHORT:
						vertex.putShort(values[valuePointer].shortValue());
						break;
					case INT:
					case UINT:
						vertex.putInt(values[valuePointer].intValue());
						break;
					case FLOAT:
						vertex.putFloat(values[valuePointer].floatValue());
						break;
					case DOUBLE:
						vertex.putDouble(values[valuePointer].doubleValue());
						break;
				}
			}
		}
		
		// Add the vertex data
		byte[] data = new byte[layout.getStride()];
		vertex.flip();
		vertex.get(data);
		vertex.flip();
		vertexData.add(data);
		
		// Update the indices
		int index = polyCount + polyStart;
		polyCount++;
		
		if (!drawLines)
		{
			if (polyCount <= 3)
			{
				// Less than 3 vertices, just add the index
				indices.add(index);
			} else
			{
				// Link the previous, current, and first vertex into a triangle
				indices.add(index - 1);
				indices.add(index);
				indices.add(polyStart);
			}
		}
		else
		{
			indices.add(index);
			// Double the index for the other points
			if (polyCount > 1)
				indices.add(index);
		}
	}
	
}
