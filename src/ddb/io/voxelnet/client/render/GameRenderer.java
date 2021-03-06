package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.entity.EntityRenderer;
import ddb.io.voxelnet.client.render.util.Camera;
import ddb.io.voxelnet.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer for the entire game
 */
public class GameRenderer
{
	// Temporary matrix transfer arrays
	private final float[] floatMatrix = new float[16];
	
	private Camera camera;
	private Shader currentShader;
	private final Map<Class<? extends Entity>, EntityRenderer> entityRenderers;
	public final TextureAtlas tileAtlas;
	
	public GameRenderer(TextureAtlas tileAtlas)
	{
		this.tileAtlas = tileAtlas;
		entityRenderers = new LinkedHashMap<>();
	}
	
	/**
	 * Registers an entity render
	 * @param entity The entity that should use this renderer
	 * @param renderer The renderer to render the entity
	 */
	public void registerEntityRenderer(Class<? extends Entity> entity, EntityRenderer renderer)
	{
		if (entityRenderers.containsKey(entity))
			throw new IllegalStateException("Attempt to register the same entity twice");
		entityRenderers.put(entity, renderer);
	}
	
	/**
	 * Gets the renderer for the given entity
	 * @param entity The entity that should be rendered
	 * @return The entity renderer for the specified entity
	 */
	public EntityRenderer getEntityRenderer(Class<? extends Entity> entity)
	{
		if (!entityRenderers.containsKey(entity))
			throw new IllegalStateException("Entity Renderer not registered for entity " + entity.getName());
		return entityRenderers.get(entity);
	}
	
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
	 * Sends the projection and view matrix to the shader
	 */
	public void prepareShader()
	{
		currentShader.bind();
	
		// Upload projection matrix
		camera.getPerspectiveTransform().get(floatMatrix);
		currentShader.setUniformMatrix4fv("ProjectMatrix", false, floatMatrix);
		
		// Upload view matrix
		camera.getViewTransform().get(floatMatrix);
		currentShader.setUniformMatrix4fv("ViewMatrix", false, floatMatrix);
	}
	
	public Shader getCurrentShader()
	{
		return currentShader;
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
		glClearColor(134f/255f, 221f/255f, 243f/255f, 1f);
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
	 * The model does not need to be bound before calling this method,
	 * as the bind/unbind will be performed inside here
	 *
	 * @param model The model to draw
	 */
	public void drawModel(Model model)
	{
		// Upload the model matrix
		model.getTransform().get(floatMatrix);
		currentShader.setUniformMatrix4fv("ModelMatrix", false, floatMatrix);
		
		model.bind();
		glDrawElements(model.getDrawMode().toGLEnum(), model.getIndexCount(), GL_UNSIGNED_INT, 0L);
		model.unbind();
	}
	
}
