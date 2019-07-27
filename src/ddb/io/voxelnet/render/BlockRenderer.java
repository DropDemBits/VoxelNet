package ddb.io.voxelnet.render;

import ddb.io.voxelnet.util.Facing;

public class BlockRenderer
{
	// No instance can be made
	private BlockRenderer() {}
	
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
