package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.client.render.gl.GLContext;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL15.*;

public class Model
{
	// Draw mode of the model. TRIANGLES by default
	private EnumDrawMode drawMode = EnumDrawMode.TRIANGLES;
	
	// Index count
	private int indicesCount = 0;
	
	// GL *BO Handles
	private final int vboHandle;
	private final int iboHandle;
	
	// Binding state of the buffer
	private boolean isBound = false;
	
	// Layout of the vertex buffer
	private final BufferLayout layout;
	
	// Transformation matrix of the model
	private Matrix4f modelMatrix;
	
	/**
	 * Creates a new model with the specified vertex layout
	 * @param layout The layout of the model's vertex buffer
	 */
	public Model(BufferLayout layout)
	{
		this.layout = layout;
		
		// Create the buffers
		vboHandle = glGenBuffers();
		iboHandle = glGenBuffers();
		
		GLContext.INSTANCE.addBuffer(vboHandle);
		GLContext.INSTANCE.addBuffer(iboHandle);
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
	 * Updates the data of the GL Buffers with the given model builder
	 */
	public void updateVertices(ModelBuilder builder)
	{
		indicesCount = builder.indexCount;
		
		if (indicesCount > 0)
		{
			builder.vertexBuffer.flip();
			builder.indexBuffer.flip();
			
			glBufferData(GL_ARRAY_BUFFER, builder.vertexBuffer, GL_STATIC_DRAW);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, builder.indexBuffer, GL_STATIC_DRAW);
		}
		else
		{
			glBufferData(GL_ARRAY_BUFFER, 0, GL_STATIC_DRAW);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, 0, GL_STATIC_DRAW);
		}
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
		layout.bindLayout();
	}
	
	/**
	 * Unbinds the model from the current render context
	 * Called automatically after every call to free()
	 */
	public void unbind()
	{
		// TODO: Pass in RenderContext to lazy-unload the current layout
		if(!isBound)
			return;
		isBound = false;
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
		
		// Unload the buffer layout
		layout.unbindLayout();
	}
	
	/**
	 * Get the number of indices
	 * Used in glDrawElements
 	 */
	public int getIndexCount()
	{
		return indicesCount;
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
	
}
