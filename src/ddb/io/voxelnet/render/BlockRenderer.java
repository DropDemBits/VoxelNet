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
			
			int[] offset = face.getOffset();
			byte adjacentBlock = chunk.getBlock(x + offset[0], y + offset[1], z + offset[2]);
			
			if (adjacentBlock == -1)
			{
				// Check the nearby chunk for the appropriate block id
				adjacentBlock = chunk.world.getBlock(
						chunk.chunkX * 16 + x + offset[0],
						chunk.chunkY * 16 + y + offset[1],
						chunk.chunkZ * 16 + z + offset[2]);
			}
			
			Block adjacent = Block.idToBlock(adjacentBlock);
			if (adjacent.isSolid() && !adjacent.isTransparent())
			{
				// Don't add the face if the adjacent block is
				// solid and not transparent
				continue;
			}
			
			float[] texCoords = atlas.getPositions(faceTextures[face.ordinal()]);
			BlockRenderer.addCubeFace(
					model,
					(float) (chunk.chunkX * 16 + x),
					(float) (chunk.chunkY * 16 + y),
					(float) (chunk.chunkZ * 16 + z),
					face,
					texCoords);
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
	public static void addCubeFace(Model model, float x, float y, float z, Facing face, float[] texCoords)
	{
		model.beginPoly();
		switch (face)
		{
			case NORTH:
				// North Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
			case WEST:
				// West Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[0], texCoords[3]);
				break;
			case SOUTH:
				// South Face
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				break;
			case EAST:
				// East Face
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
			case UP:
				// Up/Top Face
				model.addVertex(x + 0.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[1]);
				model.addVertex(x + 0.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 1.0f, y + 1.0f, z + 0.0f, texCoords[2], texCoords[3]);
				break;
			case DOWN:
				// Down/Bottom Face
				model.addVertex(x + 1.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[3]);
				model.addVertex(x + 1.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[3]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 1.0f, texCoords[0], texCoords[1]);
				model.addVertex(x + 0.0f, y + 0.0f, z + 0.0f, texCoords[2], texCoords[1]);
				break;
		}
		model.endPoly();
	}
}
