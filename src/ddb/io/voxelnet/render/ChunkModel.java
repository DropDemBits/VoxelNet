package ddb.io.voxelnet.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.world.Chunk;

/**
 * Model of a chunk being rendered
 */
public class ChunkModel
{
	// Worst case scenario model size: 2064384 B / 2016 KiB / ~ 2 MiB
	public static long generateAccum = 0;
	public static long generateCount = 0;
	
	Model model;
	Model transparentLayer;
	Chunk chunk;
	private boolean isDirty = false;
	private boolean updatePending = false;
	private boolean updateInProgress = false;
	private boolean hasTransparency = false;
	/**
	 * Creates a chunk model
	 * @param chunk The chunk to be based off from
	 */
	public ChunkModel(Chunk chunk)
	{
		this.chunk = chunk;
		this.model = new Model();
		this.transparentLayer = new Model();
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
		
		// Reset the models
		if (model.getIndexCount() > 0)
			model.reset();
		
		if (transparentLayer.getIndexCount() > 0)
			transparentLayer.reset();
		
		// Indicate that the chunk has been updated
		chunk.makeClean();
		
		// Check if the chunk has been made empty
		if (chunk.isEmpty())
		{
			// Defer the vertex buffer update to the render stage
			isDirty = true;
			return true;
		}
		
		//System.out.println("(" + chunk.chunkX + ", " + chunk.chunkY + ", " + chunk.chunkZ + ")");
		
		// Reset transparency status
		hasTransparency = false;
		
		// Chunk is not empty, update the things
		long blockGenAccum = 0;
		long blockGenCount = 0;
		
		long start = System.nanoTime();
		for (int y = 0; y < 16; y++)
		{
			for (int z = 0; z < 16; z++)
			{
				//long blockNow = System.nanoTime();
				for (int x = 0; x < 16; x++)
				{
					byte id = chunk.getData()[x + (z << 4) + (y << 8)];
					if (id == 0)
						continue;
					
					Block block = Block.idToBlock(id);
					Model targetModel = model;
					
					if (block.isTransparent())
					{
						// Change the vertex target
						targetModel = transparentLayer;
						hasTransparency = true;
					}
					
					int[] faceTextures = block.getFaceTextures();
					BlockRenderer.addCube(targetModel, chunk, block, x, y, z, faceTextures, atlas);
				}
				/*blockGenAccum += System.nanoTime() - blockNow;
				blockGenCount += 16;*/
			}
		}
		
		long currentGenerate = System.nanoTime() - start;
		generateAccum += currentGenerate;
		generateCount += 1;
		
		if ((generateCount % 8) == 0)
		{
			System.out.print("\tAvg Generate Time: " + (((double) generateAccum / (double) generateCount) / 1000000.0d) + "ms");
			System.out.println(", Current Generate Time: " + (currentGenerate) / 1000000.0d);
			System.out.println("\tAvg Block Gen Time: " + (((double) blockGenAccum / (double) blockGenCount) / 1000.0d) + "us");
			System.out.println("\tExtrapolate BlockGen Time: " + (((double) blockGenAccum / (double) blockGenCount) / 1000.0d) * 256.0d + "us");
			System.out.println("---------------------------------");
		}
		
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
	
	/**
	 * Gets the model holding all of the transparent blocks for this chunk
	 * @return The model holding the transparent blocks
	 */
	public Model getTransparentModel()
	{
		return transparentLayer;
	}
	
	public boolean isDirty()
	{
		return isDirty;
	}
	
	public synchronized void makeClean()
	{
		if(!isDirty)
			return;
		
		// Free the excess vertex data
		model.freeData();
		transparentLayer.freeData();
		
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
	
	/**
	 * Sets the pending update state
	 * @param updatePending The new pending update state
	 */
	public synchronized void setUpdatePending(boolean updatePending)
	{
		this.updatePending = updatePending;
	}
	
	/**
	 * Sees if a model update is in progress
	 * @return True if a model update is in progress
	 */
	public boolean isUpdateInProgress()
	{
		return updateInProgress;
	}
	
	/**
	 * Sets the update progress state
	 * @param updateProgressing The new update progress state
	 */
	public synchronized void setUpdateProgress(boolean updateProgressing)
	{
		this.updateInProgress = updateProgressing;
	}
	
	/**
	 * Sees if the chunk model has transparent blocks
	 * @return True if there are transparent blocks to render
	 */
	public boolean hasTransparency()
	{
		return hasTransparency;
	}
}
