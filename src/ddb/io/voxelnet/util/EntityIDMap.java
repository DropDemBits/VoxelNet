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
	// Both maps are modified as an atomic operation
	private final Map<Integer, Entity> idToEntity = new ConcurrentHashMap<>();
	private final Map<Entity, Integer> entityToId = new ConcurrentHashMap<>();
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
	 * Adds an entity with an existing id to the map
	 * @param entity The entity to add a mapping for
	 * @param entityID The id for mapping the entity
	 */
	public synchronized void addExistingEntity(Entity entity, int entityID)
	{
		// Atomically add to both maps
		idToEntity.put(entityID, entity);
		entityToId.put(entity, entityID);
	}
	
	/**
	 * Removes an entity from the mapping
	 * @param entityID The entity id to remove the mapping for
	 */
	public synchronized void removeEntity(int entityID)
	{
		// Atomically remove the entity from the mappings
		Entity oldEntity = idToEntity.remove(entityID);
		entityToId.remove(oldEntity);
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
	 * Gets an entity's associated id
	 * @param entity The entity to get the associated id
	 * @return The id of the entity, or -1 if none was found
	 */
	public int getIdFor(Entity entity)
	{
		return entityToId.getOrDefault(entity, -1);
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
