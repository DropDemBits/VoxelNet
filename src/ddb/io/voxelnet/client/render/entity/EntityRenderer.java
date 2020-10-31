package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.GameRenderer;
import ddb.io.voxelnet.entity.Entity;

/**
 * Entity renderer for a given entity
 *
 * Each entity type shares a common entity renderer, instead of having a separate entity renderer instance
 * for each active entity in the game
 */
public interface EntityRenderer
{
	
	/**
	 * Renders the entity
	 * @param e The entity instance to render
	 * @param renderer The game render used for rendering
	 */
	void render(Entity e, GameRenderer renderer, double partialTicks);
	
}
