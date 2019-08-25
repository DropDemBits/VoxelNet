package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.*;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityFallingBlock;
import ddb.io.voxelnet.util.Facing;
import org.joml.Matrix4f;

public class EntityRendererFalling extends EntityRenderer
{
	private Model model;
	
	public EntityRendererFalling()
	{
		model = new Model(BlockRenderer.BLOCK_LAYOUT);
		model.setTransform(new Matrix4f());
	}
	
	@Override
	public void render(Entity e, GameRenderer renderer)
	{
		EntityFallingBlock entity = (EntityFallingBlock)e;
		
		model.reset();
		
		// Build the cube model on the fly
		// This is not the most optimal way of doing things, but... it
		// works, okay?
		// TODO: Make this more optimal
		int[] faceTextures = entity.falling.getFaceTextures();
		for (Facing face : Facing.values())
		{
			if (faceTextures[face.ordinal()] == -1)
				continue;
			
			final float[] faceIntensities = new float[] { 0.75f, 0.75f, 0.75f, 0.75f, 0.95f, 0.55f };
			short[] texCoords = renderer.tileAtlas.getPixelPositions(faceTextures[face.ordinal()]);
			BlockRenderer.addCubeFace(model, 0, 0, 0, face, texCoords, (faceIntensities[face.ordinal()] * 255));
		}
		
		model.bind();
		model.updateVertices();
		model.freeData();
		
		// Setup the transform
		model.getTransform().identity().translate(e.xPos - 0.5f, e.yPos, e.zPos - 0.5f);
		renderer.drawModel(model);
	}
	
}
