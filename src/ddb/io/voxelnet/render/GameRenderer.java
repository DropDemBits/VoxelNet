package ddb.io.voxelnet.render;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class GameRenderer
{
	private final float[] matrix = new float[16];
	private Camera camera;
	private Shader currentShader;
	
	/**
	 * Uses the shader for the next set of models
	 * @param shader The shader to use
	 */
	public void useShader(Shader shader)
	{
		this.currentShader = shader;
	}
	
	/**
	 * Uses the specified camera for rendering
	 * @param camera The camera for rendering
	 */
	public void useCamera(Camera camera)
	{
		this.camera = camera;
	}
	
	/**
	 * Gets the current camera
 	 * @return The camera
	 */
	public Camera getCamera()
	{
		return camera;
	}
	
	/**
	 * Prepares the shader for drawing models
	 * Sends the PV matrix to the shader
	 */
	public void prepareShader()
	{
		camera.getTransform().get(matrix);
		
		currentShader.bind();
		currentShader.setUniformMatrix4fv("PVMatrix", false, matrix);
	}
	
	/**
	 * Finishes drawing models
	 */
	public void finishShader()
	{
		currentShader.unbind();
	}
	
	/**
	 * Begins rendering
	 */
	public void begin()
	{
		glClearColor(0f, 0f, 0f, 1f);
		glEnable(GL_CULL_FACE);
		glCullFace(GL_BACK);
		
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}
	
	/**
	 * Draws a single model
	 * @param model The model to draw
	 * @param modelMatrix The model transform to use
	 */
	public void drawModel(Model model, Matrix4f modelMatrix)
	{
		// Upload the model matrix
		modelMatrix.get(matrix);
		currentShader.setUniformMatrix4fv("ModelMatrix", false, matrix);
		
		model.bind();
		glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0L);
		model.unbind();
	}
	
}
