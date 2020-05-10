package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.GameRenderer;
import ddb.io.voxelnet.entity.Entity;

/**
 * Entity renderer for a given entity
 */
public abstract class EntityRenderer
{
	
	/**
	 * Renders the entity
	 * @param e The entity instance to render
	 * @param renderer The game render used for rendering
	 */
	public abstract void render(Entity e, GameRenderer renderer, double partialTicks);
	
}
