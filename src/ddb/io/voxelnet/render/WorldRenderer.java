package ddb.io.voxelnet.render;

import ddb.io.voxelnet.util.Vec3i;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;

import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer
{
	// List of chunks to render
	private Map<Vec3i, ChunkModel> renderChunks = new LinkedHashMap<>();
	private Stack<ChunkModel> generateQueue = new Stack<>();
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
			
			if (chunk.isDirty())
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
		
		// Update the model
		if(!generateQueue.empty())
		{
			ChunkModel model = generateQueue.pop();
			model.updateModel(atlas);
			model.setUpdatePending(false);
			//System.out.println("Model Upd (" + generateQueue.size() + ") (" + model.chunk.chunkX + ", " + model.chunk.chunkY + ", " + model.chunk.chunkZ + ")");
		}
	}
	
	public void render()
	{
		// List of chunks that have transparent blocks
		List<ChunkModel> transparentChunks = new ArrayList<>();
		
		for (ChunkModel chunkModel : renderChunks.values())
		{
			if (chunkModel.hasTransparency())
				transparentChunks.add(chunkModel);
			
			Model model = chunkModel.getModel();
			
			model.bind();
			// Update the vertices
			if (chunkModel.isDirty())
			{
				model.updateVertices();
				
				// Only update the dirty status if there is no transparency
				if (!chunkModel.hasTransparency())
					chunkModel.makeClean();
			}
			
			glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
			model.unbind();
		}
		
		for (ChunkModel chunkModel : transparentChunks)
		{
			Model model = chunkModel.getTransparentModel();
			
			// Update the vertices (have not been updated above)
			model.bind();
			if (chunkModel.isDirty())
			{
				// Update transparency model
				model.updateVertices();
				chunkModel.makeClean();
			}
			
			glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
			model.unbind();
		}
	}
}
