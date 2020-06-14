package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;

import java.nio.BufferOverflowException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer
{
	// Layers that have transparent blocks on them
	private static final RenderLayer[] transparentLayers = new RenderLayer[] { RenderLayer.TRANSPARENT, RenderLayer.FLUID };
	
	// Player that the world is rendered around
	private EntityPlayer clientPlayer;
	// List of chunk models to render
	private final List<ChunkModel> renderList = new ArrayList<>();
	// List of chunks that need model updates
	private final Stack<ChunkModel> generateQueue = new Stack<>();
	private final ExecutorService generatePool = Executors.newWorkStealingPool(); //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	private final AtomicInteger modelUpdates = new AtomicInteger();
	
	private final TextureAtlas atlas;
	private final World world;
	
	// Last chunk position of the player
	private int lastChunkX = 0, lastChunkY = 0, lastChunkZ = 0;
	
	public WorldRenderer(World world, TextureAtlas atlas)
	{
		this.world = world;
		this.atlas = atlas;
	}
	
	public void setClientPlayer(EntityPlayer clientPlayer)
	{
		this.clientPlayer = clientPlayer;
	}
	
	public void update()
	{
		boolean newChunks = false;
		
		// Update existing chunks
		for(Chunk chunk : world.chunkManager.loadedChunks.values())
		{
			if (chunk.isRecentlyLoaded() && !chunk.isPlaceholder())
			{
				// New chunk recently loaded
				newChunks = true;
				ChunkModel model = new ChunkModel(chunk);
				
				renderList.add(model);
				chunk.setPreviouslyLoaded();
				model.forceNeighborRebuild();
			}
		}
		
		Iterator<ChunkModel> modelItr = renderList.iterator();
		
		while (modelItr.hasNext())
		{
			ChunkModel model = modelItr.next();
			
			if (model.chunk.isUnloaded())
			{
				generateQueue.remove(model);
				// Remove the model from the render list
				modelItr.remove();
				continue;
			}
			
			if (model.chunk.needsRebuild() && model.getModelState() == ChunkModel.ModelState.UPDATED)
			{
				// No model update is pending, add it to the generate queue
				model.setModelState(ChunkModel.ModelState.PENDING);
				generateQueue.push(model);
			}
		}
		
		if( (int) clientPlayer.xPos >> 4 != lastChunkX ||
			(int) clientPlayer.yPos >> 4 != lastChunkY ||
			(int) clientPlayer.zPos >> 4 != lastChunkZ ||
			newChunks)
		{
			// Sort for every new chunk added or change in the player's chunk
			
			if (!generateQueue.isEmpty())
			{
				// Sort generate queue by distance to player
				generateQueue.sort(this::distanceSort);
			}
			
			// Sort the render list as well
			renderList.sort(this::distanceSort);
			
			lastChunkX = (int) clientPlayer.xPos >> 4;
			lastChunkY = (int) clientPlayer.yPos >> 4;
			lastChunkZ = (int) clientPlayer.zPos >> 4;
		}
	}
	
	public void render(GameRenderer renderer, double partialTicks)
	{
		// List of chunks that have transparent blocks
		List<ChunkModel> transparentChunks = new ArrayList<>();
		
		renderer.getCurrentShader().setUniform1i("inWater", clientPlayer.isInWater() ? 1 : 0);
		
		glDisable(GL_BLEND);
		
		for (ChunkModel chunkModel : renderList)
		{
			// Perform empty check
			if (chunkModel.chunk.hasNoBlocks())
				continue;
			
			// Cull chunks not inside the view frustum
			if (!renderer.getCamera().getViewFrustum().isSphereInside(
					(chunkModel.chunk.chunkX << 4) + 8.5f,
					(chunkModel.chunk.chunkY << 4) + 8.5f,
					(chunkModel.chunk.chunkZ << 4) + 8.5f,
					22.627416998f))
				continue;
			
			if (chunkModel.hasTransparency())
				transparentChunks.add(chunkModel);
			
			Model model = chunkModel.getModelForLayer(RenderLayer.OPAQUE);
			
			model.bind();
			
			// Update the vertices if an update is not in progress
			if (chunkModel.getModelState() == ChunkModel.ModelState.UPDATED)
				chunkModel.updateLayer(RenderLayer.OPAQUE);
			
			renderer.drawModel(model);
		}
		
		// Reverse iterate through the array & other layers
		for (RenderLayer layer : transparentLayers)
		{
			ListIterator<ChunkModel> itr = transparentChunks.listIterator(transparentChunks.size());
			
			if (layer == RenderLayer.FLUID)
			{
				glDepthMask(false);
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			}
			
			if (layer == RenderLayer.TRANSPARENT)
			{
				glEnable(GL_BLEND);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			}
			
			while (itr.hasPrevious())
			{
				ChunkModel chunkModel = itr.previous();
				Model model = chunkModel.getModelForLayer(layer);
				
				// Update the vertices if a model update is not in progress (have
				// not been updated above)
				model.bind();
				if (chunkModel.getModelState() == ChunkModel.ModelState.UPDATED)
					chunkModel.updateLayer(layer);
				
				renderer.drawModel(model);
			}
			
			if (layer == RenderLayer.FLUID)
			{
				glDepthMask(true);
				glDisable(GL_BLEND);
			}
			
			if (layer == RenderLayer.TRANSPARENT)
			{
				glDisable(GL_BLEND);
			}
		}
		
		// Draw all the entities
		for (Entity e : world.loadedEntities)
		{
			// Skip drawing the local player
			if (e == clientPlayer)
				continue;
			
			renderer.getEntityRenderer(e.getClass()).render(e, renderer, partialTicks);
		}
		
		// Enqueue all the changed chunks
		if (!generateQueue.isEmpty())
		{
			System.out.println("Model Upd (" + generateQueue.size() + ")");
			
			for (int i = 0; i < 16 && !generateQueue.isEmpty(); i++)
				generatePool.execute(new ThreadedChunkGenerator(generateQueue.pop()));
		}
	}
	
	public void stop()
	{
		generatePool.shutdownNow();
	}
	
	private int distanceSort(ChunkModel modelA, ChunkModel modelB)
	{
		Chunk a = modelA.chunk;
		Chunk b = modelB.chunk;
		
		float distA = (float) (Math.pow((a.chunkX << 4) - clientPlayer.xPos, 2) +
				Math.pow((a.chunkY << 4) - clientPlayer.yPos, 2) +
				Math.pow((a.chunkZ << 4) - clientPlayer.zPos, 2));
		float distB = (float) (Math.pow((b.chunkX << 4) - clientPlayer.xPos, 2) +
				Math.pow((b.chunkY << 4) - clientPlayer.yPos, 2) +
				Math.pow((b.chunkZ << 4) - clientPlayer.zPos, 2));
		return Float.compare(distB, distA);
	}
	
	private class ThreadedChunkGenerator implements Runnable
	{
		final ChunkModel model;
		
		ThreadedChunkGenerator(ChunkModel model)
		{
			this.model = model;
		}
		
		@Override
		public void run()
		{
			modelUpdates.incrementAndGet();
			try
			{
				model.updateModel(atlas);
			}
			catch(BufferOverflowException e)
			{
				e.printStackTrace();
			}
			modelUpdates.decrementAndGet();
		}
	}
}
