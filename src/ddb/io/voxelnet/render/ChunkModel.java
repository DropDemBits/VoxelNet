package ddb.io.voxelnet.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.Chunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		//System.out.println("(" + chunk.chunkX + ", " + chunk.chunkY + ", " + chunk.chunkZ + ")");
		
		// List of transparent blocks to add on to the end
		// Lazily allocate it
		List<Vec3i> transparentBlocks = null;
		
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
					
					if (block.isTransparent())
					{
						// If the block is transparent, add coord on to the
						// transparent layer list
						
						if (transparentBlocks == null)
							transparentBlocks = new ArrayList<>();
						
						transparentBlocks.add(new Vec3i(x, y, z));
						
						// Move on to the next block
						continue;
					}
					
					int[] faceTextures = block.getFaceTextures();
					BlockRenderer.addCube(model, chunk, block, x, y, z, faceTextures, atlas);
				}
			}
		}
		
		if (transparentBlocks != null)
		{
			// Add all the transparent blocks, if they exist
			for (Vec3i pos : transparentBlocks)
			{
				// Block will not be air, so don't check for it
				byte id = chunk.getData()[pos.getX() + pos.getZ() * 16 + pos.getY() * 256];
				Block block = Block.idToBlock(id);
				
				int[] faceTextures = block.getFaceTextures();
				BlockRenderer.addCube(model, chunk, block, pos.getX(), pos.getY(), pos.getZ(), faceTextures, atlas);
			}
		}
		
		//System.out.println("\tGenerate time: " + (System.nanoTime() - now) / 1000000000.0d);
		
		// Defer the vertex buffer update to the render stage
		isDirty = true;
		return true;
	}
	
	/**
	 * Gets the model associated with this chunk model
	 * @return The model associated with this chunk model
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
