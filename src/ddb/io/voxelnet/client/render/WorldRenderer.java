package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.Vec3i;
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
	private static final RenderLayer[] transparentLayers = new RenderLayer[] { RenderLayer.TRANSPARENT, RenderLayer.FLUID };
	
	// Player that the world is rendered around
	private EntityPlayer clientPlayer;
	// List of all chunk models
	private final Map<Vec3i, ChunkModel> renderChunks = new LinkedHashMap<>();
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
			if (chunk.recentlyGenerated())
			{
				// New chunk generated
				newChunks = true;
				ChunkModel model = new ChunkModel(chunk);
				renderList.add(model);
				chunk.setGenerated();
			}
		}
		
		for (ChunkModel model : renderList)
		{
			if (model.chunk.needsRebuild())
			{
				if (!model.isUpdatePending() && !model.isUpdateInProgress())
				{
					// No model update is pending, add it to the generate queue
					generateQueue.push(model);
					model.setUpdatePending(true);
					//System.out.println("Pending Add (" + generateQueue.size() + ") " + pos.toString());
				}
			}
		}
		
		if( (int) clientPlayer.xPos >> 4 != lastChunkX ||
			(int) clientPlayer.yPos >> 4 != lastChunkY ||
			(int) clientPlayer.zPos >> 4 != lastChunkZ ||
			newChunks)
		{
			// Sort for every new chunk added or change in the player's chunk
			System.out.println("sort!");
			
			if (!generateQueue.isEmpty())
			{
				// Sort generate queue by distance to player
				generateQueue.sort(this::distanceSort);
			}
			
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
			if (chunkModel.chunk.isEmpty())
				continue;
			
			// Render around a certain radius
			// ???: How about only rendering the active chunks? (takes care of distancing problems)
			if (((chunkModel.chunk.chunkX << 4) + 8.5f - clientPlayer.xPos)*((chunkModel.chunk.chunkX << 4) + 8.5f - clientPlayer.xPos) +
					((chunkModel.chunk.chunkZ << 4) + 8.5f - clientPlayer.zPos)*((chunkModel.chunk.chunkZ << 4) + 8.5f - clientPlayer.zPos) > (8*8)*256)
				continue;
			
			// Perform frustum culling
			if (!renderer.getCamera().getViewFrustum().isSphereInside(
					(chunkModel.chunk.chunkX << 4) + 8.5f,
					(chunkModel.chunk.chunkY << 4) + 8.5f,
					(chunkModel.chunk.chunkZ << 4) + 8.5f,
					22.627416998f))
				continue;
				
			
			long opaqueStart = System.nanoTime();
			// Allow a chunk to be rendered between model updates
			boolean isUpdating = chunkModel.isUpdateInProgress();
			
			if (chunkModel.hasTransparency())
				transparentChunks.add(chunkModel);
			
			Model model = chunkModel.getModelForLayer(RenderLayer.OPAQUE);
			
			model.bind();
			
			// Update the vertices if an update is not in progress
			if (!chunkModel.isUpdateInProgress())
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
				
				long transparentStart = System.nanoTime();
				Model model = chunkModel.getModelForLayer(layer);
				
				// Update the vertices if a model update is not in progress (have
				// not been updated above)
				model.bind();
				if (!chunkModel.isUpdateInProgress())
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
			model.setUpdateProgress(true);
			
			modelUpdates.incrementAndGet();
			try
			{
				if (!model.updateModel(atlas))
					System.err.println("OI, MODEL UPDATE DIDN'T HAPPEN @ " + new Vec3i(model.chunk.chunkX, model.chunk.chunkY, model.chunk.chunkZ).toString());
			}
			catch(BufferOverflowException e)
			{
				e.printStackTrace();
			}
			modelUpdates.decrementAndGet();
			
			model.setUpdateProgress(false);
			model.setUpdatePending(false);
		}
	}
}
