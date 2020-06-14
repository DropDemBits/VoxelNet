package ddb.io.voxelnet.world;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.client.render.RenderLayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

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
	
	// Position of the chunk (in chunk coordinates)
	public final int chunkX, chunkY, chunkZ;
	// The chunk's associated world
	public World world;
	
	// Block light & sky light data for each block
	private final byte[] lightData = new byte[LIGHT_DATA_SIZE];
	// Actual chunk data
	private final byte[] blockData = new byte[BLOCK_DATA_SIZE];
	// Block metadata (2 block clusters)
	private final byte[] blockMeta = new byte[META_DATA_SIZE];
	
	// The number of blocks in the chunk
	private short blockCount = 0;
	// The number of active block lights in the chunk
	private short blockLightCount = 0;
	// The number of active sky lights in the chunk
	private short skyLightCount = 0;
	// Number of solid, opaque blocks on each layer
	private final short[] blockLayers = new short[LAYER_DATA_SIZE];
	
	// If the chunk needs to be re-rendered (per-layer)
	private final boolean[] layerNeedsRebuild = new boolean[RenderLayer.values().length];
	// If the chunk needs to be saved to disk
	private boolean isDirty = false;
	// If the chunk was recently loaded
	private boolean recentlyLoaded = true;
	// If the chunk is a placeholder until the real data arrives
	private boolean isPlaceholder = false;
	// If the column is to be or is already unloaded
	private boolean isUnloaded = false;
	
	// List of tickables (i.e blocks that have "isTickable" return true) contained within the chunk
	public List<Integer> tickables = new ArrayList<>();
	
	// Chunk field for the given chunk
	public final ChunkField chunkField;
	
	/**
	 * Constructs a new chunk
	 * @param world The world associated with this chunk
	 * @param x The chunk x position
	 * @param y The chunk x position
	 * @param z The chunk z position
	 */
	public Chunk(World world, int x, int y, int z)
	{
		this.chunkField = new ChunkField(this);
		this.world = world;
		this.chunkX = x;
		this.chunkY = y;
		this.chunkZ = z;
		
		Arrays.fill(blockLayers, (byte)0);
		Arrays.fill(lightData, (byte)0xF0);
	}
	
	/**
	 * Deserialize a chunk from existing data
	 * Block count, light count, and block layer count is derived from their respective arrays
	 *
	 * @param blockData The chunk block data
	 * @param lightData The chunk light data
	 * @param blockMetas The chunk block meta data
	 * @param tickables The tickable blocks in the chunk
	 */
	public void deserialize(byte[] blockData, byte[] lightData, byte[] blockMetas, int[] tickables)
	{
		System.arraycopy(blockData, 0, this.blockData, 0, this.blockData.length);
		System.arraycopy(lightData, 0, this.lightData, 0, this.lightData.length);
		System.arraycopy(blockMetas, 0, this.blockMeta, 0, this.blockMeta.length);
		
		// Information can be acquired at runtime
		this.blockCount      = (short)countAll (id -> id > 0, this.blockData);
		this.blockLightCount = (short)countAll(light -> ((light >> 0) & 0xF) > 0, this.lightData);
		this.skyLightCount   = (short)countAll(light -> ((light >> 4) & 0xF) < 15, this.lightData);
		
		// Fill the layer data
		for (int layer = 0; layer < 16; layer++)
			blockLayers[layer] = (short)countAll(
					id -> !Block.idToBlock(id).isTransparent(),
					this.blockData,
					layer * (16*16),
					(layer+1) * (16*16));
		
		// Add all the tickables
		for (int tickable : tickables)
			this.tickables.add(tickable);
		
		// Update the rebuild state
		forceLayerRebuild();
	}
	
	// Count all elements matching "matchAll" in the array
	private long countAll(IntPredicate matchAll, byte[] source)
	{
		return countAll(matchAll, source, 0, source.length);
	}
	
	// Count all elements matching "matchAll" in the given range
	private long countAll(IntPredicate matchAll, byte[] source, int startIndex, int endIndex)
	{
		return IntStream.range(startIndex, endIndex)
				.map(i -> Byte.toUnsignedInt(source[i]))
				.filter(matchAll)
				.count();
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
	 * Gets the block count in the given layer
	 * The y position is wrapped inside of the chunk
	 *
	 * @param y The y position (can be world or chunk block coordinates)
	 * @return The block count for the given layer
	 */
	public short getLayerCount(int y)
	{
		return blockLayers[y & 0xF];
	}
	
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
	public void setBlock(int x, int y, int z, int id)
	{
		// Check for out of bounds access or the out of bounds id
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16 || id == -1)
			return;
		
		// Can't store id's larger than 255
		if (id > 0xFF)
			return;
		
		int blockIndex = (y << 8) | (z << 4) | (x << 0);
		
		Block block = Block.idToBlock(id);
		Block lastBlock = Block.idToBlock(blockData[x + z * 16 + y * 256]);
		
		// Lossy/truncate down convert into an int
		blockData[blockIndex] = (byte)id;
		
		// Mark that the chunk now has been modified
		makeDirty();
		
		if (block == Blocks.AIR)
		{
			// Block changes to air always force an update of the model
			forceLayerRebuild();
		}
		else
		{
			layerNeedsRebuild[block.getRenderLayer().ordinal()] = true;
			layerNeedsRebuild[RenderLayer.OPAQUE.ordinal()] = true;
		}
		
		// Update the block count
		if (lastBlock == Blocks.AIR && block != Blocks.AIR)
			++blockCount;
		else if (lastBlock != Blocks.AIR && block == Blocks.AIR)
			--blockCount;
		
		assert blockCount >= 0 : "Bad block count!";
		
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
	public int getBlock(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		return Byte.toUnsignedInt(blockData[x + z * 16 + y * 256]);
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
	public int getBlockLight(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		
		// Bits
		// Y    | Z    | X
		// 0000 | 0000 | 0000
		//    8      4      0
		int blockIdx = (y << 8) | (z << 4) | (x << 0);
		
		// Block light will be in the range of 0(darkest) - 15(brightest)
		return (lightData[blockIdx] & 0x0F);
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
	public int getSkyLight(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return -1;
		
		// Bits
		// Y    | Z    | X
		// 0000 | 0000 | 0000
		//    8      4      0
		int blockIdx = (y << 8) | (z << 4) | (x << 0);
		
		// Sky light will be in the range of 0(darkest) - 15(brightest)
		return ((lightData[blockIdx] & 0xF0) >> 4);
	}
	
	/**
	 * Sets the block light for the given position
	 * @param x The x position of the new block light
	 * @param y The y position of the new block light
	 * @param z The z position of the new block light
	 * @param newBlockLight The new block light value, between 15(brightest) - 0(darkest)
	 */
	public void setBlockLight(int x, int y, int z, int newBlockLight)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		byte newLight = (byte)(Math.min(newBlockLight, 15));
		
		int blockIndex = (y << 8) | (z << 4) | (x << 0);
		int lastLight = (lightData[blockIndex] & 0x0F);
		
		if (lastLight == newLight)
			return;
		
		lightData[blockIndex] &= ~0x0F;
		lightData[blockIndex] |= (newLight & 0xF);
		
		// Update the sky light count on transitions to & from no block light
		if (lastLight == 0 && newLight > 0)
			++blockLightCount;
		else if (lastLight > 0 && newLight == 0)
			--blockLightCount;
		
		assert blockLightCount >= 0 : "Bad block light count!";
		
		// Trigger layer rebuild
		forceLayerRebuild();
	}
	
	/**
	 * Sets the sky light for the given position
	 * @param x The x position of the new sky light
	 * @param y The y position of the new sky light
	 * @param z The z position of the new sky light
	 * @param newSkylight The new sky light value, between 15(brightest) - 0(darkest)
	 */
	public void setSkyLight(int x, int y, int z, int newSkylight)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		byte newLight = (byte)(Math.min(newSkylight, 15));
		
		int blockIndex = (y << 8) | (z << 4) | (x << 0);
		int lastLight = (lightData[blockIndex] >> 4) & 0xF;
		
		if (lastLight == newLight)
			return;
		
		lightData[blockIndex] &= ~0xF0;
		lightData[blockIndex] |= (newLight & 0xF) << 4;
		
		// Update the sky light count on transitions to & from max sky light
		if (lastLight == 15 && newLight < 15)
			++skyLightCount;
		else if (lastLight < 15 && newLight == 15)
			--skyLightCount;
		
		assert skyLightCount >= 0 : "Bad skylight count!";
		
		// Trigger layer rebuild
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
	public int getBlockMeta(int x, int y, int z)
	{
		// Check for out of bounds access
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return 0;
		
		int packed = Byte.toUnsignedInt(blockMeta[(x >> 1) + z * 8 + y * 8 * 16]);
		return ((packed >> ((x & 1) * 4)) & 0x0F);
	}
	
	/**
	 * Sets the block metadata for the given position
	 * @param x The x position to change
	 * @param y The y position to change
	 * @param z The z position to change
	 * @param meta The new metadata value
	 */
	public void setBlockMeta(int x, int y, int z, int meta)
	{
		if (x < 0 || y < 0 || z < 0 || x >= 16 || y >= 16 || z >= 16)
			return;
		
		meta &= 0xF;
		
		int index = (x >> 1) + z * 8 + y * 8 * 16;
		byte mask = 0x0F;
		int shift = 0x00;
		
		// Change mask to access the right metadata value
		if ((x & 1) != 0)
		{
			mask = (byte)0xF0;
			shift = ((x & 1) * 4);
		}
		
		blockMeta[index] &= ~mask;
		blockMeta[index] |= (byte)(meta << shift);
	}
	
	//////// Flags Galore! ////////
	
	/**
	 * Checks if the chunk is dirty and needs to be saved to disk after purging from the chunk cache
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
	 * Makes the chunk dirty, indicating that it needs to be saved if purged from the chunk cache
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
	 * Checks if the chunk has no blocks
	 * @return True if the chunk does not have any blocks
	 */
	public boolean hasNoBlocks()
	{
		return blockCount <= 0;
	}
	
	/**
	 * Checks if the chunk is completely empty
	 * @return True if the chunk has no datais
	 */
	public boolean isEmpty()
	{
		return blockCount <= 0 && blockLightCount <= 0 && skyLightCount <= 0;
	}
	
	/**
	 * Checks if the chunk is recently loaded (i.e. recently generated, recently loaded from cache)
	 * @return True if the chunk was recently loaded
	 */
	public boolean isRecentlyLoaded()
	{
		return recentlyLoaded;
	}
	
	/**
	 * Sets the chunk to be already loaded
	 */
	public void setPreviouslyLoaded()
	{
		recentlyLoaded = false;
	}
	
	/**
	 * Remarks the chunk as being recently loaded
	 * Used to re-associate a model on the client side
	 */
	public void setRecentlyLoaded()
	{
		recentlyLoaded = true;
	}
	
	/**
	 * Marks the chunk as a placeholder
	 *
	 * A chunk is only marked a placeholder if a "getChunk" is made and a request
	 * for a chunk from the chunk source (e.g. the server, chunk cache, disk)
	 * has been requested, but has not been fulfilled yet.
	 */
	public void markPlaceholder()
	{
		isPlaceholder = true;
	}
	
	/**
	 * Marks the chunk as not a placeholder
	 *
	 * This action is only performed if a request for the chunk has been
	 * fulfilled but the chunk data is empty. As it is assumed, that no
	 * modifications to the chunk data have been performed, removing
	 * the status as a placeholder chunk is the same action as creating a
	 * new empty chunk.
	 */
	public void markNotPlaceholder()
	{
		isPlaceholder = false;
	}
	
	/**
	 * Checks if the current chunk is marked as a placeholder
	 * @return True if the chunk has been marked as a placeholder
	 */
	public boolean isPlaceholder()
	{
		return isPlaceholder;
	}
	
	public void dbgPrint()
	{
		System.out.println("ChunkDump (" + chunkX + ", " + chunkY + ", " + chunkZ + ")");
		System.out.println("Bcnt " + blockCount);
		System.out.println("Rbld " + Arrays.toString(layerNeedsRebuild) + " | Drty " + isDirty + " | Rgen " + recentlyLoaded);
	}
	
	/**
	 * Mark a chunk for unloading
	 */
	public void markUnloaded()
	{
		isUnloaded = true;
	}
	
	/**
	 * Mark a chunk for not being unloaded
	 */
	public void markLoaded()
	{
		isUnloaded = false;
	}
	
	/**
	 * Check if the chunk is unloaded
	 * @return True if the chunk is unloaded
	 */
	public boolean isUnloaded()
	{
		return isUnloaded;
	}
	
}
