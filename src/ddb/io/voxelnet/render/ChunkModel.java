package ddb.io.voxelnet.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;

import java.util.Arrays;

/**
 * Model of a chunk being rendered
 */
public class ChunkModel
{
	Model model;
	Chunk chunk;
	private boolean isDirty = false;
	private boolean updatePending = false;
	
	/**
	 * Creates a chunk model
	 * @param chunk The chunk to be based off from
	 */
	public ChunkModel(Chunk chunk)
	{
		this.chunk = chunk;
		this.model = new Model();
	}
	
	/**
	 * Updates the chunk model
	 * @param atlas The texture atlas to use for the faces
	 * @return True if the chunk has been updated
	 */
	public boolean updateModel(TextureAtlas atlas)
	{
		// Check if the chunk actually needs to be re-rendered
		if (!chunk.isDirty())
			return false;
		
		// Clear the model if there is data
		if (model.getIndexCount() > 0)
			model.reset();
		
		// Indicate that the chunk has been updated
		chunk.makeClean();
		
		// Check if the chunk has been made empty
		if (chunk.isEmpty())
			return true;
		
		long now = System.nanoTime();
		System.out.println("(" + chunk.chunkX + ", " + chunk.chunkY + ", " + chunk.chunkZ + ")");
		
		// Chunk is not empty, update the things
		for (int x = 0; x < 16; x++)
		{
			for (int z = 0; z < 16; z++)
			{
				for (int y = 0; y < 16; y++)
				{
					byte id = chunk.getData()[x + z * 16 + y * 256];
					if (id == 0)
						continue;
					
					Block block = Block.idToBlock(id);
					int[] faceTextures = block.getFaceTextures();
					
					for (Facing face : Facing.values())
					{
						int[] offset = face.getOffset();
						byte adjacentBlock = chunk.getBlock(x + offset[0], y + offset[1], z + offset[2]);
						
						if (adjacentBlock == -1)
						{
							// Check the nearby chunk for the appropriate block id
							adjacentBlock = chunk.world.getBlock(
									chunk.chunkX * 16 + x + offset[0],
									chunk.chunkY * 16 + y + offset[1],
									chunk.chunkZ * 16 + z + offset[2]);
						}
						
						Block adjacent = Block.idToBlock(adjacentBlock);
						if (adjacent.isSolid())
						{
							// Don't add the face if the adjacent block is solid
							continue;
						}
						
						float[] texCoords = atlas.getPositions(faceTextures[face.ordinal()]);
						BlockRenderer.addCubeFace(
								model,
								(float) (chunk.chunkX * 16 + x),
								(float) (chunk.chunkY * 16 + y),
								(float) (chunk.chunkZ * 16 + z),
								face,
								texCoords);
					}
				}
			}
		}
		
		System.out.println("\tGenerate time: " + (System.nanoTime() - now) / 1000000000.0d);
		
		// Defer the vertex buffer update to the render stage
		isDirty = true;
		return true;
	}
	
	/**
	 * Gets the model associated with this chunk
	 * @return The model associated with this chunk
	 */
	public Model getModel()
	{
		return model;
	}
	
	public boolean isDirty()
	{
		return isDirty;
	}
	
	public void makeClean()
	{
		isDirty = false;
	}
	
	/**
	 * Sees if a model update is pending
	 * @return True if a model update is pending
	 */
	public boolean isUpdatePending()
	{
		return updatePending;
	}
	
	public void setUpdatePending(boolean updatePending)
	{
		this.updatePending = updatePending;
	}
}
