package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.*;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityFallingBlock;
import ddb.io.voxelnet.util.Facing;
import org.joml.Matrix4f;

public class EntityRendererFalling extends EntityRenderer
{
	private final Model model;
	private final ModelBuilder builder;
	
	public EntityRendererFalling()
	{
		model = new Model(BlockRenderer.BLOCK_LAYOUT);
		model.setTransform(new Matrix4f());
		
		builder = new ModelBuilder(BlockRenderer.BLOCK_LAYOUT, EnumDrawMode.TRIANGLES);
	}
	
	@Override
	public void render(Entity e, GameRenderer renderer)
	{
		EntityFallingBlock entity = (EntityFallingBlock)e;
		
		// Reset the builder to draw another cube
		builder.reset();
		
		// Build the cube model on the fly
		// This is not the most optimal way of doing things, but... it
		// works, okay?
		// TODO: Make this more optimal
		
		int[] faceTextures = entity.falling.getFaceTextures();
		
		int blockX = (int)Math.floor(e.xPos);
		int blockY = Math.round(e.yPos);
		int blockZ = (int)Math.floor(e.zPos);
		byte skyLight = e.world.getSkyLight(blockX, blockY, blockZ);
		byte blockLight = e.world.getBlockLight(blockX, blockY, blockZ);
		
		for (Facing face : Facing.values())
		{
			if (faceTextures[face.ordinal()] == -1)
				continue;
			
			int[] texCoords = renderer.tileAtlas.getPixelPositions(faceTextures[face.ordinal()]);
			BlockRenderer.addCubeFace(builder, 0, 0, 0, face, texCoords, skyLight, blockLight, (byte)face.ordinal());
		}
		
		model.bind();
		model.updateVertices(builder);
		// Compact the builder to free up unused data
		builder.compact();
		
		// Setup the transform
		model.getTransform().identity().translate(e.xPos - 0.5f, e.yPos, e.zPos - 0.5f);
		renderer.drawModel(model);
	}
	
}
