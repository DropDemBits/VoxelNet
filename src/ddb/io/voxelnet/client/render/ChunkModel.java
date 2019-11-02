package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;
import org.joml.Matrix4f;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Model of a chunk being rendered
 */
class ChunkModel
{
	// Worst case scenario model size: 688128 B / 672 KiB
	private static long generateAccum = 0;
	private static long generateCount = 0;
	
	private final Matrix4f modelMatrix;
	final Chunk chunk;
	private final Model[] modelLayers = new Model[RenderLayer.values().length];
	private final ModelBuilder[] builderLayers = new ModelBuilder[RenderLayer.values().length];
	
	private volatile boolean isDirty = false;
	private volatile boolean updatePending = false;
	private volatile boolean updateInProgress = false;
	private volatile boolean hasTransparency = false;
	private int updateAttempts = 0;
	
	private final ReentrantLock updateLock;
	// Layers that need updates
	private final boolean[] layerNeedsUpdate = new boolean[RenderLayer.values().length];
	private final Chunk[] adjacentChunks = new Chunk[Facing.values().length];
	
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
			
			builderLayers[layer.ordinal()] = new ModelBuilder(BlockRenderer.BLOCK_LAYOUT, EnumDrawMode.TRIANGLES);
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
		
		// Check if the chunk has been made empty
		if (chunk.isEmpty())
		{
			// Defer the vertex buffer update to the render stage
			isDirty = true;
			
			// Reset all the layers and indicate that the chunk has been rebuilt
			for (RenderLayer layer : RenderLayer.values())
			{
				chunk.resetLayerRebuildStatus(layer);
				layerNeedsUpdate[layer.ordinal()] = true;
				builderLayers[layer.ordinal()].reset();
				builderLayers[layer.ordinal()].compact();
			}
			
			--updateAttempts;
			updateLock.unlock();
			return true;
		}
		
		// Take a snapshot of the layer rebuild statuses
		boolean[] rebuildSnapshot = new boolean[RenderLayer.values().length];
		
		for (RenderLayer layer : RenderLayer.values())
		{
			rebuildSnapshot[layer.ordinal()] = chunk.layerNeedsRebuild(layer);
		}
		
		//System.out.println("(" + chunk.chunkX + ", " + chunk.chunkY + ", " + chunk.chunkZ + ")");
		
		// Fetch the adjacent chunks only once
		for (Facing face : Facing.values())
		{
			adjacentChunks[face.ordinal()] = chunk.world.getChunk(
					chunk.chunkX + face.getOffsetX(),
					chunk.chunkY + face.getOffsetY(),
					chunk.chunkZ + face.getOffsetZ()
			);
		}
		
		// Rebuild each layer individually
		long start = System.nanoTime();
		hasTransparency = false;
		
		for (RenderLayer layer : RenderLayer.values())
		{
			// Since the opaque layer is the most frequently updated layer,
			// allow changes to it to rebuild the other layers
			if (rebuildSnapshot[layer.ordinal()] || rebuildSnapshot[RenderLayer.OPAQUE.ordinal()])
				rebuildLayer(layer, atlas);
		}
		
		// Defer the vertex buffer update to the render stage
		isDirty = true;
		
		long currentGenerate = System.nanoTime() - start;
		generateAccum += currentGenerate;
		generateCount += 1;
		/*
		if ((generateCount % 8) == 0)
		{
			System.out.print("\tAvg Generate Time: " + (((double) generateAccum / (double) generateCount) / 1000000.0d) + "ms");
			System.out.println(", Current Generate Time: " + (currentGenerate) / 1000000.0d);
			System.out.println(BlockRenderer.statNear + ", " + BlockRenderer.statSolid + ", " + BlockRenderer.statNoShow);
			System.out.println("---------------------------------");
		}
		//*/
		
		--updateAttempts;
		updateLock.unlock();
		return true;
	}
	
	private void rebuildLayer(RenderLayer layer, TextureAtlas atlas)
	{
		ModelBuilder targetBuilder = builderLayers[layer.ordinal()];
		
		// Reset the builder
		targetBuilder.reset();
		
		// Current layer will need update
		layerNeedsUpdate[layer.ordinal()] = true;
		
		// Chunk is not empty, update the things
		// TODO: Rework so that the adjacent blocks & metas are fetched once per block
		for (int y = 0; y < 16; y++)
		{
			boolean skipLayer = false;
			boolean layerAboveFilled = false;
			boolean layerBelowFilled = false;
			
			// Check if a layer can be skipped
			if (y < 15 && chunk.getLayerData()[y + 1] == 16 * 16)
				layerAboveFilled = true;
			else if (y == 15 && adjacentChunks[Facing.UP.ordinal()].getLayerData()[0] == 16 * 16)
				layerAboveFilled = true;
			
			if (y > 0 && chunk.getLayerData()[y - 1] == 16 * 16)
				layerBelowFilled = true;
			else if (y == 0 && adjacentChunks[Facing.DOWN.ordinal()].getLayerData()[15] == 16 * 16)
				layerBelowFilled = true;
			
			if (layerAboveFilled && layerBelowFilled)
			{
				skipLayer = true;
				
				// Check if the current layer can actually be skipped
				for (Facing adjacentDir : Facing.CARDINAL_FACES)
				{
					if (adjacentChunks[adjacentDir.ordinal()].getLayerData()[y] < 16 * 16)
					{
						skipLayer = false;
						break;
					}
				}
			}
			
			if (skipLayer)
				continue;
			
			// Do all the blocks for the layer
			for (int z = 0; z < 16; z++)
			{
				for (int x = 0; x < 16; x++)
				{
					byte id = chunk.getData()[x + (z << 4) + (y << 8)];
					if (id == 0)
						continue;
					
					Block block = Block.idToBlock(id);
					// Skip blocks not part of the requested layer
					if (block.getRenderLayer() != layer)
						continue;
					
					if (block.isTransparent())
						hasTransparency = true;
					
					int[] faceTextures = block.getFaceTextures();
					
					BlockRenderer.addModel(targetBuilder, adjacentChunks, chunk, block, x, y, z, faceTextures, atlas);
				}
			}
		}
		
		// Indicate that the chunk has been updated
		chunk.resetLayerRebuildStatus(layer);
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
				updatingModel.updateVertices(builderLayers[layer.ordinal()]);
				builderLayers[layer.ordinal()].compact();
			}
			
			layerNeedsUpdate[layer.ordinal()] = false;
		}
		finally
		{
			updateLock.unlock();
		}
	}
	
}
