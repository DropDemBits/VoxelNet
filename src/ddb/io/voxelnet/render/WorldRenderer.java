package ddb.io.voxelnet.render;

import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class WorldRenderer
{
	// List of chunks to render
	private List<ChunkModel> renderChunks = new ArrayList<>();
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
			if (chunk.recentlyGenerated())
			{
				renderChunks.add(new ChunkModel(chunk));
				chunk.setGenerated();
			}
		}
		
		for (ChunkModel chunkModel : renderChunks)
		{
			if (chunkModel.updateModel(atlas))
				break;
		}
	}
	
	public void render()
	{
		for (ChunkModel chunkModel : renderChunks)
		{
			Model model = chunkModel.getModel();
			
			model.bind();
			// Update the vertices
			if (chunkModel.isDirty())
			{
				model.updateVertices();
				chunkModel.makeClean();
			}
			
			glDrawElements(GL_TRIANGLES, model.getIndexCount(), GL_UNSIGNED_INT, 0);
			model.unbind();
		}
	}
}
