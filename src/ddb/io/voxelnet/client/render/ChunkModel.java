package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.world.Chunk;
import org.joml.Matrix4f;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Model of a chunk being rendered
 */
public class ChunkModel
{
	// Worst case scenario model size: 688128 B / 672 KiB
	public static long generateAccum = 0;
	public static long generateCount = 0;
	
	Matrix4f modelMatrix;
	Chunk chunk;
	private Model[] modelLayers = new Model[RenderLayer.values().length];
	private volatile boolean isDirty = false;
	private volatile boolean updatePending = false;
	private volatile boolean updateInProgress = false;
	private volatile boolean hasTransparency = false;
	private int updateAttempts = 0;
	
	private ReentrantLock updateLock;
	// Layers that need updates
	private boolean[] layerNeedsUpdate = new boolean[RenderLayer.values().length];
	
	/**
	 * Creates a chunk model
	 * @param chunk The chunk to be based off from
	 */
	public ChunkModel(Chunk chunk)
	{
		this.chunk = chunk;
		this.modelMatrix = new Matrix4f();
		this.modelMatrix.translate(chunk.chunkX * 16, chunk.chunkY * 16, chunk.chunkZ * 16);
		
		for (RenderLayer layer : RenderLayer.values())
		{
			modelLayers[layer.ordinal()] = new Model(BlockRenderer.BLOCK_LAYOUT);
			modelLayers[layer.ordinal()].setTransform(this.modelMatrix);
		}
		
		this.updateLock = new ReentrantLock();
	}
	
	/**
	 * Updates the chunk model
	 * @param atlas The texture atlas to use for the faces
	 * @return True if the chunk has been updated
	 */
	public boolean updateModel(TextureAtlas atlas)
	{
		updateLock.lock();
		++updateAttempts;
		
		// Check if the chunk actually needs to be re-rendered
		if (!chunk.needsRebuild())
		{
			System.out.println("UPD InP" + updateAttempts);
			--updateAttempts;
			updateLock.unlock();
			return false;
		}
		
		// Reset the model layers
		for (int i = 0; i < modelLayers.length; i++)
		{
			if (modelLayers[i].getIndexCount() > 0)
				modelLayers[i].reset();
			// All layers initially need updates
			layerNeedsUpdate[i] = true;
		}
		
		// Check if the chunk has been made empty
		if (chunk.isEmpty())
		{
			// Defer the vertex buffer update to the render stage
			isDirty = true;
			
			// Indicate that the chunk has been rebuilt
			chunk.resetRebuildStatus();
			
			--updateAttempts;
			updateLock.unlock();
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
					Model targetModel = modelLayers[block.getRenderLayer().ordinal()];
					
					if (block.isTransparent())
						hasTransparency = true;
					
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
		
		/*if ((generateCount % 8) == 0)
		{
			System.out.print("\tAvg Generate Time: " + (((double) generateAccum / (double) generateCount) / 1000000.0d) + "ms");
			System.out.println(", Current Generate Time: " + (currentGenerate) / 1000000.0d);
			System.out.println("\tAvg Block Gen Time: " + (((double) blockGenAccum / (double) blockGenCount) / 1000.0d) + "us");
			System.out.println("\tExtrapolate BlockGen Time: " + (((double) blockGenAccum / (double) blockGenCount) / 1000.0d) * 256.0d + "us");
			System.out.println("---------------------------------");
		}*/
		
		// Defer the vertex buffer update to the render stage
		isDirty = true;
		// Indicate that the chunk has been updated
		chunk.resetRebuildStatus();
		
		--updateAttempts;
		updateLock.unlock();
		return true;
	}
	
	/**
	 * Gets the model for the appropriate layer
	 * @param layer The layer to fetch the model for
	 * @return The layer's model
	 */
	public Model getModelForLayer(RenderLayer layer)
	{
		return modelLayers[layer.ordinal()];
	}
	
	public boolean isDirty()
	{
		return isDirty;
	}
	
	public Matrix4f getTransform()
	{
		return modelMatrix;
	}
	
	public synchronized void makeClean()
	{
		if(!isDirty)
			return;
		
		// Update the model data
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
	
	/**
	 * Updates the layer's vertices
	 * @param layer The layer to update
	 */
	public void updateLayer(RenderLayer layer)
	{
		updateLock.lock();
		try
		{
			Model updatingModel = modelLayers[layer.ordinal()];
			
			// Update the model & clear the vertices
			if (layerNeedsUpdate[layer.ordinal()])
			{
				updatingModel.updateVertices();
				updatingModel.freeData();
			}
			
			layerNeedsUpdate[layer.ordinal()] = false;
		}
		finally
		{
			updateLock.unlock();
		}
	}
	
}
