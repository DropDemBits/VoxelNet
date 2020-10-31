package ddb.io.voxelnet.client.render.entity;

import ddb.io.voxelnet.client.render.BlockRenderer;
import ddb.io.voxelnet.client.render.GameRenderer;
import ddb.io.voxelnet.client.render.Model;
import ddb.io.voxelnet.client.render.ModelBuilder;
import ddb.io.voxelnet.client.render.gl.EnumDrawMode;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.entity.EntityFallingBlock;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.MathUtil;

import java.util.Arrays;

public class EntityRendererFalling extends EntityRenderer
{
	private final Model model;
	private final ModelBuilder builder;
	
	public EntityRendererFalling()
	{
		model = new Model(BlockRenderer.BLOCK_LAYOUT);
		
		builder = new ModelBuilder(BlockRenderer.BLOCK_LAYOUT, EnumDrawMode.TRIANGLES);
	}
	
	@Override
	public void render(Entity e, GameRenderer renderer, double partialTicks)
	{
		EntityFallingBlock entity = (EntityFallingBlock)e;
		
		// Reset the builder to draw another cube
		builder.reset();
		
		// Build the cube model on the fly
		// This is not the most optimal way of doing things, but... it
		// works, okay?
		// TODO: Make this more optimal (e.g. use static model & update it on the fly)
		
		int[] faceTextures = entity.falling.getFaceTextures();
		
		int blockX = (int)Math.floor(e.xPos);
		int blockY = Math.round(e.yPos);
		int blockZ = (int)Math.floor(e.zPos);
		final int[] skyLights = new int[4];
		final int[] blockLights = new int[4];
		final int[] aoLights = new int[4];
		
		Arrays.fill(skyLights, e.world.getSkyLight(blockX, blockY, blockZ) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
		Arrays.fill(blockLights, e.world.getBlockLight(blockX, blockY, blockZ) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
		
		for (Facing face : Facing.directions())
		{
			if (faceTextures[face.ordinal()] == -1)
				continue;
			
			int[] texCoords = renderer.tileAtlas.getPixelPositions(faceTextures[face.ordinal()]);
			
			Arrays.fill(aoLights, face.ordinal());
			BlockRenderer.addCubeFace(builder, 0, 0, 0, face, texCoords, skyLights, blockLights, aoLights);
		}
		
		model.bind();
		model.updateVertices(builder);
		// Compact the builder to free up unused data
		builder.compact();
		
		// Setup the transform
		float centeredX = e.xPos - 0.5f;
		float centeredY = e.yPos;
		float centeredZ = e.zPos - 0.5f;
		
		float lerpX = (float) MathUtil.lerp(centeredX, centeredX + e.xVel, partialTicks);
		float lerpY = (float) MathUtil.lerp(centeredY, centeredY + e.yVel, partialTicks);
		float lerpZ = (float) MathUtil.lerp(centeredZ, centeredZ + e.zVel, partialTicks);
		
		model.getTransform().identity().translate(lerpX, lerpY, lerpZ);
		renderer.drawModel(model);
	}
	
}
