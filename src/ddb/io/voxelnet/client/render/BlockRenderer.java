package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockFluid;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.client.render.gl.BufferLayout;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;

public class BlockRenderer
{
	public static final BufferLayout BLOCK_LAYOUT = new BufferLayout()
			// Vertex position
			.addAttribute(BufferLayout.EnumAttribType.FLOAT, 3, false)
			// TexCoord
			.addAttribute(BufferLayout.EnumAttribType.USHORT, 2, true)
			// Light (skyLight, blockLight, aoLight)
			.addAttribute(BufferLayout.EnumAttribType.UBYTE, 3, false);
	
	public static int statNear = 0;
	public static int statSolid = 0;
	public static int statNoShow = 0;
	
	// No instance can be made
	private BlockRenderer() {}
	
	public static void addModel(ModelBuilder builder, Chunk[] adjacentChunks, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		// TODO: Unify things together (have common ao-lighting)
		// TODO: Do AO/smooth lighting per-vertex through averaging
		switch (block.getRenderModel())
		{
			case TORCH:
			case CUBE:
				BlockRenderer.addCube(builder, adjacentChunks, chunk, block, x, y, z, faceTextures, atlas);
				break;
			case FLUID:
				BlockRenderer.addFluid(builder, adjacentChunks, chunk, block, x, y, z, faceTextures, atlas);
				break;
		}
	}
	
