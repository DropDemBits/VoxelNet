package ddb.io.voxelnet.render;

import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer
{
	// Player that the world is rendered around
	private EntityPlayer player;
	// List of chunks to render
	private Map<Vec3i, ChunkModel> renderChunks = new LinkedHashMap<>();
	// List of chunks that need model updates
	// TODO: Add threads for chunk model generation
	private Stack<ChunkModel> generateQueue = new Stack<>();
	private ExecutorService generatePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
	private TextureAtlas atlas;
	private World world;
	
	public WorldRenderer(World world, TextureAtlas atlas)
	{
		this.world = world;
		this.atlas = atlas;
	}
	
	public void update()
	{
		for (Chunk chunk : world.loadedChunks.values())
		{
			Vec3i pos = new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
			if (chunk.recentlyGenerated())
			{
				ChunkModel model = new ChunkModel(chunk);
				renderChunks.put(pos, model);
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
		}
		
		// Enqueue more updates
		for(int upd = 0; upd < 16 && !generateQueue.empty(); upd++)
		{
			ChunkModel model = generateQueue.peek();
			generatePool.execute(new ThreadedChunkGenerator(generateQueue.pop()));
			System.out.println("Model Upd (" + generateQueue.size() + ") (" + model.chunk.chunkX + ", " + model.chunk.chunkY + ", " + model.chunk.chunkZ + ")");
		}
		
		/*if (!generateQueue.isEmpty())
			new ThreadedChunkGenerator(generateQueue.pop()).run();*/
	}
	
	public void render(Shader shader, Camera camera)
	{
		// List of chunks that have transparent blocks
		List<ChunkModel> transparentChunks = new ArrayList<>();
		
		final float[] matrix = new float[4 * 4];
		
		long opaqueCount = 0;
		long opaqueAccum = 0;
		long updProgressCount = 0;
		for (ChunkModel chunkModel : renderChunks.values())
		{
			// Perform frustum culling
			if (!camera.getViewFrustum().isSphereInside(
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
			
			Model model = chunkModel.getModel();
			
			model.bind();
			// Update the vertices if an update is not in progress
			if (!isUpdating && chunkModel.isDirty())
			{
				model.updateVertices();
				
				// Only update the dirty status if there is no transparency
				if (!chunkModel.hasTransparency())
					chunkModel.makeClean();
			}
			
			chunkModel.getTransform().get(matrix);
			shader.setUniformMatrix4fv("ModelMatrix", false, matrix);
			glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
			model.unbind();
			
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
				chunkModel.getModel().updateVertices();
				model.updateVertices();
				chunkModel.makeClean();
			}
			
			chunkModel.getTransform().get(matrix);
			shader.setUniformMatrix4fv("ModelMatrix", false, matrix);
			glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
			model.unbind();
			
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
	}
	
	public void stop()
	{
		generatePool.shutdownNow();
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
