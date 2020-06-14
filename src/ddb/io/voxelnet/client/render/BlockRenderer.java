package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockFluid;
import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.MathUtil;
import ddb.io.voxelnet.world.ChunkField;

import java.util.Arrays;

public class BlockRenderer
{
	/** Position : 3(float), TexCoord : 2(ushort), LightVals : 3(byte) */
	public static final BufferLayout BLOCK_LAYOUT = new BufferLayout()
			// Vertex position
			.addAttribute(BufferLayout.EnumAttribType.FLOAT, 3, false)
			// TexCoord
			.addAttribute(BufferLayout.EnumAttribType.USHORT, 2, true)
			// Light (skyLight, blockLight, [aoLight : 3-4, faceIdx : 0-2])
			.addAttribute(BufferLayout.EnumAttribType.UBYTE, 3, false);
	
	public static final int MAX_SMOOTHING_WEIGHTING = 4;
	
	public static int statNear = 0;
	public static int statSolid = 0;
	public static int statNoShow = 0;
	
	private static final boolean useSmoothLighting = true;
	
	// No instance can be made
	private BlockRenderer() {}
	
	public static void addModel(ModelBuilder builder, ChunkField field, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		switch (block.getRenderModel())
		{
			case TORCH:
			case CUBE:
				BlockRenderer.addCube(builder, field, block, x, y, z, faceTextures, atlas);
				break;
			case FLUID:
				BlockRenderer.addFluid(builder, field, block, x, y, z, faceTextures, atlas);
				break;
		}
	}
	
