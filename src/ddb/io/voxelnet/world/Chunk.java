package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.client.render.RenderLayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Representation of a game chunk (16*16*16 chunk of tiles)
 */
public class Chunk
{
	public static final int LAYER_DATA_SIZE = 16;
	public static final int LIGHT_DATA_SIZE = 16 * 16 * 16;
	public static final int BLOCK_DATA_SIZE = 16 * 16 * 16;
	public static final int META_DATA_SIZE = (16/2) * 16 * 16;
	
	// Size of the fixed area in bytes
	// Save Format:
	// cX | cY | cZ | blockCount | blockLayers | blockLighting | blockData | blockMeta  ~ tickablesCount | tickables
	public static final int FIXED_SIZE =
			Integer.BYTES * 3 +             // Chunk Pos
			Short.BYTES +                   // blockCount
			LAYER_DATA_SIZE * Short.BYTES +
			LIGHT_DATA_SIZE * Byte.BYTES +
			BLOCK_DATA_SIZE * Byte.BYTES +
			META_DATA_SIZE  * Byte.BYTES +
			Short.BYTES; // for tickables count
	public static final int TICKPOS_BYTES = 3;
	
	public final int chunkX, chunkY, chunkZ;
	public World world;
	
	// Number of solid blocks on each layer
	private final short[] blockLayers = new short[LAYER_DATA_SIZE];
	// Block light & sky light data for each block
	private final byte[] lightData = new byte[LIGHT_DATA_SIZE];
	// The number of blocks in the chunk
	private short blockCount = 0;
	// Actual chunk data
	private final byte[] blockData = new byte[BLOCK_DATA_SIZE];
	// Block metadata (2 block clusters)
	private final byte[] blockMeta = new byte[META_DATA_SIZE];
	// If the chunk holds data (by default, they are empty)
	private boolean isEmpty = true;
	// If the chunk needs to be re-rendered (per-layer)
	private final boolean[] layerNeedsRebuild = new boolean[RenderLayer.values().length];
	// If the chunk needs to be saved to disk
	private boolean isDirty = false;
	// If the chunk was recently generated
	private boolean recentlyGenerated = true;
	
	public List<Integer> tickables = new ArrayList<>();
	
	/**
	 * Constructs a new chunk
	 * @param world The world associated with this chunk
	 * @param x The chunk x position
	 * @param y The chunk x position
	 * @param z The chunk z position
	 */
	public Chunk(World world, int x, int y, int z)
	{
		this.world = world;
		this.chunkX = x;
		this.chunkY = y;
		this.chunkZ = z;
		
		Arrays.fill(blockLayers, (byte)0);
		Arrays.fill(lightData, (byte)0xF0);
	}
	
	/**
	 * Deserialize a chunk from existing data
	 * @param blockData The chunk block data
	 * @param blockLights The chunk block light data
	 * @param blockMetas The chunk block meta data
	 * @param layerData The chunk layer data
	 * @param tickables The tickable blocks in the chunk
	 * @param blockCount The chunk's block count
	 */
	public void deserialize(byte[] blockData, byte[] blockLights, byte[] blockMetas,
	                        short[] layerData, int[] tickables, int blockCount)
	{
		this.blockCount = (short)blockCount;
		System.arraycopy(blockData, 0, this.blockData, 0, this.blockData.length);
		System.arraycopy(blockLights, 0, this.lightData, 0, this.lightData.length);
		System.arraycopy(blockMetas, 0, this.blockMeta, 0, this.blockMeta.length);
		System.arraycopy(layerData, 0, this.blockLayers, 0, this.blockLayers.length);
		
		// Add all the tickables
		for (int tickable : tickables)
			this.tickables.add(tickable);
		
		// Update the rebuild state
		forceLayerRebuild();
		
		// Most likely not empty
		isEmpty = false;
	}
	
	/**
	 * Gets the block data for the chunk
	 * The data is organized in a single dimension list, and is always accessed
	 * using the following formula:
	 * <code>x + z * 16 + y * (16 * 16)</code>
	 * @return The block data for this chunk
	 */
	public byte[] getData() { return blockData; }
	
	/**
	 * Gets the block meta data for the chunk
	 * The data is organized in a single dimension list, and is always accessed
	 * using the following formula:
	 * <code>(x >> 1) + (z * 8) + (y * 8 * 16)</code>
	 * with each of the nibbles making up two block's metadatas
	 * @return The block meta data for this chunk
	 */
	public byte[] getMetaData() { return blockMeta; }
	
	public short[] getLayerData() { return blockLayers; }
	
	/**
	 * Gets the per-block light data in the chunk
	 * @return The block light data of the chunk
	 */
	public byte[] getLightData() { return lightData; }
	
