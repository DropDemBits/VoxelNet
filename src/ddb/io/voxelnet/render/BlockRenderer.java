package ddb.io.voxelnet.render;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.world.Chunk;

public class BlockRenderer
{
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
	public static void addCube(Model model, Chunk chunk, Block block, int x, int y, int z, int[] faceTextures, TextureAtlas atlas)
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
				adjacentBlock = chunk.world.getBlock(adjacentX, adjacentY, adjacentZ);
			}
			
			Block adjacent = Block.idToBlock(adjacentBlock);
			if (adjacent.isSolid() && !adjacent.isTransparent())
			{
				// Don't add the face if the adjacent block is
				// solid and not transparent
				continue;
			}
			
			// Don't show the face if it's the same block
			if (adjacent == block)
				continue;
			
			float[] texCoords = atlas.getPositions(faceTextures[face.ordinal()]);
			final float[] faceIntensities = new float[] { 0.85f, 0.85f, 0.85f, 0.85f, 0.95f, 0.75f };
			BlockRenderer.addCubeFace(
					model,
					(float) (chunk.chunkX * 16 + x),
					(float) (chunk.chunkY * 16 + y),
					(float) (chunk.chunkZ * 16 + z),
					face,
					texCoords,
					((faceLight + 1f) / 16f) * faceIntensities[face.ordinal()]);
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
	public static void addCubeFace(Model model, float x, float y, float z, Facing face, float[] texCoords, float intensity)
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
}
