package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorldRenderer
{
	// Player that the world is rendered around
	private EntityPlayer player;
	// List of all chunk models
	private Map<Vec3i, ChunkModel> renderChunks = new LinkedHashMap<>();
	// List of chunk models to render
	private List<ChunkModel> renderList = new ArrayList<>();
	// List of chunks that need model updates
	private Stack<ChunkModel> generateQueue = new Stack<>();
	private ExecutorService generatePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	private TextureAtlas atlas;
	private World world;
	
	// Last chunk position of the player
	private int lastChunkX = 0, lastChunkY = 0, lastChunkZ = 0;
	
	public WorldRenderer(World world, TextureAtlas atlas)
	{
		this.world = world;
		this.atlas = atlas;
	}
	
	public void setPlayer(EntityPlayer player)
	{
		this.player = player;
	}
	
	public void update()
	{
		boolean newChunks = false;
		
		for(Chunk chunk : world.loadedChunks.values())
		{
			Vec3i pos = new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
			if (chunk.recentlyGenerated())
			{
				// New chunk generated
				newChunks = true;
				ChunkModel model = new ChunkModel(chunk);
				renderChunks.put(pos, model);
				renderList.add(model);
				chunk.setGenerated();
			}
			
			if (chunk.needsRebuild())
			{
				assert(renderChunks.get(pos) != null);
				if (!renderChunks.get(pos).isUpdatePending())
				{
					// No model update is pending, add it to the generate queue
					generateQueue.push(renderChunks.get(pos));
					renderChunks.get(pos).setUpdatePending(true);
					//System.out.println("Pending Add (" + generateQueue.size() + ") " + pos.toString());
				}
			}
		};
		
		if( (int)player.xPos >> 4 != lastChunkX ||
			(int)player.yPos >> 4 != lastChunkY ||
			(int)player.zPos >> 4 != lastChunkZ ||
			newChunks)
		{
			// Sort for every new chunk added or change in the player's chunk
			System.out.println("sort!");
			
			if (!generateQueue.isEmpty())
			{
				// Sort generate queue by distance to player
				generateQueue.sort(this::distanceSort);
			}
			
			lastChunkX = (int)player.xPos >> 4;
			lastChunkY = (int)player.yPos >> 4;
			lastChunkZ = (int)player.zPos >> 4;
		}
		
		// Enqueue all the changed chunks
		while(!generateQueue.isEmpty())
		{
			ChunkModel model = generateQueue.peek();
			if (!model.isUpdateInProgress())
			{
				generatePool.execute(new ThreadedChunkGenerator(generateQueue.pop()));
				System.out.println("Model Upd (" + generateQueue.size() + ") (" + model.chunk.chunkX + ", " + model.chunk.chunkY + ", " + model.chunk.chunkZ + ")");
			}
		}
		
		/*if (!generateQueue.isEmpty())
			new ThreadedChunkGenerator(generateQueue.pop()).run();*/
	}
	
	public void render(GameRenderer renderer)
	{
		// List of chunks that have transparent blocks
		List<ChunkModel> transparentChunks = new ArrayList<>();
		
		long opaqueCount = 0;
		long opaqueAccum = 0;
		long updProgressCount = 0;
		for (ChunkModel chunkModel : renderList)
		{
			// Perform empty check
			if (chunkModel.chunk.isEmpty())
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
			
			if (isUpdating)
				++updProgressCount;
			
			if (chunkModel.hasTransparency())
				transparentChunks.add(chunkModel);
			
			Model model = chunkModel.getOpaqueLayer();
			
			model.bind();
			
			// Update the vertices if an update is not in progress
			if (!chunkModel.isUpdateInProgress() && chunkModel.isDirty())
			{
				model.updateVertices();
				
				// Only update the dirty status if there is no transparency
				if (!chunkModel.hasTransparency())
					chunkModel.makeClean();
			}
			
			renderer.drawModel(model);
			
			++opaqueCount;
			opaqueAccum += System.nanoTime() - opaqueStart;
		}
		
		long transparentAccum = 0;
		long transparentCount = transparentChunks.size();
		for (ChunkModel chunkModel : transparentChunks)
		{
			long transparentStart = System.nanoTime();
			boolean isUpdating = chunkModel.isUpdateInProgress();
			Model model = chunkModel.getTransparentModel();
			
			// Update the vertices if a model update is not in progress (have
			// not been updated above)
			model.bind();
			if (!chunkModel.isUpdateInProgress() && chunkModel.isDirty())
			{
				// Update both models
				// Done just in case the update status has changed between the loops
				chunkModel.getOpaqueLayer().updateVertices();
				model.updateVertices();
				chunkModel.makeClean();
			}
			
			renderer.drawModel(model);
			
			transparentAccum += System.nanoTime() - transparentStart;
		}
		
		/*System.out.println("-----------------------------");
		System.out.println(
				"OpqC " + (float)opaqueAccum / ((float)opaqueCount * 1000f) + " us, " +
				"TrnC " + (float)transparentAccum / ((float) transparentCount * 1000f) + " us"
		);
		System.out.println(
				"OpqT " + (float)opaqueAccum / 1000f + " us, " +
				"TrnT " + (float)transparentAccum / 1000f + " us"
		);
		System.out.println(
				"OpqA " + opaqueCount + ", " +
				"TrnA " + transparentCount + ", " +
				"UpdA " + updProgressCount
		);*/
		
		// Draw all the entities
		for (Entity e : world.loadedEntities)
		{
			// Skip drawing the local player
			if (e instanceof EntityPlayer)
				continue;
			
			renderer.getEntityRenderer(e.getClass()).render(e, renderer);
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
		
		float distA = (float) (Math.pow((a.chunkX << 4) - player.xPos, 2) +
				Math.pow((a.chunkY << 4) - player.yPos, 2) +
				Math.pow((a.chunkZ << 4) - player.zPos, 2));
		float distB = (float) (Math.pow((b.chunkX << 4) - player.xPos, 2) +
				Math.pow((b.chunkY << 4) - player.yPos, 2) +
				Math.pow((b.chunkZ << 4) - player.zPos, 2));
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
			model.setUpdatePending(false);
			model.setUpdateProgress(true);
			model.updateModel(atlas);
			model.setUpdateProgress(false);
		}
	}
}