	/**
	 * Gets the number of blocks in the chunk
	 * @return The number of blocks in the chunk
	 */
	public short getBlockCount() { return blockCount; }
	
	/**
	 * Sets the block at the specified block position to the specified id
	 * If the id is equal to -1, nothing occurs
	 * @param x The x position of the block, in blocks
	 * @param y The y position of the block, in blocks
	 * @param z The z position of the block, in blocks
	 * @param id The id of the new block
	 */
	public void setBlock(int x, int y, int z, byte id)
	{
		// Check for out of bounds access or the out of bounds id
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16 || id == -1)
			return;
		
		int blockIndex = (y << 8) | (z << 4) | (x << 0);
		
		Block block = Block.idToBlock(id);
		Block lastBlock = Block.idToBlock(blockData[x + z * 16 + y * 256]);
		blockData[blockIndex] = id;
		
		// Update the dirty & rebuild states
		isDirty = true;
		
		if (block == Blocks.AIR)
		{
			forceLayerRebuild();
		}
		else
		{
			layerNeedsRebuild[block.getRenderLayer().ordinal()] = true;
			layerNeedsRebuild[RenderLayer.OPAQUE.ordinal()] = true;
		}
		
		// Update the block count & isEmpty
		if (lastBlock == Blocks.AIR && block != Blocks.AIR)
		{
			++blockCount;
			isEmpty = false;
		}
		else if (lastBlock != Blocks.AIR && block == Blocks.AIR)
		{
			--blockCount;
			
			if (blockCount < 0)
				blockCount = 0;
			
			if (blockCount == 0)
				isEmpty = true;
		}
		
		// Handle opaque block layer count
		if (lastBlock.isTransparent() && !block.isTransparent())
		{
			if (block.getRenderLayer() == RenderLayer.OPAQUE)
				++blockLayers[y];
			
			if (blockLayers[y] > 16 * 16)
				blockLayers[y] = 16 * 16;
		}
		else if (!lastBlock.isTransparent() && block.isTransparent())
		{
			if (lastBlock.getRenderLayer() == RenderLayer.OPAQUE)
				--blockLayers[y];
			
			if (blockLayers[y] < 0)
				blockLayers[y] = 0;
		}
		
