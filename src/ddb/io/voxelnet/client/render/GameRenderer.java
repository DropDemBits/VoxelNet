package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.client.render.entity.EntityRenderer;
import ddb.io.voxelnet.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer for the entire game
 */
public class GameRenderer
{
	private final float[] matrix = new float[16];
	private Camera camera;
	private Shader currentShader;
	private Map<Class<? extends Entity>, EntityRenderer> entityRenderers;
	public TextureAtlas tileAtlas;
	
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
	 * @return
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
	 * Sends the PV matrix to the shader
	 */
	public void prepareShader()
	{
		camera.getTransform().get(matrix);
		
		currentShader.bind();
		currentShader.setUniformMatrix4fv("PVMatrix", false, matrix);
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
	 * @param model The model to draw
	 */
	public void drawModel(Model model)
	{
		// Upload the model matrix
		model.getTransform().get(matrix);
		currentShader.setUniformMatrix4fv("ModelMatrix", false, matrix);
		
		model.bind();
		glDrawElements(model.getDrawMode().toGLEnum(), model.getIndexCount(), GL_UNSIGNED_INT, 0L);
		model.unbind();
	}
	
}
