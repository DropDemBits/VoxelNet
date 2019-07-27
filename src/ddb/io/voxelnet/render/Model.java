package ddb.io.voxelnet.render;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class Model
{
	// Size of a vertex, in floats
	private static final int VERTEX_SIZE = 5;
	
	/// Polygon State ///
	// Current starting index for the polygon
	private int polyStart = 0;
	// Number of vertices for the current polygon
	private int polyCount = 0;
	// Whether a polygon is being constructed or not
	private boolean constructingPolygon = false;
	
	// Vertex Data
	// Vertex position (by 3)
	private List<Float> vertexData;
	
	// Indices
	private List<Integer> indices;
	
	// GL *BO Handles
	private int vboHandle;
	private int iboHandle;
	
	public Model()
	{
		vertexData = new ArrayList<>();
		indices = new ArrayList<>();
		
		// Create the buffers
		vboHandle = glGenBuffers();
		iboHandle = glGenBuffers();
		
		GLContext.INSTANCE.addBuffer(vboHandle);
		GLContext.INSTANCE.addBuffer(iboHandle);
	}
	
	private float[] getVertexData()
	{
		float[] array = new float[vertexData.size()];
		
		for (int i = 0; i < array.length; i++)
			array[i] = vertexData.get(i);
		
		return array;
	}
	
	private int[] getIndexData()
	{
		return indices.stream().mapToInt(f -> f == null ? -1 : f).toArray();
	}
	
	/**
	 * Updates the data of the GL Buffers
	 */
	public void updateVertices()
	{
		glBufferData(GL_ARRAY_BUFFER, getVertexData(), GL_STATIC_DRAW);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, getIndexData(), GL_STATIC_DRAW);
	}
	
	/**
	 * Binds the model's data to the current render context
	 */
	public void bind()
	{
		glBindBuffer(GL_ARRAY_BUFFER, vboHandle);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboHandle);
		
		// position
		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0);
		
		// texCoord
		glEnableVertexAttribArray(1);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3 * 4);
	}
	
	/**
	 * Unbinds the model from the current render context
	 * Called automatically after every call to free()
	 */
	public void unbind()
	{
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}
	
	/**
	 * Get the number of indices
	 * Used in glDrawElements
 	 */
	public int getIndexCount()
	{
		return indices.size();
	}
	
	public void reset()
	{
		polyCount = 0;
		polyStart = 0;
		constructingPolygon = false;
		vertexData.clear();
		indices.clear();
	}
	
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
		polyStart = vertexData.size() / VERTEX_SIZE;
	}
	
	public void endPoly()
	{
		if (!constructingPolygon)
			// Do nothing
			return;
		
		// Terminate the current polygon
		constructingPolygon = false;
	}
	
	/**
	 * Adds a 2d point to the current polygon
	 * @param x The x coordinate of the point
	 * @param y The y coordinate of the point
	 */
	public void addVertex(float x, float y)
	{
		addVertex(x, y, 0, 0, 0);
	}
	
	/**
	 * Adds a 3d point to the current polygon
	 * @param x The x coordinate of the point
	 * @param y The y coordinate of the point
	 * @param z The z coordinate of the point
	 */
	public void addVertex(float x, float y, float z)
	{
		// Top left by default
		addVertex(x, y, z, 0, 0);
	}
	
	public void addVertex(float x, float y, float z, float u, float v)
	{
		int index = polyCount + polyStart;
		
		// Add position first...
		vertexData.add(x);
		vertexData.add(y);
		vertexData.add(z);
		
		// Then the texture position
		vertexData.add(u);
		vertexData.add(v);
		
		// Update the indices
		polyCount++;
		if (polyCount <= 3) {
			// Less than 3 vertices, just add the index
			indices.add(index);
		}
		else {
			// Link the previous, current, and first vertex into a triangle
			indices.add(index - 1);
			indices.add(index);
			indices.add(polyStart);
		}
	}
	
}