		// Handle tickable updates
		if (!lastBlock.isTickable() && block.isTickable())
			tickables.add(blockIndex);
		else if (lastBlock.isTickable() && !block.isTickable())
			tickables.remove((Integer) blockIndex);
	}
	
	/**
	 * Gets the block id for the given position
	 * @param x The x position of the block, in blocks
	 * @param y The x position of the block, in blocks
	 * @param z The x position of the block, in blocks
	 * @return The id of the block, or -1 if the position is out of bounds
	 */
	public byte getBlock(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		return blockData[x + z * 16 + y * 256];
	}
	
	/**
	 * Gets the block light for the given position
	 *
	 * @param x The x position of the block, in blocks
	 * @param y The x position of the block, in blocks
	 * @param z The x position of the block, in blocks
	 * @return The light level of the block, as specified above, or -1 if the
	 *         block position is out of bounds
	 */
	public byte getBlockLight(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		
		// Bits
		// Y    | Z    | X
		// 0000 | 0000 | 0000
		//    8      4      0
		
		// Block light will be in the range of 0(darkest) - 15(brightest)
		byte blockLight = (byte)(lightData[(y << 8) | (z << 4) | (x << 0)] & 0x0F);
		
		return blockLight;
	}
	
	/**
	 * Gets the sky light for the given position
	 *
	 * @param x The x position of the block, in blocks
	 * @param y The x position of the block, in blocks
	 * @param z The x position of the block, in blocks
	 * @return The light level of the sky at the block's position,
	 *         or -1 if the block position is out of bounds
	 */
	public byte getSkyLight(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		
		// Bits
		// Y    | Z    | X
		// 0000 | 0000 | 0000
		//    8      4      0
		
		// Sky light will be in the range of 0(brightest) - 15(darkest)
		// Technically a shadow map, but whatever
		byte skyLight = (byte)((lightData[(y << 8) | (z << 4) | (x << 0)] & 0xF0) >> 4);
		
		return skyLight /*== 0 ? (byte)5 : (byte)15*/;
	}
	
	/**
	 * Sets the block light for the given position
	 * @param x The x position of the new block light
	 * @param y The y position of the new block light
	 * @param z The z position of the new block light
	 * @param amount The new block light value, between 15(brightest) - 0(darkest)
	 */
	public void setBlockLight(int x, int y, int z, byte amount)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		int lightIndex = (y << 8) | (z << 4) | (x << 0);
		byte light = (byte)(lightData[lightIndex] & 0xF0);
		byte lastLight = (byte)(lightData[lightIndex] & 0x0F);
		
		if (lastLight == amount)
			return;
		
		light |= (amount & 0xF);
		lightData[lightIndex] = light;
		
		forceLayerRebuild();
	}
	
	/**
	 * Sets the sky light for the given position
	 * @param x The x position of the new sky light
	 * @param y The y position of the new sky light
	 * @param z The z position of the new sky light
	 * @param amount The new sky light value, between 15(brightest) - 0(darkest)
	 */
	public void setSkyLight(int x, int y, int z, byte amount)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		int lightIndex = (y << 8) | (z << 4) | (x << 0);
		byte light = (byte)(lightData[lightIndex] & 0x0F);
		byte lastLight = (byte)((lightData[lightIndex] >> 4) & 0xF);
		
		if (lastLight == amount)
			return;
		
		light |= (amount & 0xF) << 4;
		lightData[lightIndex] = light;
		
		forceLayerRebuild();
	}
	
	// ???: Should there be a change to a flattened model? (i.e. 1 block-id = 1 state)
	/**
	 * Gets the block metadata for the given position
	 * @param x The x position to change
	 * @param y The y position to change
	 * @param z The z position to change
	 * @return The block metadata for the position
	 */
	public byte getBlockMeta(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return 0;
		
		int packed = Byte.toUnsignedInt(blockMeta[(x >> 1) + z * 8 + y * 8 * 16]);
		return (byte)((packed >> ((x & 1) * 4)) & 0x0F);
	}
	
	/**
	 * Sets the block metadata for the given position
	 * @param x The x position to change
	 * @param y The y position to change
	 * @param z The z position to change
	 * @param meta The new metadata value
	 */
	public void setBlockMeta(int x, int y, int z, byte meta)
	{
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		meta &= 0xF;
		
		int index = (x >> 1) + z * 8 + y * 8 * 16;
		byte mask = 0x0F;
		int shift = 0x00;
		
		if ((x & 1) != 0)
		{
			mask = (byte)0xF0;
			shift = ((x & 1) * 4);
		}
		
		blockMeta[index] &= ~mask;
		blockMeta[index] |= meta << shift;
	}
	
	/**
	 * Checks if the chunk is dirty and needs to be saved to disk
	 * @return True if the chunk needs to be saved
	 */
	public boolean isDirty()
	{
		return isDirty;
	}
	
	/**
	 * Makes the chunk clean
	 */
	public void makeClean()
	{
		isDirty = false;
	}
	
	/**
	 * Makes the chunk dirty
	 */
	public void makeDirty()
	{
		isDirty = true;
	}
	
	/**
	 * Checks if the chunk render layer needs to be rebuilt
	 * @return True if the chunk render layer needs to be rebuilt
	 */
	public boolean layerNeedsRebuild(RenderLayer layer)
	{
		return layerNeedsRebuild[layer.ordinal()];
	}
	
	/**
	 * Checks if the chunk needs to be re-rendered
	 * @return True if the chunk needs to be re-rendered
	 */
	public boolean needsRebuild()
	{
		for (RenderLayer layer : RenderLayer.values())
			if (layerNeedsRebuild[layer.ordinal()])
				return true;
		
		return false;
	}
	
	/**
	 * Resets the chunk rebuild status
	 */
	public void resetLayerRebuildStatus(RenderLayer layer)
	{
		layerNeedsRebuild[layer.ordinal()] = false;
	}
	
	/**
	 * Forces a chunk rebuild to occur
	 */
	public void forceLayerRebuild()
	{
		for (int i = 0; i < RenderLayer.values().length; i++)
			layerNeedsRebuild[i] = true;
	}
	
	/**
	 * Checks if the chunk is empty
	 * @return True if the chunk is empty
	 */
	public boolean isEmpty()
	{
		return isEmpty;
	}
	
	/**
	 * Checks if the chunk is recently generated
	 * @return True if the chunk was recently generated
	 */
	public boolean recentlyGenerated()
	{
		return recentlyGenerated;
	}
	
	/**
	 * Sets the chunk to be already generated
	 */
	public void setGenerated()
	{
		recentlyGenerated = false;
	}
	
	public void dbgPrint()
	{
		System.out.println("ChunkDump (" + chunkX + ", " + chunkY + ", " + chunkZ + ")");
		System.out.println("Bcnt " + blockCount);
		System.out.println("Empt " + isEmpty + " | Rbld " + Arrays.toString(layerNeedsRebuild) + " | Drty " + isDirty + " | Rgen " + recentlyGenerated);
	}
	
}
