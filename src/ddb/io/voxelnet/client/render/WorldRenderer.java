package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;
import org.lwjgl.opengl.GL11;

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
	private EntityPlayer player;
	// List of all chunk models
	private final Map<Vec3i, ChunkModel> renderChunks = new LinkedHashMap<>();
	// List of chunk models to render
	private final List<ChunkModel> renderList = new ArrayList<>();
	// List of chunks that need model updates
	private final Stack<ChunkModel> generateQueue = new Stack<>();
	private final ExecutorService generatePool = Executors.newWorkStealingPool(); //Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	private final AtomicInteger modelUpdates = new AtomicInteger();
	private int updates = 5;
	
	private final TextureAtlas atlas;
	private final World world;
	
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
		
		// Generate chunks in a 16x16 radius around the player
		/*for (int cx = -3; cx <= 3; cx++)
		{
			for (int cz = -3; cz <= 3; cz++)
			{
				int cxOff = (int)(player.xPos / 16f);
				int czOff = (int)(player.zPos / 16f);
				
				if (world.getChunk(cx + cxOff, 0, cz + czOff) == world.EMPTY_CHUNK)
					world.generateChunk(cx + cxOff, cz + czOff);
			}
		}*/
		
		// Update existing chunks
		for(Chunk chunk : world.loadedChunks.values())
		{
			Vec3i pos = new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
			if (chunk.recentlyGenerated())
			{
				// New chunk generated
				newChunks = true;
				ChunkModel model = new ChunkModel(chunk);
				//renderChunks.put(pos, model);
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
		
		
		/*if (!generateQueue.isEmpty())
			new ThreadedChunkGenerator(generateQueue.pop()).run();*/
	}
	
	public void render(GameRenderer renderer)
	{
		// Enqueue all the changed chunks
		if (!generateQueue.isEmpty())
		{
			System.out.println("Model Upd (" + generateQueue.size() + ")");
			while (!generateQueue.isEmpty())
			{
				ChunkModel model = generateQueue.peek();
				//System.out.println("Model Upd (" + generateQueue.size() + ") (" + model.chunk.chunkX + ", " + model.chunk.chunkY + ", " + model.chunk.chunkZ + ")");
				generatePool.execute(new ThreadedChunkGenerator(generateQueue.pop()));
				//new ThreadedChunkGenerator(generateQueue.pop()).run();
			}
		}
		
		// List of chunks that have transparent blocks
		List<ChunkModel> transparentChunks = new ArrayList<>();
		boolean canUpdateChunks = false;
		
		//if (modelUpdates.get() == 0)
		/*	--updates;
		else
			updates = 5;*/
		
		//if (updates == 0)
		{
			//updates = 5;
			canUpdateChunks = true;
		}
		
		renderer.getCurrentShader().setUniform1i("inWater", player.isInWater() ? 1 : 0);
		
		long opaqueCount = 0;
		long opaqueAccum = 0;
		long updProgressCount = 0;
		
		glDisable(GL_BLEND);
		
		for (ChunkModel chunkModel : renderList)
		{
			// Perform empty check
			if (chunkModel.chunk.isEmpty())
				continue;
			
			// Render around a certain radius
			/*if (((chunkModel.chunk.chunkX << 4) + 8.5f - player.xPos)*((chunkModel.chunk.chunkX << 4) + 8.5f - player.xPos)+
					((chunkModel.chunk.chunkZ << 4) + 8.5f - player.zPos)*((chunkModel.chunk.chunkZ << 4) + 8.5f - player.zPos) > (48 * 48))
				continue;*/
			
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
			
			Model model = chunkModel.getModelForLayer(RenderLayer.OPAQUE);
			
			model.bind();
			
			// Update the vertices if an update is not in progress
			if (!chunkModel.isUpdateInProgress() && canUpdateChunks)
				chunkModel.updateLayer(RenderLayer.OPAQUE);
			
			renderer.drawModel(model);
			
			++opaqueCount;
			opaqueAccum += System.nanoTime() - opaqueStart;
		}
		
		/*if (!transparentChunks.isEmpty())
			transparentChunks.sort((a, b) -> -distanceSort(a, b));*/
		
		// Reverse iterate through the array & other layers
		for (RenderLayer layer : transparentLayers)
		{
			long transparentAccum = 0;
			long transparentCount = transparentChunks.size();
			ListIterator<ChunkModel> itr = transparentChunks.listIterator(transparentChunks.size());
			
			if (layer == RenderLayer.FLUID)
			{
				glDisable(GL11.GL_CULL_FACE);
				//glDisable(GL_DEPTH_TEST);
				//glDepthMask(false);
				glEnable(GL_BLEND);
				//glBlendFunc(GL_ZERO, GL_SRC_COLOR);
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
				boolean isUpdating = chunkModel.isUpdateInProgress();
				Model model = chunkModel.getModelForLayer(layer);
				
				// Update the vertices if a model update is not in progress (have
				// not been updated above)
				model.bind();
				if (!chunkModel.isUpdateInProgress() && canUpdateChunks)
					chunkModel.updateLayer(layer);
				
				renderer.drawModel(model);
				
				transparentAccum += System.nanoTime() - transparentStart;
			}
			
			if (layer == RenderLayer.FLUID)
			{
				GL11.glEnable(GL11.GL_CULL_FACE);
				//glEnable(GL_DEPTH_TEST);
				//glDepthMask(true);
				glDisable(GL_BLEND);
				//glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			}
			
			if (layer == RenderLayer.TRANSPARENT)
			{
				glDisable(GL_BLEND);
			}
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
