package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.ChunkField;
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
	Chunk chunk;
	private final Model[] modelLayers = new Model[RenderLayer.values().length];
	
	// Model builders for the layers
	private final ModelBuilder[] layerBuilders = new ModelBuilder[RenderLayer.values().length];
	
	// Layers that need updates
	private final boolean[] layerNeedsUpdate = new boolean[RenderLayer.values().length];
	// Layers that have data
	private final boolean[] layerHasData = new boolean[RenderLayer.values().length];
	// Update state for the model
	private volatile ModelState modelState = ModelState.CURRENT;
	private int updateAttempts = 0;
	
	// Synchronizes model updates
	private final ReentrantLock updateLock;
	// Synchronizes state updates
	private final ReentrantLock stateLock;
	
	/**
	 * Creates a chunk model
	 * @param chunk The chunk to be based off from
	 */
	public ChunkModel(Chunk chunk)
	{
		this.modelMatrix = new Matrix4f();
		this.modelMatrix.translate(chunk.chunkX * 16, chunk.chunkY * 16, chunk.chunkZ * 16);
		
		for (RenderLayer layer : RenderLayer.values())
		{
			modelLayers[layer.ordinal()] = new Model(BlockRenderer.BLOCK_LAYOUT);
			modelLayers[layer.ordinal()].setTransform(this.modelMatrix);
			
			layerBuilders[layer.ordinal()] = new ModelBuilder(BlockRenderer.BLOCK_LAYOUT, EnumDrawMode.TRIANGLES);
		}
		
		this.updateLock = new ReentrantLock();
		this.stateLock = new ReentrantLock();
		
		// Associate chunk to this model
		associateWith(chunk);
	}
	
	/**
	 * Associates a chunk with the model
	 * @param chunk The chunk to associate with the model
	 */
	public void associateWith(Chunk chunk)
	{
		this.chunk = chunk;
		this.modelMatrix.translation(chunk.chunkX * 16, chunk.chunkY * 16, chunk.chunkZ * 16);
	}
	
	/**
	 * Updates the chunk model
	 * @param atlas The texture atlas to use for the faces
	 */
	public void updateModel(TextureAtlas atlas)
	{
		setModelState(ModelState.UPDATING);
		updateLock.lock();
		++updateAttempts;
		
		chunk.chunkField.rebuildField();
		chunk.chunkField.rebuildNeighborFields();
		
		// Check if the chunk has been made empty
		if (chunk.hasNoBlocks())
		{
			// Reset all the layers and indicate that the chunk has been rebuilt
			for (RenderLayer layer : RenderLayer.values())
			{
				chunk.resetLayerRebuildStatus(layer);
				
				layerBuilders[layer.ordinal()].reset();
				layerBuilders[layer.ordinal()].compact();
				
				layerNeedsUpdate[layer.ordinal()] = true;
			}
		}
		else
		{
			// Take a snapshot of the layer rebuild statuses
			boolean[] rebuildSnapshot = new boolean[RenderLayer.values().length];
			
			for (RenderLayer layer : RenderLayer.values())
			{
				rebuildSnapshot[layer.ordinal()] = chunk.layerNeedsRebuild(layer);
			}
			
			// Rebuild each layer individually
			long start = System.nanoTime();
			
			for (RenderLayer layer : RenderLayer.values())
			{
				// Since the opaque layer is the most frequently updated layer,
				// allow changes to it to rebuild the other layers
				if (rebuildSnapshot[layer.ordinal()] || rebuildSnapshot[RenderLayer.OPAQUE.ordinal()])
					rebuildLayer(layer, atlas);
			}
			
			long currentGenerate = System.nanoTime() - start;
			generateAccum += currentGenerate;
			generateCount += 1;
			
			if (Game.showDetailedDebug && (generateCount % 8) == 0)
			{
				System.out.print("\tAvg Generate Time: " + (((double) generateAccum / (double) generateCount) / 1000000.0d) + "ms");
				System.out.println(", Current Generate Time: " + (currentGenerate) / 1000000.0d);
				System.out.println(BlockRenderer.statNear + ", " + BlockRenderer.statSolid + ", " + BlockRenderer.statNoShow);
				System.out.println("---------------------------------");
				
				// reset counts (it's fiiiiiine that it's globally mutable data, these are rough counters)
				generateAccum = 0;
				generateCount = 0;
			}
		}
		
		--updateAttempts;
		updateLock.unlock();
		setModelState(ModelState.UPDATE_DONE);
		
		// Make sure that we are the only ones updating the chunk
		assert updateAttempts == 0;
	}
	
	private void rebuildLayer(RenderLayer layer, TextureAtlas atlas)
	{
		ModelBuilder targetBuilder = layerBuilders[layer.ordinal()];
		ChunkField field = chunk.chunkField;
		
		// Reset the builder
		targetBuilder.reset();
		
		// Chunk is not empty, update the things
		for (int y = 0; y < 16; y++)
		{
			boolean skipLayer = false;
			boolean layerAboveFilled = false;
			boolean layerBelowFilled = false;
			
			// Check if a layer can be skipped
			int layerY = y;
			if ((field.getChunk(0, y + 1, 0, Facing.NONE).map(chunk -> chunk.getLayerCount(layerY + 1)).orElse((short)0)) == 16 * 16)
				layerAboveFilled = true; // Layer above is filled
			
			if ((field.getChunk(0, y - 1, 0, Facing.NONE).map(chunk -> chunk.getLayerCount(layerY - 1)).orElse((short) 0)) == 16 * 16)
				layerBelowFilled = true; // Layer below is filled
			
			if (layerAboveFilled && layerBelowFilled && chunk.getLayerCount(y) == 16 * 16)
			{
				// Layer is a candidate for skipping (layer above, below, and self is full)
				skipLayer = true;
				
				// Check if the current layer can actually be skipped by
				// checking if the cardinally adjacent layers are filled
				for (Facing adjacentDir : Facing.CARDINAL_FACES)
				{
					// If the cardinal adjacent layer is not full, don't skip the layer
					if (field.getAdjacentChunk(adjacentDir).map(chunk->chunk.getLayerCount(layerY)).orElse((short)0) < 16 * 16)
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
					Block block = Block.idToBlock(chunk.getBlock(x, y, z));
					
					// Skip blocks that are air or not part of the requested layer
					if (block == Blocks.AIR || block == Blocks.VOID || block.getRenderLayer() != layer)
						continue;
					
					int[] faceTextures = block.getFaceTextures();
					
					BlockRenderer.addModel(targetBuilder, field, block, x, y, z, faceTextures, atlas);
				}
			}
		}
		
		// Indicate that the chunk has been updated
		chunk.resetLayerRebuildStatus(layer);
		
		// Current layer will need update
		layerNeedsUpdate[layer.ordinal()] = true;
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
	
	/**
	 * Sees if the given layer has data to render
	 * @return True if there are things to render in the layer
	 */
	public boolean dataInLayer(RenderLayer layer)
	{
		return this.layerHasData[layer.ordinal()];
	}
	
	public Matrix4f getTransform()
	{
		return modelMatrix;
	}
	
	/**
	 * Marks the model as pending a rebuild
	 * @return True if the model was marked for update, false if an update was already pending
	 */
	public boolean markForUpdate()
	{
		if (getModelState() == ModelState.CURRENT)
		{
			setModelState(ModelState.PENDING);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Sets the update state for the model
	 * @param newState The new update state
	 */
	private void setModelState(ModelState newState)
	{
		stateLock.lock();
		this.modelState = newState;
		stateLock.unlock();
	}
	
	/**
	 * Sets the update state for the model
	 * @return The current update state
	 */
	private ModelState getModelState()
	{
		return this.modelState;
	}
	
	/**
	 * Updates the layer's vertices
	 * @param layer The layer to update
	 */
	private void updateLayer(RenderLayer layer)
	{
		Model updatingModel = modelLayers[layer.ordinal()];
		
		// Update the model & clear the vertices
		if (layerNeedsUpdate[layer.ordinal()])
		{
			ModelBuilder builder = layerBuilders[layer.ordinal()];
			layerHasData[layer.ordinal()] = builder.hasData();
			
			updatingModel.updateVertices(builder);
			builder.compact();
		}
			
		layerNeedsUpdate[layer.ordinal()] = false;
	}
	
	/**
	 * Updates the layer models
	 */
	public void updateModels()
	{
		if (modelState != ModelState.UPDATE_DONE)
		{
			// If not called in the update done state, bail out
			return;
		}
		
		for (RenderLayer layer : RenderLayer.values())
		{
			Model model = getModelForLayer(layer);
			model.bind();
			updateLayer(layer);
			model.unbind();
		}
			
		setModelState(ModelState.CURRENT);
	}
	
	// Forces a rebuild of the adjacent neighbors
	public void forceNeighborRebuild()
	{
		for (Facing sides : Facing.directions())
			chunk.chunkField.getAdjacentChunk(sides).ifPresent(Chunk::forceLayerRebuild);
	}
	
	/**
	 * Update state for a model
	 */
	private enum ModelState
	{
		// Model update is queued, will be processed later
		PENDING,
		// Model is in the process for being updated
		// Don't observe the ChunkModel in this state, values may be indeterminant / transient
		UPDATING,
		// Model update is finished, vertex buffer updates are delayed to the render stage
		UPDATE_DONE,
		// Vertex buffer update is complete, most recent model is being displayed
		CURRENT,
	}
}
