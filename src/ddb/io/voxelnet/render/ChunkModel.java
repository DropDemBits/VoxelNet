package ddb.io.voxelnet.render;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;

/**
 * Model of a chunk being rendered
 */
public class ChunkModel
{
	Model model;
	Chunk chunk;
	
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
	 * @param adjacentChunks The chunks adjacent to the current chunk model
	 *                          (in the order of NORTH, WEST, SOUTH, EAST, UP, DOWN)
	 * @param atlas The texture atlas to use for the faces
	 * @return True if the chunk has been updated
	 */
	public boolean updateModel(Chunk[] adjacentChunks, TextureAtlas atlas)
	{
		// Check if the chunk actually needs to be re-rendered
		if(!chunk.isDirty())
			return false;
		
		// Clear the model if there is data
		if(model.getIndexCount() > 0)
			model.reset();
		
		// Check if the chunk has been made empty
		if(chunk.isEmpty())
			return true;
		
		int[] faceTexIDs = new int[] {1, 1, 1, 1, 0, 2};
		
		long now = System.currentTimeMillis();
		// Chunk is not empty, update the things
		for (int i = 0; i < 16 * 16 * 16; i++)
		{
			int x = i % 16;
			int y = i / 256;
			int z = (i / 16) % 16;
			
			if (chunk.getData()[x + z * 16 + y * 256] == 0)
				continue;
			
			for(Facing face : Facing.values())
			{
				int[] offset = face.getOffset();
				byte adjacentBlock = chunk.getBlock(x + offset[0], y + offset[1], z + offset[2]);
				
				// Don't add the face if the adjacent block is solid
				if(adjacentBlock > 0)
					continue;
				
				float[] texCoords = atlas.getPositions(faceTexIDs[face.ordinal()]);
				BlockRenderer.addCubeFace(
						model,
						(float) chunk.chunkX * 16 + x,
						(float) y,
						(float) chunk.chunkZ * 16 + z,
						face,
						texCoords);
			}
		}
		System.out.println("(" + chunk.chunkX + ", " + chunk.chunkZ + ")");
		System.out.println("\tGenerate time: " + (System.currentTimeMillis() - now));
		
		model.bind();
		model.updateVertices();
		model.unbind();
		
		System.out.println("\tUpdate time: " + (System.currentTimeMillis() - now));
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
}
