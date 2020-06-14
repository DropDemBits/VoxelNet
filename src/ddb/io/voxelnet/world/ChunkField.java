package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.Facing;

import java.util.Arrays;

/**
 * A ChunkField is a 3x3x3 volume of the central chunk's surrounding chunks
 *
 * The chunk field also contains the central chunk to ease fetching of adjacent chunks
 */
public class ChunkField
{
	// If the field does not contain other chunks (aside from the reference chunk)
	private boolean isEmptyField = true;
	// Chunks surrounding the reference chunk
	private final Chunk[] adjacentField = new Chunk[3*3*3];
	// The reference chunk
	private final Chunk referenceChunk;
	
	/**
	 * Creates a new chunk field centred around the given reference chunk
	 * @param chunk The chunk to have as the reference
	 */
	public ChunkField(Chunk chunk)
	{
		this.referenceChunk = chunk;
	}
	
	/**
	 * Rebuilds the chunk field from the given reference chunk
	 */
	public void rebuildField()
	{
		// Update the adjacency field
		for(int i = 0; i < adjacentField.length; i++)
		{
			int xOff = (i % 3) - 1;
			int zOff = ((i / 3) % 3) - 1;
			int yOff = (i / 9) - 1;
			
			int aI = toAdjacentIndex(xOff, yOff, zOff);
			
			adjacentField[aI] = referenceChunk.world.getChunk(
					referenceChunk.chunkX + xOff,
					referenceChunk.chunkY + yOff,
					referenceChunk.chunkZ + zOff
			);
		}
		
		isEmptyField = false;
	}
	
	/**
	 * Rebuild the field neighbor's chunk field
	 */
	public void rebuildNeighborFields()
	{
		// Do nothing for an empty field
		if (isEmptyField)
			return;
		
		for (Chunk neighbor : adjacentField)
		{
			if (neighbor == referenceChunk || neighbor == referenceChunk.world.chunkManager.EMPTY_CHUNK)
				continue;
			
			neighbor.chunkField.rebuildField();
		}
	}
	
	/**
	 * Clears the chunk field, except for the reference position
	 */
	public void clearField()
	{
		Arrays.fill(adjacentField, null);
		adjacentField[toAdjacentIndex(0, 0, 0)] = referenceChunk;
	}
	
	/**
	 * Gets a chunk inside the given chunk field
	 * @param x The chunk block x coordinate, relative to the reference chunk
	 * @param y The chunk block y coordinate, relative to the reference chunk
	 * @param z The chunk block z coordinate, relative to the reference chunk
	 * @param face The offset to apply to the position
	 * @return The chunk in the chunk field containing the chunk block position
	 */
	public Chunk getChunk(int x, int y, int z, Facing face)
	{
		// Positions relative to the current chunk
		int relX = x + face.getOffsetX();
		int relY = y + face.getOffsetY();
		int relZ = z + face.getOffsetZ();
		
		// Offsets to the target chunk
		int chunkX = getAdjacentOffset(relX);
		int chunkY = getAdjacentOffset(relY) * 9;
		int chunkZ = getAdjacentOffset(relZ) * 3;
		
		// Chunks are ordered (from the most to least significant coordinate) as YZX
		
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
		
		return adjacentField[index];
	}
	
	/**
	 * Gets an adjacent chunk in the specified direction
	 * @param dir The direction to get the adjacent chunk for
	 * @return The adjacent chunk
	 */
	public Chunk getAdjacentChunk(Facing dir)
	{
		return adjacentField[toAdjacentIndex(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ())];
	}
	
	/**
	 * Gets the block within the given adjacency field
	 * @param x The chunk block x coordinate, relative to the reference chunk
	 * @param y The chunk block y coordinate, relative to the reference chunk
	 * @param z The chunk block z coordinate, relative to the reference chunk
	 * @param off The offset for the block coordinate
	 * @return
	 */
	public Block getBlock(int x, int y, int z, Facing off)
	{
		return Block.idToBlock(getChunk(x, y, z, off).getBlock(
				(x + off.getOffsetX()) & 0xF,
				(y + off.getOffsetY()) & 0xF,
				(z + off.getOffsetZ()) & 0xF));
	}
	
	/**
	 * Gets the block meta within the given adjacency field
	 * @param x The chunk block x coordinate, relative to the reference chunk
	 * @param y The chunk block y coordinate, relative to the reference chunk
	 * @param z The chunk block z coordinate, relative to the reference chunk
	 * @param off The offset for the block coordinate
	 * @return The block meta for the appropriate block
	 */
	public int getMeta(int x, int y, int z, Facing off)
	{
		return getChunk(x, y, z, off).getBlockMeta(
				(x + off.getOffsetX()) & 0xF,
				(y + off.getOffsetY()) & 0xF,
				(z + off.getOffsetZ()) & 0xF);
	}
	
	/**
	 * Gets the block light within the given adjacency field
	 * @param x The chunk block x coordinate, relative to the reference chunk
	 * @param y The chunk block y coordinate, relative to the reference chunk
	 * @param z The chunk block z coordinate, relative to the reference chunk
	 * @param off The offset for the block coordinate
	 * @return The block light for the appropriate block position
	 */
	public int getBlockLight(int x, int y, int z, Facing off)
	{
		return getChunk(x, y, z, off).getBlockLight(
				(x + off.getOffsetX()) & 0xF,
				(y + off.getOffsetY()) & 0xF,
				(z + off.getOffsetZ()) & 0xF);
	}
	
	/**
	 * Gets the sky light within the given adjacency field
	 * @param x The chunk block x coordinate, relative to the reference chunk
	 * @param y The chunk block y coordinate, relative to the reference chunk
	 * @param z The chunk block z coordinate, relative to the reference chunk
	 * @param off The offset for the block coordinate
	 * @return The sky light for the appropriate block position
	 */
	public int getSkyLight(int x, int y, int z, Facing off)
	{
		return getChunk(x, y, z, off).getSkyLight(
				(x + off.getOffsetX()) & 0xF,
				(y + off.getOffsetY()) & 0xF,
				(z + off.getOffsetZ()) & 0xF);
	}
	
	/**
	 * Converts a three coordinate offset into an adjacent index
	 * @param xOff The x offset to the adjacent position
	 * @param yOff The y offset to the adjacent position
	 * @param zOff The z offset to the adjacent position
	 * @return The adjacent index
	 */
	private static int toAdjacentIndex(int xOff, int yOff, int zOff)
	{
		return (xOff + 1) + (zOff + 1) * 3 + (yOff + 1) * 9;
	}
	
	// Computes a selection offset used in getChunk, using a chunk relative block coordinate
	private static int getAdjacentOffset(int coordinate)
	{
		// Use bitshifts as division doesn't work with values between -15 to 15
		// as integer division rounds towards zero
		int chunkCoord = coordinate >> 4;
		
		if (chunkCoord < 0)
			return 0;   // Towards negative, -1
		if (chunkCoord > 0)
			return 2;   // Towards positive, +1
		
		return 1;       // Same column / row
	}
	
}