	/**
	 * Adds a cube to a model
	 * Performs automatic face culling
	 * @param builder The model builder to add the cube to
	 * @param field The field for the associated chunk
	 * @param x The x position of the cube, relative to the chunk
	 * @param y The y position of the cube, relative to the chunk
	 * @param z The z position of the cube, relative to the chunk
	 * @param faceTextures The face textures for each face
	 * @param atlas The texture atlas to use
	 */
	public static void addCube(ModelBuilder builder, ChunkField field, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		for (Facing face : Facing.directions())
		{
			// If the specified face is -1, the face isn't supposed to be rendered
			if(faceTextures[face.ordinal()] == -1)
				continue;
			
			Block adjacent = field.getBlock(x, y, z, face);
			
			// Don't show the face the given block doesn't allow it
			if (!block.showFace(adjacent, face))
			{
				++statNoShow;
				continue;
			}
			
			int[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			
			// Fetch the required light
			final int[] blockLights = new int[4];
			final int[] skyLights = new int[4];
			final int[] aoData = new int[4];
			
			// Flip the face as needed
			if (computeLighting(block, x, y, z, face, field, false, blockLights, skyLights, aoData))
				builder.setIndexOffset(1);
			
			switch(block.getRenderModel())
			{
				case CUBE:
					BlockRenderer.addCubeFace(
							builder,
							x, y, z,
							face, texCoords,
							skyLights, blockLights, aoData);
					break;
				case TORCH:
					BlockRenderer.addTorchFace(
							builder,
							x, y, z,
							face, texCoords, atlas,
							skyLights, blockLights, aoData);
			}
			
			builder.setIndexOffset(0);
		}
	}
	
	/**
	 * Adds a cube face to the model
	 * @param builder The ModelBuilder to use
	 * @param x The x block coordinate of the face
	 * @param y The y block coordinate of the face
	 * @param z The z block coordinate of the face
	 * @param face The specific face to draw.
	 * @param texCoords The texture coordinates of the face
	 */
	public static void addCubeFace(
			ModelBuilder builder,
			float x, float y, float z,
			Facing face, int[] texCoords,
			int[] skyLight, int[] blockLight, int[] aoLight)
	{
		// SmoothLight computations
		// 1 2
		// 0 3
		// Cube
		
		builder.addPoly(4);
		switch (face)
		{
			case SOUTH:
				// x y | 3 2 1 0
				// South Face
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case NORTH:
				// x y | 0 1 2 3
				// North Face
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case EAST:
				// y z | 3 2 1 0
				// East Face
				builder.pos3f(x + 1.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case WEST:
				// y z | 0 1 2 3
				// West Face
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case UP:
				// x z | 0 1 2 3
				// Up/Top Face
				builder.pos3f(x + 0.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case DOWN:
				// x z | 3 2 1 0
				// Down/Bottom Face
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
		}
	}
	
	public static void addTorchFace (
			ModelBuilder builder,
			float x, float y, float z,
			Facing face, int[] texCoords, TextureAtlas atlas,
			int[] skyLight, int[] blockLight, int[] aoLight)
	{
		// Torch
		builder.addPoly(4);
		switch(face)
		{
			case SOUTH:
				// South Face
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.5f + 1f/16f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 0.5f + 1f/16f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 0.5f + 1f/16f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.5f + 1f/16f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case NORTH:
				// North Face
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.5f - 1f/16f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + 1.0f, z + 0.5f - 1f/16f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + 1.0f, z + 0.5f - 1f/16f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.5f - 1f/16f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case EAST:
				// East Face
				builder.pos3f(x + 0.5f + 1/16f, y + 1.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 1.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case WEST:
				// West Face
				builder.pos3f(x + 0.5f - 1/16f, y + 0.0f, z + 0.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[0]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 0.0f, z + 1.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[2], blockLight[2], aoLight[1]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 1.0f, z + 1.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[1], blockLight[1], aoLight[2]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 1.0f, z + 0.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[0], blockLight[0], aoLight[3]).endVertex();
				break;
			case UP:
				// Up/Top Face
				builder.pos3f(x + 0.5f + 1/16f, y + 9/16f, z + 0.5f + 1/16f).tex2i(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*7/16f)).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 9/16f, z + 0.5f - 1/16f).tex2i(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*7/16f)).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 9/16f, z + 0.5f - 1/16f).tex2i(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*7/16f)).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 9/16f, z + 0.5f + 1/16f).tex2i(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*7/16f)).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case DOWN:
				// Down/Bottom face
				builder.pos3f(x + 0.5f - 1/16f, y + 0.0f, z + 0.5f + 1/16f).tex2i(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()* 0/16f)).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 0.5f - 1/16f, y + 0.0f, z + 0.5f - 1/16f).tex2i(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()* 0/16f)).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 0.0f, z + 0.5f - 1/16f).tex2i(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*14/16f)).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.5f + 1/16f, y + 0.0f, z + 0.5f + 1/16f).tex2i(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*14/16f)).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
		}
	}
	
	public static void addFluid(ModelBuilder builder, ChunkField field, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		int meta = field.getMeta(x, y, z, Facing.NONE);
		// x = i % 3
		// z = i / 3
		
		// NW(0) N(1)  NE(2)
		//  W(3) -(4)  E (5)
		// SW(6) S(7)  SE(8)
		final int[] adjacentMetas = new int[9];
		
		// Current level of the block
		int level = 8 - (meta & BlockFluid.DISTANCE);
		final double HEIGHT_VAL = ((15f / 8f) / 16f);
		
		// NW(0) N(1)  NE(2)
		//  W(3) -(4)  E (5)
		// SW(6) S(7)  SE(8)
		// Corner heights
		float heightNW = 1f;
		float heightSW = 1f;
		float heightSE = 1f;
		float heightNE = 1f;
		
		// Get the water levels of the neighboring water blocks
		for (int off = 0; off < 9; off++)
		{
			if (off == 4)
			{
				// Current block, no need to fetch
				adjacentMetas[off] = (8 - (meta & BlockFluid.DISTANCE));
				continue;
			}
			
			int xOff = (off % 3) - 1;
			int zOff = (off / 3) - 1;
			
			Block adjacent   = field.getBlock(x + xOff, y, z + zOff, Facing.NONE);
			Block adjacentUp = field.getBlock(x + xOff, y + 1, z + zOff, Facing.NONE);
			int adjacentMeta = field.getMeta(x + xOff, y, z + zOff, Facing.NONE);
			
			if (!((BlockFluid)block).isSameFluid(adjacent))
				// Neighbor block isn't water at all
				adjacentMetas[off] = -1;
			else
			{
				if (((BlockFluid)block).isSameFluid(adjacentUp))
					// Neighbor & neighbor's above block are water, probably falling
					adjacentMetas[off] = 16;
				else
					// Not falling, get the regular distance
					adjacentMetas[off] = (8 - (adjacentMeta & BlockFluid.DISTANCE));
			}
		}
		
		// Check the above block
		Block aboveBlock = field.getBlock(x, y, z, Facing.UP);
		if (!((BlockFluid)block).isSameFluid(aboveBlock))
		{
			// Fluid is not falling
			heightNW = (float)(tripleAverage(adjacentMetas[1], adjacentMetas[0], adjacentMetas[3], level) * HEIGHT_VAL); // N + NW + W
			heightSW = (float)(tripleAverage(adjacentMetas[7], adjacentMetas[6], adjacentMetas[3], level) * HEIGHT_VAL); // S + SW + W
			heightSE = (float)(tripleAverage(adjacentMetas[7], adjacentMetas[8], adjacentMetas[5], level) * HEIGHT_VAL); // S + SE + E
			heightNE = (float)(tripleAverage(adjacentMetas[1], adjacentMetas[2], adjacentMetas[5], level) * HEIGHT_VAL); // N + NE + E
			
			// If neighbor is falling, hoist up the corner height
			if(adjacentMetas[1] == 16 || adjacentMetas[0] == 16 || adjacentMetas[3] == 16) heightNW = 1f;
			if(adjacentMetas[7] == 16 || adjacentMetas[6] == 16 || adjacentMetas[3] == 16) heightSW = 1f;
			if(adjacentMetas[7] == 16 || adjacentMetas[8] == 16 || adjacentMetas[5] == 16) heightSE = 1f;
			if(adjacentMetas[1] == 16 || adjacentMetas[2] == 16 || adjacentMetas[5] == 16) heightNE = 1f;
		}
		
		// Build the faces
		for (Facing face : Facing.directions())
		{
			Block adjacent = field.getBlock(x, y, z, face);
			
			// Don't show the face if the block doesn't allow
			if (!block.showFace(adjacent, face))
				continue;
			
			int[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			
			// Fetch the required light
			final int[] blockLights = new int[4];
			final int[] skyLights = new int[4];
			final int[] aoLights = new int[4];
			
			computeLighting(block, x, y, z, face, field, true, blockLights, skyLights, aoLights);
			
			addFluidFace(
					builder,
					x, y, z,
					heightNW, heightSW, heightSE, heightNE, face, texCoords,
					skyLights, blockLights, aoLights);
		}
	}
	
	private static double tripleAverage(double a, double b, double c, double def)
	{
		double retval = def;
		int avgCount = 0;
		
		if (a >= 0f) avgCount++;
		if (b >= 0f) avgCount++;
		if (c >= 0f) avgCount++;
		
		if (avgCount == 3)
		{
			// All three are not zero, average the three of them
			retval = (a + b + c + def) / (4f);
		}
		else if (avgCount == 2)
		{
			// Figure out which one should be the one
			double h1 = -1f, h2 = -1f;
			
			if (a >= 0f) { h1 = a; avgCount--; }
			if (b >= 0f) { if (h1 < 0f) h1 = b; else h2 = b; avgCount--; }
			if (c >= 0f) { if (h1 < 0f) h1 = c; else h2 = c; avgCount--; }
			
			assert avgCount == 0 : "AvgCount is " + avgCount;
			
			retval = (h1 + h2 + def) / 3f;
		}
		else if (avgCount == 1)
		{
			if (a >= 0f) retval = (a + def) / 2f;
			//if (b >= 0f) retval = (b + def) / 2f;
			if (c >= 0f) retval = (c + def) / 2f;
		}
		
		// Default
		return retval;
	}
	
	private static void addFluidFace(
			ModelBuilder builder,
			float x, float y, float z,
			float nw, float sw, float se, float ne,
			Facing face, int[] texCoords,
			int[] skyLight, int[] blockLight, int[] aoLight)
	{
		int vLeft, vRight;
		
		builder.addPoly(4);
		switch (face)
		{
			case SOUTH:
				// South Face
				vLeft = (int)MathUtil.lerp(texCoords[1], texCoords[3], se);
				vRight = (int)MathUtil.lerp(texCoords[1], texCoords[3], sw);
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + se,   z + 1.0f).tex2i(texCoords[2],        vLeft).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + sw,   z + 1.0f).tex2i(texCoords[0],       vRight).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case NORTH:
				// North Face
				vLeft = (int)MathUtil.lerp(texCoords[1], texCoords[3], nw);
				vRight = (int)MathUtil.lerp(texCoords[1], texCoords[3], ne);
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + nw,   z + 0.0f).tex2i(texCoords[2],        vLeft).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + ne,   z + 0.0f).tex2i(texCoords[0],       vRight).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case EAST:
				// East Face
				vLeft = (int)MathUtil.lerp(texCoords[1], texCoords[3], ne);
				vRight = (int)MathUtil.lerp(texCoords[1], texCoords[3], se);
				builder.pos3f(x + 1.0f, y + ne,   z + 0.0f).tex2i(texCoords[2],        vLeft).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + se,   z + 1.0f).tex2i(texCoords[0],       vRight).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
			case WEST:
				// West Face
				vLeft = (int)MathUtil.lerp(texCoords[1], texCoords[3], sw);
				vRight = (int)MathUtil.lerp(texCoords[1], texCoords[3], nw);
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + sw,   z + 1.0f).tex2i(texCoords[2],        vLeft).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + nw,   z + 0.0f).tex2i(texCoords[0],       vRight).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case UP:
				// Up/Top Face
				builder.pos3f(x + 0.0f, y + nw,   z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				builder.pos3f(x + 0.0f, y + sw,   z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 1.0f, y + se,   z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 1.0f, y + ne,   z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				break;
			case DOWN:
				// Down/Bottom Face
				builder.pos3f(x + 1.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[3]).light3b(skyLight[3], blockLight[3], aoLight[3]).endVertex();
				builder.pos3f(x + 1.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[3]).light3b(skyLight[2], blockLight[2], aoLight[2]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 1.0f).tex2i(texCoords[0], texCoords[1]).light3b(skyLight[1], blockLight[1], aoLight[1]).endVertex();
				builder.pos3f(x + 0.0f, y + 0.0f, z + 0.0f).tex2i(texCoords[2], texCoords[1]).light3b(skyLight[0], blockLight[0], aoLight[0]).endVertex();
				break;
		}
	}
	
	/*--* Lighting Helpers *--*/
	
	/**
	 * Computes the lighting for the given face
	 *
	 * Uses the smoothing method if "useSmoothLighting" is true
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param face The face to compute the smooth lighting for
	 * @param field The adjacent field for the block's chunk
	 * @param forceSmoothing Whether to force smoothing or not
	 * @param destBlockLights The destination sum of the block lights
	 * @param destSkyLights The destination sum of the sky lights
	 * @return True if the current face needs to be flipped, false otherwise
	 */
	private static boolean computeLighting(Block block,
			int x, int y, int z,
			Facing face, ChunkField field,
			boolean forceSmoothing,
			int[] destBlockLights, int[] destSkyLights, int[] destAOLights)
	{
		if (!useSmoothLighting)
		{
			if (block.isTransparent() || !block.isFilledCube())
			{
				// Is transparent, get the light values at the current position
				Arrays.fill(destBlockLights, field.getBlockLight(x, y, z, Facing.NONE) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
				Arrays.fill(destSkyLights,   field.getSkyLight(x, y, z, Facing.NONE) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
				Arrays.fill(destAOLights, (0 << 3) | face.ordinal());
			} else
			{
				// Not transparent, fetch the lighting for the adjacent face
				Arrays.fill(destBlockLights, field.getBlockLight(x, y, z, face) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
				Arrays.fill(destSkyLights,   field.getSkyLight(x, y, z, face) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
				Arrays.fill(destAOLights, (0 << 3) | face.ordinal());
			}
			
			// Non-smooth faces don't need flipping
			return false;
		}
		
		if ((!block.isFilledCube() && !forceSmoothing) || face == Facing.NONE)
		{
			// Non-filled cubes get the current block as lighting
			// "NONE" faces also get the current block as lighting
			Arrays.fill(destBlockLights, field.getBlockLight(x, y, z, Facing.NONE) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
			Arrays.fill(destSkyLights,   field.getSkyLight(x, y, z, Facing.NONE) * BlockRenderer.MAX_SMOOTHING_WEIGHTING);
			Arrays.fill(destAOLights, (0 << 3) | face.ordinal());
			
			// No flipping to be done
			return false;
		}
		
		// True verts
		//  0 3 +h
		//  1 2
		// +v   +vh
		
		// Vertex Horizontal Offset
		final int[] vho = new int[] {0, 0, 1, 1};
		// Vertex Vertical Offset
		final int[] vvo = new int[] {0, 1, 1, 0};
		
		// Opaque blocks existing on the face's plane
		final boolean[] opaqueBlocks = new boolean[4];
		
		// Get light at the current position
		int currBlockLight = field.getBlockLight(x, y, z, Facing.NONE);
		int currSkyLight   = field.getSkyLight  (x, y, z, Facing.NONE);
		
		// Accumulate the average light for each vertex
		for (int v = 0; v < 4; v++)
		{
			for (int off = 0; off < 4; off++)
			{
				int horizontalOff = (vho[off] - 1) + vho[v];
				int verticalOff = (vvo[off] - 1) + vvo[v];
				
				int lastBlockLight = destBlockLights[v];
				int lastSkyLight   = destSkyLights[v];
				final int weighting = 1;
				
				// Search position
				int xOff = 0, yOff = 0, zOff = 0;
				
				switch (face)
				{
					case WEST:
					case EAST:
						// Axis is y as horizontal, z as vertical
						yOff = horizontalOff;
						zOff = verticalOff;
						break;
					case NORTH:
					case SOUTH:
						// Axis is x as horizontal, y as vertical
						xOff = horizontalOff;
						yOff = verticalOff;
						break;
					case UP:
					case DOWN:
						// Axis is x as horizontal, z as vertical
						xOff = horizontalOff;
						zOff = verticalOff;
						break;
					default:
						// Do nothing, but should not be here
				}
				
				// Update the lighting
				destBlockLights[v] += field.getBlockLight(x + xOff, y + yOff, z + zOff, face) * weighting;
				destSkyLights[v]   += field.getSkyLight  (x + xOff, y + yOff, z + zOff, face) * weighting;
				
				// Get the transparent state of the block
				opaqueBlocks[off]   = !field.getBlock(x + xOff, y + yOff, z + zOff, face).isTransparent();
				
				// If no change & smoothing is forced, apply the light at the block's position
				boolean canUseSelfLight = forceSmoothing || block.isTransparent() || !block.isFilledCube();
				
				if (canUseSelfLight)
				{
					if (destBlockLights[v] - lastBlockLight == 0)
						destBlockLights[v] += currBlockLight * weighting;
					if (destSkyLights[v] - lastSkyLight == 0)
						destSkyLights[v] += currSkyLight * weighting;
				}
			}
			
			// Compute ao index
			int side0, side1, corner;
			
			// Convert booleans to int
			side0  = opaqueBlocks[(v + 3) % 4] ? 1 : 0;
			corner = opaqueBlocks[(v + 0) % 4] ? 1 : 0;
			side1  = opaqueBlocks[(v + 1) % 4] ? 1 : 0;
			
			if (side0 + side1 == 2)
				destAOLights[v] = 3;
			else
				destAOLights[v] = (side0 + side1 + corner);
			
			// Merge the face index
			destAOLights[v] <<= 3;
			destAOLights[v] |= face.ordinal() & 0x7;
		}
		
		// Flip depending on the ao index
		boolean flipFace = (destAOLights[0] + destAOLights[2]) < (destAOLights[1] + destAOLights[3]);
		
		// If it's the negative face for the given axis, flip the condition
		if (face == Facing.WEST || face == Facing.NORTH || face == Facing.UP)
			flipFace = !flipFace;
		
		return flipFace;
	}
	
}