	/**
	 * Adds a cube to a model
	 * Performs automatic face culling
	 * @param builder The model builder to add the cube to
	 * @param chunk The chunk the cube exists in
	 * @param x The x position of the cube, relative to the chunk
	 * @param y The y position of the cube, relative to the chunk
	 * @param z The z position of the cube, relative to the chunk
	 * @param faceTextures The face textures for each face
	 * @param atlas The texture atlas to use
	 */
	public static void addCube(ModelBuilder builder, Chunk[] adjacentChunks, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		builder.setIndexOffset(((x^y^z) >> 1) & 1);
		for (Facing face : Facing.values())
		{
			// If the specified face is -1, the face isn't supposed to be rendered
			if(faceTextures[face.ordinal()] == -1)
				continue;
			
			int adjacentX = chunk.chunkX * 16 + x + face.getOffsetX();
			int adjacentY = chunk.chunkY * 16 + y + face.getOffsetY();
			int adjacentZ = chunk.chunkZ * 16 + z + face.getOffsetZ();
			
			byte blockLight;
			byte skyLight;
			
			if (block.isTransparent())
			{
				// Is transparent, get the light values at the current position
				blockLight = chunk.getBlockLight(x, y, z);
				skyLight = chunk.getSkyLight(x, y, z);
			}
			else
			{
				blockLight = chunk.world.getBlockLight(adjacentX, adjacentY, adjacentZ);
				skyLight = chunk.world.getSkyLight(adjacentX, adjacentY, adjacentZ);
			}
			
			Block adjacent = getAdjacentBlock(x, y, z, face, adjacentChunks);
			
			// Don't show the face if it's the same block
			if (!block.showFace(adjacent, face))
			{
				++statNoShow;
				continue;
			}
			
			int[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			
			switch(block.getRenderModel())
			{
				case CUBE:
					BlockRenderer.addCubeFace(
							builder,
							x, y, z,
							face, texCoords,
							skyLight, blockLight, (byte) face.ordinal());
					break;
				case TORCH:
					BlockRenderer.addTorchFace(
							builder,
							x, y, z,
							face, texCoords, atlas,
							skyLight, blockLight, (byte) face.ordinal());
			}
		}
		builder.setIndexOffset(0);
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
			byte skyLight, byte blockLight, byte aoLight)
	{
		builder.addPoly(4);
		switch (face)
		{
			case NORTH:
				// North Face
				builder.pos3(x + 0.0f, y + 1.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 0.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case WEST:
				// West Face
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 1.0f, z + 1.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 1.0f, z + 0.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case SOUTH:
				// South Face
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 1.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 1.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case EAST:
				// East Face
				builder.pos3(x + 1.0f, y + 1.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case UP:
				// Up/Top Face
				builder.pos3(x + 0.0f, y + 1.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 1.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case DOWN:
				// Down/Bottom Face
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
		}
	}
	
	public static void addTorchFace (
			ModelBuilder builder,
			float x, float y, float z,
			Facing face, int[] texCoords, TextureAtlas atlas,
			byte skyLight, byte blockLight, byte aoLight)
	{
		builder.addPoly(4);
		switch(face)
		{
			case NORTH:
				// North Face
				builder.pos3(x + 0.0f, y + 1.0f, z + 0.5f - 1f/16f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 0.5f - 1f/16f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.5f - 1f/16f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.5f - 1f/16f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case WEST:
				// West Face
				builder.pos3(x + 0.5f - 1/16f, y + 0.0f, z + 0.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 0.0f, z + 1.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 1.0f, z + 1.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 1.0f, z + 0.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case SOUTH:
				// South Face
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.5f + 1f/16f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.5f + 1f/16f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 1.0f, z + 0.5f + 1f/16f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 1.0f, z + 0.5f + 1f/16f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case EAST:
				// East Face
				builder.pos3(x + 0.5f + 1/16f, y + 1.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 1.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case UP:
				// Up/Top Face
				builder.pos3(x + 0.5f + 1/16f, y + 9/16f, z + 0.5f + 1/16f).tex2(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*7/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 9/16f, z + 0.5f - 1/16f).tex2(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*7/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 9/16f, z + 0.5f - 1/16f).tex2(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*7/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 9/16f, z + 0.5f + 1/16f).tex2(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*7/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case DOWN:
				// Down/Bottom face
				builder.pos3(x + 0.5f - 1/16f, y + 0.0f, z + 0.5f + 1/16f).tex2(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*0/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f - 1/16f, y + 0.0f, z + 0.5f - 1/16f).tex2(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[1] + (int)(atlas.getPixelScaleY()*0/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 0.0f, z + 0.5f - 1/16f).tex2(texCoords[0] + (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*14/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.5f + 1/16f, y + 0.0f, z + 0.5f + 1/16f).tex2(texCoords[2] - (int)(atlas.getPixelScaleX()*7/16f), texCoords[3] - (int)(atlas.getPixelScaleY()*14/16f)).light3(skyLight, blockLight, aoLight).endVertex();
				break;
		}
	}
	
	public static void addFluid(ModelBuilder builder, Chunk[] adjacentChunks, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		byte meta = chunk.getBlockMeta(x, y, z);
		// x = i % 3
		// z = i / 3
		
		// NW(0) N(1)  NE(2)
		//  W(3) -(4)  E (5)
		// SW(6) S(7)  SE(8)
		byte[] adjacentMetas = new byte[9];
		
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
				adjacentMetas[off] = (byte) (8 - (meta & BlockFluid.DISTANCE));
				continue;
			}
			
			int xOff = (off % 3) - 1;
			int zOff = (off / 3) - 1;
			
			int adjacentX = chunk.chunkX * 16 + x + xOff;
			int adjacentY = chunk.chunkY * 16 + y;
			int adjacentZ = chunk.chunkZ * 16 + z + zOff;
			
			Block adjacentBlock = Block.idToBlock(chunk.getBlock(x + xOff, y, z + zOff));
			Block adjacentAbove = Block.idToBlock(chunk.getBlock(x + xOff, y + 1, z + zOff));
			byte adjacentMeta = chunk.getBlockMeta(x + xOff, y, z + zOff);
			
			if (adjacentBlock == Blocks.VOID)
			{
				// Check the nearby chunk for the appropriate block id & lighting
				adjacentBlock = chunk.world.getBlock(adjacentX, adjacentY, adjacentZ);
				adjacentMeta = chunk.world.getBlockMeta(adjacentX, adjacentY, adjacentZ);
			}
			
			if (adjacentAbove == Blocks.VOID)
			{
				adjacentAbove = chunk.world.getBlock(adjacentX, adjacentY + 1, adjacentZ);
			}
			
			Block adjacent = adjacentBlock;
			Block adjacentUp = adjacentAbove;
			
			if (!((BlockFluid)block).isSameFluid(adjacent))
				// Neighbor block isn't water at all
				adjacentMetas[off] = (byte)-1;
			else
			{
				if (((BlockFluid)block).isSameFluid(adjacentUp))
					// Neighbor & neighbor's above block are water, probably falling
					adjacentMetas[off] = 16;
				else
					// Not falling, get the regular distance
					adjacentMetas[off] = (byte) (8 - (adjacentMeta & BlockFluid.DISTANCE));
			}
		}
		
		// Check the above block
		Block aboveBlock = getAdjacentBlock(x, y, z, Facing.UP, adjacentChunks);
		if (!(aboveBlock instanceof BlockFluid))
		{
			// Water is not falling
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
		for (Facing face : Facing.values())
		{
			Block adjacent = getAdjacentBlock(x, y, z, face, adjacentChunks);
			byte skyLight = chunk.getSkyLight(x, y, z);
			byte blockLight = chunk.getBlockLight(x, y, z);
			
			// Don't show the face if it's the same block
			if (!block.showFace(adjacent, face))
				continue;
			
			int[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			
			addFluidFace(
					builder,
					x, y, z,
					heightNW, heightSW, heightSE, heightNE, face, texCoords,
					skyLight, blockLight, (byte)face.ordinal());
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
			
			if (a >= 0f) { h1 = a; }
			if (b >= 0f) { if (h1 < 0f) h1 = b; else h2 = b; avgCount--; }
			if (c >= 0f) { if (h1 < 0f) h1 = c; else h2 = c; avgCount--; }
			
			assert(avgCount == 0);
			
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
			byte skyLight, byte blockLight, byte aoLight)
	{
		builder.addPoly(4);
		switch (face)
		{
			case NORTH:
				// North Face
				builder.pos3(x + 0.0f, y + nw,   z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + ne,   z + 0.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case WEST:
				// West Face
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + sw,   z + 1.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + nw,   z + 0.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case SOUTH:
				// South Face
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + se,   z + 1.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + sw,   z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case EAST:
				// East Face
				builder.pos3(x + 1.0f, y + ne,   z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + se,   z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case UP:
				// Up/Top Face
				builder.pos3(x + 0.0f, y + nw,   z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + sw,   z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + se,   z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + ne,   z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
			case DOWN:
				// Down/Bottom Face
				builder.pos3(x + 1.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 1.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[3]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 1.0f).tex2(texCoords[0], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				builder.pos3(x + 0.0f, y + 0.0f, z + 0.0f).tex2(texCoords[2], texCoords[1]).light3(skyLight, blockLight, aoLight).endVertex();
				break;
		}
	}
	
	private static Chunk getChunk(int x, int y, int z, Facing face, Chunk[] adjacentChunks)
	{
		// Positions relative to the current chunk
		int relX = x + face.getOffsetX();
		int relY = y + face.getOffsetY();
		int relZ = z + face.getOffsetZ();
		
		// Offsets to the target chunk
		int chunkX = getAdjacentOffset(relX);
		int chunkY = getAdjacentOffset(relY) * 9;
		int chunkZ = getAdjacentOffset(relZ) * 3;
		
		// -  -  0  +
		// -  0  1  2
		// 0  3  4  5
		// +  6  7  8
		
		// 0  -  0  +
		// -  9 10 11
		// 0 12 13 14
		// + 15 16 17
		
		// +  -  0  +
		// - 18 19 20
		// 0 21 22 23
		// + 24 25 26
		
		int index = chunkX + chunkY + chunkZ;
		
		return adjacentChunks[index];
	}
	
	private static int getAdjacentOffset(int coordinate)
	{
		if (coordinate < 0)
			return 0;   // Towards negative, -1
		if (coordinate > 0xF)
			return 2;   // Towards positive, +1
		
		return 1;       // Same column / row
	}
	
	private static Block getAdjacentBlock(int x, int y, int z, Facing off, Chunk[] adjacentChunks)
	{
		return Block.idToBlock(getChunk(x, y, z, off, adjacentChunks).getBlock(
				(x + off.getOffsetX()) & 0xF,
				(y + off.getOffsetY()) & 0xF,
				(z + off.getOffsetZ()) & 0xF));
	}
	
	/**
	 * Converts a three coordinate offset into an adjacent index
	 * @param xOff The x offset to the adjacent position
	 * @param yOff The y offset to the adjacent position
	 * @param zOff The z offset to the adjacent position
	 * @return The adjacent index
	 */
	public static int toAdjacentIndex(int xOff, int yOff, int zOff)
	{
		return (xOff + 1) + (zOff + 1) * 3 + (yOff + 1) * 9;
	}
	
}
