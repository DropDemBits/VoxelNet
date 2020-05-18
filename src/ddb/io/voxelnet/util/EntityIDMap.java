package ddb.io.voxelnet.util;

import ddb.io.voxelnet.entity.Entity;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds mappings between entities and ids
 * This is used to map between network ids and entities
 */
public class EntityIDMap
{
	// Atomic operation to modify both maps
	private Map<Integer, Entity> idToEntity = new ConcurrentHashMap<>();
	private Map<Entity, Integer> entityToId = new ConcurrentHashMap<>();
	private int nextEntityID = 0;
	
	/**
	 * Adds an entity to the map
	 * @param entity The entity to add a mapping for
	 * @return The new id for the entity
	 */
	public int addEntity(Entity entity)
	{
		int eId = nextEntityID++;
		addExistingEntity(entity, eId);
		return eId;
	}
	
	/**
	 * Adds an entity with an existind id to the map
	 * @param entity The entity to add a mapping for
	 * @param entityID The id for mapping the entity
	 */
	public void addExistingEntity(Entity entity, int entityID)
	{
		idToEntity.put(entityID, entity);
		entityToId.put(entity, entityID);
	}
	
	public void removeEntity(int entityID)
	{
		// Remove the entity from the mappings
		Entity oldEntity = idToEntity.remove(entityID);
		entityToId.remove(oldEntity);
		
		// Remove the entity
		oldEntity.setDead();
	}
	
	/**
	 * Gets the entity using its associated id
	 * @param entityID The entity id to use for lookup
	 * @param expectedKind The expected kind of entity for this id
	 * @return The entity associated with this id, or null if not found
	 */
	public Entity getEntity(int entityID, Class<? extends Entity> expectedKind)
	{
		Entity entity = idToEntity.getOrDefault(entityID, null);
		if (entity == null || entity.getClass() != expectedKind)
			return null;
		
		return entity;
	}
	
	/**
	 * Gets all of the ids for the active entities
	 * @return The set containing all of the active ids
	 */
	public Set<Integer> getAllEntityIds()
	{
		return idToEntity.keySet();
	}
	
}
