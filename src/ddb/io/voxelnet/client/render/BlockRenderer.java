package ddb.io.voxelnet.client.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.BlockWater;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;

public class BlockRenderer
{
	public static final BufferLayout BLOCK_LAYOUT = new BufferLayout()
			// Vertex position
			.addAttribute(BufferLayout.EnumAttribType.FLOAT, 3, false)
			// TexCoord
			.addAttribute(BufferLayout.EnumAttribType.USHORT, 2, true)
			// Intensity
			.addAttribute(BufferLayout.EnumAttribType.UBYTE, 1, true);
	
	// No instance can be made
	private BlockRenderer() {}
	
	/**
	 * Adds a cube to a model
	 * Performs automatic face culling
	 * @param model The model to add the cube to
	 * @param chunk The chunk the cube exists in
	 * @param x The x position of the cube, relative to the chunk
	 * @param y The y position of the cube, relative to the chunk
	 * @param z The z position of the cube, relative to the chunk
	 * @param faceTextures The face textures for each face
	 * @param atlas The texture atlas to use
	 */
	public static void addCube(Model model, Chunk[] adjacentChunks, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		for (Facing face : Facing.values())
		{
			// If the specified face is -1, the face isn't supposed to be rendered
			if(faceTextures[face.ordinal()] == -1)
				continue;
			
			int adjacentX = chunk.chunkX * 16 + x + face.getOffsetX();
			int adjacentY = chunk.chunkY * 16 + y + face.getOffsetY();
			int adjacentZ = chunk.chunkZ * 16 + z + face.getOffsetZ();
			
			byte faceLight = chunk.world.getBlockLight(adjacentX, adjacentY, adjacentZ);
			byte adjacentBlock = chunk.getBlock(x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
			
			if (adjacentBlock == -1)
			{
				// Check the nearby chunk for the appropriate block id & lighting
				adjacentBlock = adjacentChunks[face.ordinal()].getBlock(adjacentX & 0xF, adjacentY & 0xF, adjacentZ & 0xF); //chunk.world.getBlock(adjacentX, adjacentY, adjacentZ);
			}
			
			Block adjacent = Block.idToBlock(adjacentBlock);
			if (adjacent.isSolid() && !adjacent.isTransparent())
			{
				// Don't add the face if the adjacent block is
				// solid and not transparent
				continue;
			}
			
			// Don't show the face if it's the same block
			if (!block.showFace(adjacent, face))
				continue;
			
			short[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			final float[] faceIntensities = new float[] { 0.75f, 0.75f, 0.75f, 0.75f, 0.95f, 0.55f };
			
			BlockRenderer.addCubeFace(
					model,
					(float) (x),
					(float) (y),
					(float) (z),
					face,
					texCoords,
					(byte)(((faceLight) / 15f) * faceIntensities[face.ordinal()] * 255));
		}
	}
	
	/**
	 * Adds a cube face to the model
	 * @param model The model to modify
	 * @param x The x block coordinate of the face
	 * @param y The y block coordinate of the face
	 * @param z The z block coordinate of the face
	 * @param face The specific face to draw.
	 * @param texCoords The texture coordinates of the face
	 */
	public static void addCubeFace(Model model, float x, float y, float z, Facing face, short[] texCoords, float intensity)
	{
		model.beginPoly();
		switch (face)
		{
			case NORTH:
				// North Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
			case WEST:
				// West Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3], intensity);
				break;
			case SOUTH:
				// South Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3], intensity);
				break;
			case EAST:
				// East Face
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
			case UP:
				// Up/Top Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3], intensity);
				break;
			case DOWN:
				// Down/Bottom Face
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
		}
		model.endPoly();
	}
	
	public static void addFluid(Model model, Chunk[] adjacentChunks, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
	{
		//Block adj = getAdjacentBlock(x, y, z, Facing.UP, adjacentChunks, chunk);
		//if (adj == Blocks.WATER || adj == Blocks.UPDATING_WATER)
		//	return;
		
		byte meta = chunk.getBlockMeta(x, y, z);
		// x = i % 3
		// z = i / 3
		
		// NW(0) N(1)  NE(2)
		//  W(3) -(4)  E (5)
		// SW(6) S(7)  SE(8)
		byte[] adjacentMetas = new byte[9];
		
		int level = 8 - (meta & BlockWater.DISTANCE);
		final double HEIGHT_VAL = ((15f / 8f) / 16f);
		
		for (int off = 0; off < 9; off++)
		{
			if (off == 4)
			{
				adjacentMetas[off] = (byte) (8 - (meta & BlockWater.DISTANCE));
				continue;
			}
			
			int xOff = (off % 3) - 1;
			int zOff = (off / 3) - 1;
			
			int adjacentX = chunk.chunkX * 16 + x + xOff;
			int adjacentY = chunk.chunkY * 16 + y;
			int adjacentZ = chunk.chunkZ * 16 + z + zOff;
			
			byte adjacentBlock = chunk.getBlock(x + xOff, y, z + zOff);
			byte adjacentMeta = chunk.getBlockMeta(x + xOff, y, z + zOff);
			
			if (adjacentBlock == -1)
			{
				// Check the nearby chunk for the appropriate block id & lighting
				adjacentBlock = chunk.world.getBlock(adjacentX, adjacentY, adjacentZ);
				adjacentMeta = chunk.world.getBlockMeta(adjacentX, adjacentY, adjacentZ);
			}
			
			Block adjacent = Block.idToBlock(adjacentBlock);
			if (adjacent != Blocks.WATER && adjacent != Blocks.UPDATING_WATER)
				adjacentMetas[off] = (byte)-1;
			else
				adjacentMetas[off] = (byte)(8 - (adjacentMeta & BlockWater.DISTANCE));
		}
		
		// NW(0) N(1)  NE(2)
		//  W(3) -(4)  E (5)
		// SW(6) S(7)  SE(8)
		float heightNW = 1f;
		float heightSW = 1f;
		float heightSE = 1f;
		float heightNE = 1f;
		
		Block adjUp = getAdjacentBlock(x, y, z, Facing.UP, adjacentChunks, chunk);
		if (adjUp != Blocks.WATER && adjUp != Blocks.UPDATING_WATER)
		{
			heightNW = (float)(tripleAverage(adjacentMetas[1], adjacentMetas[0], adjacentMetas[3], level) * HEIGHT_VAL); // N + NW + W
			heightSW = (float)(tripleAverage(adjacentMetas[7], adjacentMetas[6], adjacentMetas[3], level) * HEIGHT_VAL); // S + SW + W
			heightSE = (float)(tripleAverage(adjacentMetas[7], adjacentMetas[8], adjacentMetas[5], level) * HEIGHT_VAL); // S + SE + E
			heightNE = (float)(tripleAverage(adjacentMetas[1], adjacentMetas[2], adjacentMetas[5], level) * HEIGHT_VAL); // N + NE + E
		}
		
		for (Facing face : Facing.values())
		{
			int adjacentX = chunk.chunkX * 16 + x + face.getOffsetX();
			int adjacentY = chunk.chunkY * 16 + y + face.getOffsetY();
			int adjacentZ = chunk.chunkZ * 16 + z + face.getOffsetZ();
			
			Block adjacent = getAdjacentBlock(x, y, z, face, adjacentChunks, chunk);
			byte faceLight = chunk.world.getBlockLight(adjacentX, adjacentY, adjacentZ);
			
			if (adjacent.isSolid() && !adjacent.isTransparent())
			{
				// Don't add the face if the adjacent block is
				// solid and not transparent
				continue;
			}
			
			// Don't show the face if it's the same block
			if (!(face == Facing.UP && adjacent == Blocks.AIR) && !block.showFace(adjacent, face))
				continue;
			
			short[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			final float[] faceIntensities = new float[] { 0.75f, 0.75f, 0.75f, 0.75f, 0.95f, 0.55f };
			
			addFluidFace(model, x, y, z, heightNW, heightSW, heightSE, heightNE, face, texCoords, (byte)(((faceLight) / 15f) * faceIntensities[face.ordinal()] * 255));
		}
		
		/*
		model.beginPoly();
		model.addVertex(x + 0f, y + heightNW, z + 0f, texCoords[2], texCoords[1], 255);
		model.addVertex(x + 0f, y + heightSW, z + 1f, texCoords[0], texCoords[1], 255);
		model.addVertex(x + 1f, y + heightSE, z + 1f, texCoords[0], texCoords[3], 255);
		model.addVertex(x + 1f, y + heightNE, z + 0f, texCoords[2], texCoords[3], 255);
		model.endPoly();*/
		
		/*for (Facing face : Facing.values())
		{
			// If the specified face is -1, the face isn't supposed to be rendered
			if(faceTextures[face.ordinal()] == -1)
				continue;
			
			int adjacentX = chunk.chunkX * 16 + x + face.getOffsetX();
			int adjacentY = chunk.chunkY * 16 + y + face.getOffsetY();
			int adjacentZ = chunk.chunkZ * 16 + z + face.getOffsetZ();
			
			byte faceLight = chunk.world.getBlockLight(adjacentX, adjacentY, adjacentZ);
			byte adjacentBlock = chunk.getBlock(x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
			
			if (adjacentBlock == -1)
			{
				// Check the nearby chunk for the appropriate block id & lighting
				adjacentBlock = adjacentChunks[face.ordinal()].getBlock(adjacentX & 0xF, adjacentY & 0xF, adjacentZ & 0xF); //chunk.world.getBlock(adjacentX, adjacentY, adjacentZ);
			}
			
			Block adjacent = Block.idToBlock(adjacentBlock);
			if (adjacent.isSolid() && !adjacent.isTransparent())
			{
				// Don't add the face if the adjacent block is
				// solid and not transparent
				continue;
			}
			
			// Don't show the face if it's the same block
			if (!block.showFace(adjacent, face) && face != Facing.UP)
				continue;
			
			short[] texCoords = atlas.getPixelPositions(faceTextures[face.ordinal()]);
			final float[] faceIntensities = new float[] { 0.75f, 0.75f, 0.75f, 0.75f, 0.95f, 0.55f };
			
			BlockRenderer.addFluidFace(
					model,
					(float) (x),
					(float) (y),
					(float) (z),
					meta,
					face,
					texCoords,
					(byte)(((faceLight) / 15f) * faceIntensities[face.ordinal()] * 255));
		}*/
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
	
	public static void addFluidFace(Model model, float x, float y, float z, float nw, float sw, float se, float ne, Facing face, short[] texCoords, float intensity)
	{
		model.beginPoly();
		switch (face)
		{
			case NORTH:
				// North Face
				model.addVertex(x + 0.0f, y + nw, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + ne, z + 0.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
			case WEST:
				// West Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + sw, z + 1.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + nw, z + 0.0f, texCoords[0], texCoords[3], intensity);
				break;
			case SOUTH:
				// South Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + se, z + 1.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + sw, z + 1.0f, texCoords[0], texCoords[3], intensity);
				break;
			case EAST:
				// East Face
				model.addVertex(x + 1.0f, y + ne, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + se, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
			case UP:
				// Up/Top Face
				model.addVertex(x + 0.0f, y + nw, z + 0.0f, texCoords[2], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + sw, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 1.0f, y + se, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + ne, z + 0.0f, texCoords[2], texCoords[3], intensity);
				break;
			case DOWN:
				// Down/Bottom Face
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[3], intensity);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[3], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1], intensity);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1], intensity);
				break;
		}
		model.endPoly();
	}
	
	private static Block getAdjacentBlock(int x, int y, int z, Facing face, Chunk[] adjacentChunks, Chunk chunk)
	{
		int adjacentX = chunk.chunkX * 16 + x + face.getOffsetX();
		int adjacentY = chunk.chunkY * 16 + y + face.getOffsetY();
		int adjacentZ = chunk.chunkZ * 16 + z + face.getOffsetZ();
		
		byte adjacentBlock = chunk.getBlock(x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
		
		if (adjacentBlock == -1)
		{
			// Check the nearby chunk for the appropriate block id
			adjacentBlock = adjacentChunks[face.ordinal()].getBlock(adjacentX & 0xF, adjacentY & 0xF, adjacentZ & 0xF);
		}
		
		return Block.idToBlock(adjacentBlock);
	}
}
