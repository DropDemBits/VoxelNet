package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.Entity;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.fluid.FluidInstance;
import ddb.io.voxelnet.util.AABBCollider;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.RaycastResult;
import ddb.io.voxelnet.util.Vec3i;
import org.joml.Vector3d;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class World
{
	// Sidedness
	// True if the world is on the client side
	public final boolean isClient;
	
	// The chunk manager
	public final ChunkManager chunkManager;
	
	private float accumulatedWorldTick;
	
	// List of entities that are waiting to be added
	public final List<Entity> pendingEntities;
	// List of currently loaded entities
	public final List<Entity> loadedEntities;
	
	private Queue<LightUpdate> pendingLightRemoves;
	private Queue<LightUpdate> pendingLightUpdates;
	private Queue<LightUpdate> pendingShadowRemoves;
	public Queue<LightUpdate> pendingShadowUpdates;
	
	// Fluid instances
	private Map<Fluid, FluidInstance> fluidInstances;
	// TODO: Add tick schedules
	private int[] fluidTickSchedules = new int[Fluid.ALL_FLUIDS.length];
	
	// WorldGen
	private long worldSeed;
	public final Random worldRandom;
	
	public World(boolean isClient)
	{
		this.isClient = isClient;
		
		//worldSeed = System.currentTimeMillis();
		//worldSeed = 1566757735901L;
		worldSeed = 1566847034636L;
		worldRandom = new Random(worldSeed);
		loadedEntities = new ArrayList<>();
		pendingEntities = new ArrayList<>();
		
		pendingLightUpdates = new ConcurrentLinkedQueue<>();
		pendingLightRemoves = new ConcurrentLinkedQueue<>();
		
		pendingShadowUpdates = new ConcurrentLinkedQueue<>();
		pendingShadowRemoves = new ConcurrentLinkedQueue<>();
		
		fluidInstances = new LinkedHashMap<>();
		
		for (int i = 0; i < Fluid.ALL_FLUIDS.length; i++)
		{
			Fluid fluid = Fluid.ALL_FLUIDS[i];
			fluidInstances.put(fluid, new FluidInstance(fluid));
			fluidTickSchedules[i] = fluid.updateRate;
		}
		
		// TODO: Request dynamic client chunk loads
		if (isClient)
			chunkManager = new ClientChunkManager(this);
		else
			chunkManager = new ChunkManager(this);
		
		setWorldSeed(worldSeed);
	}
	
	/**
	 * Generates a set of chunks
	 */
	public void generate()
	{
		System.out.println("Generating world with seed " + worldSeed);
		
		long startGen = System.currentTimeMillis();
		final int radius = 4;
		
		for (int cx = -radius; cx <= radius; cx++)
		{
			for (int cz = -radius; cz <= radius; cz++)
			{
				chunkManager.generateChunk(cx, cz);
			}
		}
		System.out.println("Done terrain generation in " + (System.currentTimeMillis() - startGen) + " ms");
		
		// Old explosion generation
		/*int explosionCount = worldRandom.nextInt(100) + 50;
		for (int i = 0; i < explosionCount; i++)
		{
			int x = worldRandom.nextInt(256) - 128;
			int y = worldRandom.nextInt(64);
			int z = worldRandom.nextInt(256) - 128;
			int radius = worldRandom.nextInt(20) + 5;
			
			explode(x, y, z, radius);
		}*/
		
		startGen = System.currentTimeMillis();
		processLightUpdate();
		System.out.println("Done light generation in " + (System.currentTimeMillis() - startGen) + " ms");
	}
	
	public void setWorldSeed(long newSeed)
	{
		worldSeed = newSeed;
		worldRandom.setSeed(newSeed);
		chunkManager.perlinNoise.seed(worldSeed);
	}
	
	public long getWorldSeed()
	{
		return worldSeed;
	}
	
	public int getColumnHeight(int x, int y, int z)
	{
		if (y >= 256)
			return y;
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockZ = z & 0xF;
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkManager.getColumn(chunkX, chunkZ);
		if (column == null)
			return 0;
		
		return column.opaqueColumns[blockX + blockZ * 16];
	}
	
	public boolean canBlockSeeSky(int x, int y, int z)
	{
		if (y >= 256)
			return true;
		
		int chunkX = x >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockZ = z & 0xF;
		
		// Check the ChunkColumn for access to the sky
		ChunkColumn column = chunkManager.getColumn(chunkX, chunkZ);
		boolean canSeeSky;
		
		if (column == null)
			canSeeSky = true; // If a column is missing, the blocks can definitely see the sky
		else if(y >= 0)
			canSeeSky = y > Byte.toUnsignedInt(column.opaqueColumns[blockX + blockZ * 16]); // Can't see at the opaque
		else
			canSeeSky = Byte.toUnsignedInt(column.opaqueColumns[blockX + blockZ * 16]) == 0; // If 0 (column empty), can see the sky
		
		return canSeeSky;
	}
	
	public boolean canBlockDirectlySeeSky(int x, int y, int z)
	{
		if (y >= 256)
			return true;
		
		if (!canBlockSeeSky(x, y, z))
			return false;
		
		for (int checkY = y+1; checkY < 256; checkY++)
		{
			if (getBlock(x, checkY, z) != Blocks.AIR)
				return false;
		}
		
		return true;
	}
	
	/**
	 * Gets the sky light at the given position
	 * Note:
	 * 15 indicates the maximum amount of skylight, 0 indicates the least
	 * amount
	 *
	 * @param x The x position to fetch
	 * @param y The y position to fetch
	 * @param z The z position to fetch
	 * @return The sky light value
	 */
	public byte getSkyLight(int x, int y, int z)
	{
		if (y < 0)
		{
			// If the position can see the sky, full skylight
			return canBlockSeeSky(x, y, z) ? (byte)15 : (byte)0;
		}
		if (y >= 256)
			return 15;
		
		int chunkX = x >> 4;
		int chunkY = y >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Technically a shadow map, but whatever
		return chunkManager.getChunk(chunkX, chunkY, chunkZ, false).getSkyLight(blockX, blockY, blockZ);
	}
	
	/**
	 * Gets the block light at the given position
	 * @param x The x position to fetch
	 * @param y The y position to fetch
	 * @param z The z position to fetch
	 * @return The block light value
	 */
	public byte getBlockLight(int x, int y, int z)
	{
		if (y < 0)
			return 0;
		if (y >= 256)
			return 15;
		
		int chunkX = x >> 4;
		int chunkY = y >> 4;
		int chunkZ = z >> 4;
		
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		return chunkManager.getChunk(chunkX, chunkY, chunkZ, false).getBlockLight(blockX, blockY, blockZ);
	}
	
	private void setBlockLight(int x, int y, int z, byte newLight)
	{
		if (newLight < 0)
			return;
		
		Chunk chunk = chunkManager.getChunk(x >> 4, y >> 4, z >> 4, newLight > 0);
		chunk.setBlockLight(x & 0xF, y & 0xF, z & 0xF, newLight);
	}
	
	private void setSkyLight(int x, int y, int z, byte newLight)
	{
		if (newLight > 15 || newLight < 0)
			return;
		
		Chunk chunk = chunkManager.getChunk(x >> 4, y >> 4, z >> 4, newLight != 0);
		chunk.setSkyLight(x & 0xF, y & 0xF, z & 0xF, newLight);
	}
	
	/**
	 * Gets the block at the specified position
	 * If the position is not inside of a loaded chunk, air (0) is returned
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @return The specified block, or Blocks.VOID if
	 * the position is out of bounds
	 */
	public Block getBlock (int x, int y, int z)
	{
		if (y < 0)
			return Blocks.VOID;
		
		Chunk chunk = chunkManager.getChunk(x >> 4, y >> 4, z >> 4, false);
		
		return Block.idToBlock(chunk.getBlock(x & 0xF, y & 0xF, z & 0xF));
	}
	
	/**
	 * Sets the block id at the given position
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to place
	 */
	public void setBlock (int x, int y, int z, Block block)
	{
		setBlock(x, y, z, block, (byte)0, 7);
	}
	
	/**
	 * Sets the block id at the given position, with some metadata
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to place
	 * @param meta The metadata of the block
	 */
	public void setBlock (int x, int y, int z, Block block, byte meta)
	{
		setBlock(x, y, z, block, meta, 7);
	}
	
	/**
	 * Sets the block id at the given position, with some metadata and flags
	 * The flags are defined to be as follows:
	 * Bit 0: When set, updates the lighting
	 * Bit 1: When set, updates the adjacent chunks
	 * Bit 2: When set, updates the adjacent neighbors
	 * Bit 3: When set, doesn't update the opaque chunk column
	 *
	 * @param x The x position of the block
	 * @param y The y position of the block
	 * @param z The z position of the block
	 * @param block The block to place
	 * @param meta The metadata of the block
	 * @param flags The bitmap of the flags to use
	 */
	public void setBlock (int x, int y, int z, Block block, byte meta, int flags)
	{
		// Decode the flags
		boolean updateLighting  = (flags & 1) != 0;
		boolean updateNeighborChunks = (flags & 2) != 0;
		boolean updateNeighbors = (flags & 4) != 0;
		boolean updateColumns   = (flags & 8) == 0;
		
		// If the light update was the result of the tallest block moving down
		boolean tallestDown = false;
		// Last block light
		int lastBlockLight;
		int lastSkyLight;
		
		// Don't set block below or above the world
		if (y < 0 || y > 255)
			return;
		
		// Do nothing for void
		if (block == Blocks.VOID)
			return;
		
		Vec3i chunkPos = new Vec3i(x >> 4, y >> 4, z >> 4);
		Chunk chunk = chunkManager.getChunk(chunkPos, block != Blocks.AIR);
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Chunk column index
		int columnIdx = blockX + blockZ * 16;
		
		// Set the block, block light & meta
		lastBlockLight = chunk.getBlockLight(blockX, blockY, blockZ);
		lastSkyLight = chunk.getSkyLight(blockX, blockY, blockZ);
		
		chunk.setBlock(blockX, blockY, blockZ, block.getId());
		chunk.setBlockMeta(blockX, blockY, blockZ, meta);
		
		// Only update the current light value if
		// - The new block is air
		// - The new block is not transparent
		// - The new block's light is not the same as the old one
		if (block == Blocks.AIR || !block.isTransparent() || block.getBlockLight() != lastBlockLight)
			chunk.setBlockLight(blockX, blockY, blockZ, block.getBlockLight());
		
		// Update the chunk column
		ChunkColumn chunkColumn = chunkManager.getColumn(x >> 4, z >> 4);
		
		if (chunkColumn == null)
		{
			if (block == Blocks.AIR)
				return; // Don't need to add a column if air is being placed
			
			// Force load the column (should have been loaded with the chunks)
			chunkColumn = chunkManager.loadColumn(x >> 4, z >> 4);
		}
		
		int oldestHeight = Byte.toUnsignedInt(chunkColumn.opaqueColumns[columnIdx]);
		
		if (updateLighting && updateColumns)
		{
			// Update the appropriate column
			int tallestOpaque = Byte.toUnsignedInt(chunkColumn.opaqueColumns[columnIdx]);
			
			// 3 Main Groups for Column placement
			// - Below tallest opaque block
			// - At tallest opaque block
			// - Above tallest opaque block
			// If the block is below the tallest opaque block, then do nothing
			// If the block is above the tallest block and is opaque, make this
			//    block the tallest one, otherwise do nothing
			// If the block is at the tallest opaque block and is not opaque (i.e
			//    air or isTransparent), search for the next opaque block below.
			//    Otherwise, nothing happens
			// If no match is found, set the tallest opaque block to zero.
			
			if (y > tallestOpaque)
			{
				// Only do update for opaque blocks
				if (!block.isTransparent())
				{
					chunkColumn.opaqueColumns[columnIdx] = (byte) y;
					tallestDown = false;
				}
			}
			
			if (y == tallestOpaque && block.isTransparent())
			{
				// Only update on same height if the block is not opaque or a search should be performed
				int height = y - 1;
				
				for (; height != y; height = (--height) & 0xFF)
				{
					if (!getBlock(x, height, z).isTransparent())
						break;
				}
				
				// If the height is the same, make the column empty
				if (height == y)
					height = 0;
				
				chunkColumn.opaqueColumns[columnIdx] = (byte) height;
				tallestDown = true;
			}
		}
		
		if (updateLighting)
		{
			// Only check for unrestricted skylight
			boolean skyAvailable = canBlockDirectlySeeSky(x, y, z);
			
			// Was doing: Fixing issues with opacity propagation
			
			// Check needs to be done so that opacity is correctly applied
			if (block == Blocks.AIR && skyAvailable)
			{
				// If the block can see the sky, and the tallest block has moved down, set it to the maximum light value
				chunk.setSkyLight(blockX, blockY, blockZ, (byte) 15);
				pendingShadowUpdates.add(new LightUpdate(new Vec3i(x, tallestDown ? oldestHeight : y, z), (byte) 0));
				
				if (Game.showThings)
					System.out.println("ho boi! " + tallestDown + ", " + oldestHeight + ", " + y);
			}
			else
			{
				if ((!block.isTransparent()) || block.getOpacity() > 0)
				{
					if (lastSkyLight != 0)
					{
						// Remove light
						chunk.setSkyLight(blockX, blockY, blockZ, (byte) 0);
						pendingShadowRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte) lastSkyLight));
					}
				}
				else //if (!skyAvailable)
				{
					// "Remove" the light to find the closest one
					pendingShadowRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte) 0));
				}
			}
			
			// If the new block:
			// - Is air
			// - Is not transparent
			// - Has a smaller block light
			// set it up for removal
			if (block == Blocks.AIR || !block.isTransparent() || block.getBlockLight() < lastBlockLight)
				pendingLightRemoves.add(new LightUpdate(new Vec3i(x, y, z), (byte)lastBlockLight));
			else
				pendingLightUpdates.add(new LightUpdate(new Vec3i(x, y, z), (byte)0));
		}
		
		if (updateNeighborChunks)
		{
			// Update the directly adjacent chunks
			updateNeighboringChunks(chunkPos, blockX, blockY, blockZ);
		}
		
		if (updateNeighbors)
		{
			// Update the neighboring blocks
			for (Facing face : Facing.values())
			{
				Block neighbor = getBlock(x + face.getOffsetX(), y + face.getOffsetY(), z + face.getOffsetZ());
				
				// Update the neighbor if it's not air
				if (neighbor != Blocks.AIR)
					neighbor.onNeighborUpdated(
							this,
							x + face.getOffsetX(),
							y + face.getOffsetY(),
							z + face.getOffsetZ(),
							face.getOpposite());
			}
		}
	}
	
	/**
	 * Updates the adjacent chunks from the given chunk block position
	 * @param sourceChunk The chunk to check the sources from
	 * @param blockX The block's x position in the bounds of the chunk
	 * @param blockY The block's y position in the bounds of the chunk
	 * @param blockZ The block's z position in the bounds of the chunk
	 */
	private void updateNeighboringChunks(Vec3i sourceChunk, int blockX, int blockY, int blockZ)
	{
		if (blockX ==  0) chunkManager.getChunk(sourceChunk.add(-1,  0,  0), false).forceLayerRebuild();
		if (blockZ == 15) chunkManager.getChunk(sourceChunk.add( 0,  0,  1), false).forceLayerRebuild();
		if (blockY ==  0) chunkManager.getChunk(sourceChunk.add( 0, -1,  0), false).forceLayerRebuild();
		if (blockY == 15) chunkManager.getChunk(sourceChunk.add( 0,  1,  0), false).forceLayerRebuild();
		if (blockX == 15) chunkManager.getChunk(sourceChunk.add( 1,  0,  0), false).forceLayerRebuild();
		if (blockZ ==  0) chunkManager.getChunk(sourceChunk.add( 0,  0, -1), false).forceLayerRebuild();
	}
	
	/**
	 * Sets the block metadata for the given position
	 * @param x The x position of the target block
	 * @param y The y position of the target block
	 * @param z The z position of the target block
	 * @param meta The new metadata for the block
	 */
	public void setBlockMeta (int x, int y, int z, byte meta)
	{
		// Don't set block below or above the world
		if (y < 0 || y > 255)
			return;
		
		Chunk chunk = chunkManager.getChunk(x >> 4, y >> 4, z >> 4, false);
		
		if (chunk == chunkManager.EMPTY_CHUNK)
			return;
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Set the block meta
		chunk.setBlockMeta(blockX, blockY, blockZ, meta);
	}
	
	/**
	 * Gets the block metadata for the given position
	 * @param x The x position of the target block
	 * @param y The y position of the target block
	 * @param z The z position of the target block
	 * @return  The metadata for the block position
	 */
	public byte getBlockMeta (int x, int y, int z)
	{
		// Don't fetch data below or above the world
		if (y < 0 || y > 255)
			return 0;
		
		Chunk chunk = chunkManager.getChunk(x >> 4, y >> 4, z >> 4, false);
		
		if (chunk == chunkManager.EMPTY_CHUNK)
			return 0;
		
		// Block positions within the chunk
		int blockX = x & 0xF;
		int blockY = y & 0xF;
		int blockZ = z & 0xF;
		
		// Get the block meta
		return chunk.getBlockMeta(blockX, blockY, blockZ);
	}
	
	/**
	 * Performs a step-by-step raycast to find a selectable block
	 * @param direction The direction of the ray
	 * @param startX The starting x position of the ray
	 * @param startY The starting y position of the ray
	 * @param startZ The starting z position of the ray
	 * @param range The range of the ray
	 * @return The raycast result of the selectable block, or
	 *         RaycastResult.NO_RESULT if none was found
	 */
	public RaycastResult blockRaycast(Vector3d direction, double startX, double startY, double startZ, int range)
	{
		// From https://gamedev.stackexchange.com/questions/47362/cast-ray-to-select-block-in-voxel-game/49423#49423
		double dx = direction.x, dy = direction.y, dz = direction.z;
		
		int blockX = (int)Math.floor(startX), blockY = (int)Math.floor(startY), blockZ = (int)Math.floor(startZ);
		
		// Orthogonal step
		int stepX = (int)Math.signum(dx), stepY = (int)Math.signum(dy), stepZ = (int)Math.signum(dz);
		
		// Calculate the initial max t's
		// t = p_inv(p) = (p(t) - p_0) / dp
		double tMaxX = intBound(startX, dx);
		double tMaxY = intBound(startY, dy);
		double tMaxZ = intBound(startZ, dz);
		
		// Calculate the delta t's
		// dt = (1 - 0) / dp
		double tDeltaX = stepX / dx;
		double tDeltaY = stepY / dy;
		double tDeltaZ = stepZ / dz;
		
		// Scale range to allow direct comparison between 't' values
		double radius = range / Math.sqrt(dx*dx + dy*dy + dz*dz);
		
		Facing hitFace = Facing.UP;
		
		double rayOffX = 0;
		double rayOffY = 0;
		double rayOffZ = 0;
		
		// Step while in range
		while (true)
		{
			Block block = getBlock(blockX, blockY, blockZ);
			
			if (block != Blocks.AIR && block.getHitBox() != null)
			{
				// Perform fine stepping to detect if the hit really landed
				// Ray position (for intersection testing)
				double rayX = rayOffX + startX;
				double rayY = rayOffY + startY;
				double rayZ = rayOffZ + startZ;
				// Accumulated distance
				double dist = 0;
				final double MAX_DIST = 1.5d;
				
				Vector3d ray = new Vector3d(rayX, rayY, rayZ);
				Vector3d step = new Vector3d(direction);
				// Step in 1/32nds (half a unit)
				step.mul(1d/32d);
				
				AABBCollider blockBox = new AABBCollider(block.getHitBox());
				blockBox.setPosition(blockX, blockY, blockZ);
				
				double deltaDist = step.length();
				
				// Step until the hitbox of the block is intersected
				while(dist < MAX_DIST)
				{
					if (blockBox.intersectsWith((float)ray.x, (float)ray.y, (float)ray.z))
					{
						// Found block, stop
						RaycastResult result = new RaycastResult();
						result.hitX = ray.x;
						result.hitY = ray.y;
						result.hitZ = ray.z;
						result.blockX = blockX;
						result.blockY = blockY;
						result.blockZ = blockZ;
						result.face = hitFace;
						
						return result;
					}
					
					ray.add(step);
					dist += deltaDist;
				}
				
				// Block not found, continue
			}
			
			// Perform orthogonal step
			boolean doXStep = false, doYStep = false;
			
			if (tMaxX < tMaxY)
			{
				if (tMaxX < tMaxZ)
					doXStep = true;
				// else: Z Step is implied
			}
			else
			{
				if (tMaxY < tMaxZ)
					doYStep = true;
				// else: Z Step is implied
			}
			
			if (doXStep)
			{
				// X Step
				if (tMaxX > radius)
					break;
				
				rayOffX = tMaxX * dx;
				rayOffY = tMaxX * dy;
				rayOffZ = tMaxX * dz;
				
				tMaxX += tDeltaX;
				blockX += stepX;
				
				// Keep track of face
				hitFace = Facing.WEST;
				if (stepX < 0)
					hitFace = Facing.EAST;
			}
			else if (doYStep)
			{
				// Y Step
				if (tMaxY > radius)
					break;
				
				rayOffX = tMaxY * dx;
				rayOffY = tMaxY * dy;
				rayOffZ = tMaxY * dz;
				
				tMaxY += tDeltaY;
				blockY += stepY;
				
				// Keep track of face
				hitFace = Facing.DOWN;
				if (stepY < 0)
					hitFace = Facing.UP;
			}
			else
			{
				// Z Step
				if (tMaxZ > radius)
					break;
				
				rayOffX = tMaxZ * dx;
				rayOffY = tMaxZ * dy;
				rayOffZ = tMaxZ * dz;
				
				tMaxZ += tDeltaZ;
				blockZ += stepZ;
				
				// Keep track of face
				hitFace = Facing.NORTH;
				if (stepZ < 0)
					hitFace = Facing.SOUTH;
			}
		}
		
		return RaycastResult.NO_RESULT;
	}
	
	// Finds integer boundary
	private double intBound(double p, double dp)
	{
		return (dp > 0? Math.ceil(p)-p: p-Math.floor(p)) / Math.abs(dp);
	}
	
	// Coord utility //
	
	/**
	 * Converts a world coordinate to a chunk coordinate
	 * @param pos The world coordinates to convert
	 * @return The chunk position from the pos
	 */
	public Vec3i toChunkCoord(Vec3i pos)
	{
		return new Vec3i(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
	}
	
	public Vec3i toBlockChunkCoord(Vec3i pos)
	{
		return new Vec3i(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF);
	}
	
	// Chunk management //
	/**
	 * Gets the chunk for the requested position
	 * @param chunkX The x position of the target chunk (in chunks)
	 * @param chunkY The y position of the target chunk (in chunks)
	 * @param chunkZ The z position of the target chunk (in chunks)
	 * @return The requested chunk
	 */
	public Chunk getChunk(int chunkX, int chunkY, int chunkZ)
	{
		return chunkManager.getChunk(chunkX, chunkY, chunkZ);
	}
	
	/**
	 * Gets the chunk for the requested position
	 * @param pos The position of the target chunk (in chunks)
	 * @return The requested chunk
	 */
	public Chunk getChunk(Vec3i pos)
	{
		return chunkManager.getChunk(pos);
	}
	
	// Fluid management //
	public FluidInstance getFluidInstance(Fluid fluid)
	{
		return fluidInstances.get(fluid);
	}
	
	// Entity management //
	/**
	 * Adds an entity to the world
	 * @param e The entity to add
	 */
	public void addEntity(Entity e)
	{
		// Set the entity's world
		e.setWorld(this);
		
		// Add to the list
		pendingEntities.add(e);
	}
	
	// Other //
	public void explode(int centreX, int centreY, int centreZ, int radius)
	{
		// Pass 1: Initial block removal
		// No updates to adjacent chunks, or nearest neighbor updates
		for (int y = -radius; y <= radius; y++)
		{
			for (int z = -radius; z <= radius; z++)
			{
				for (int x = -radius; x <= radius; x++)
				{
					int dist = x*x + y*y + z*z;
					if (dist > radius*radius)
						continue;
					setBlock(centreX + x, centreY + y, centreZ + z, Blocks.AIR, (byte)0, 1);
				}
			}
		}
		
		// Pass 2: Updates
		// Update all the things (flags=7) at the edge
		for (int y = -radius; y <= radius; y++)
		{
			for (int z = -radius; z <= radius; z++)
			{
				for (int x = -radius; x <= radius; x++)
				{
					int dist = x*x + y*y + z*z;
					if (dist < (radius*radius) - 2*radius + 1 || dist > (radius*radius))
						continue;
					
					setBlock(centreX + x, centreY + y, centreZ + z, Blocks.AIR, (byte)0, 7);
				}
			}
		}
	}
	
	/**
	 * Updates the world
	 * @param delta The delta for updates
	 */
	public void update(float delta)
	{
		accumulatedWorldTick += delta;
		if (accumulatedWorldTick > 1f/20f)
		{
			accumulatedWorldTick = 0;
			doBlockTick();
		}
		
		// Add all of the pending entities
		loadedEntities.addAll(pendingEntities);
		pendingEntities.clear();
		
		// Update all of the entities
		for (Entity e : loadedEntities)
		{
			e.update(delta);
		}
		
		// Remove all the entities that need to be removed
		loadedEntities.removeIf((e) -> e.isRemoved);
		
		// Process the lighting updates
		processLightUpdate();
	}
	
	private void doBlockTick()
	{
		if (!pendingShadowUpdates.isEmpty() || !pendingShadowRemoves.isEmpty())
			return;
		
		// Update the loaded chunks
		List<Chunk> workingList = new ArrayList<>(chunkManager.loadedChunks.values());
		workingList.iterator().forEachRemaining((chunk) ->
		{
			for (int pos : chunk.tickables)
			{
				int x = (pos >> 0) & 0xF;
				int y = (pos >> 8) & 0xF;
				int z = (pos >> 4) & 0xF;
				
				Block block = Block.idToBlock(chunk.getBlock(x, y, z));
				block.onTick(this, x + chunk.chunkX * 16, y + chunk.chunkY * 16, z + chunk.chunkZ * 16);
			}
			
			// Select 24 different positions
			for (int i = 0; i < 24; i++)
			{
				int x = worldRandom.nextInt(16);
				int y = worldRandom.nextInt(16);
				int z = worldRandom.nextInt(16);
				
				Block block = Block.idToBlock(chunk.getBlock(x, y, z));
				
				if (block.isRandomlyTickable())
					block.onRandomTick(this, x + chunk.chunkX * 16, y + chunk.chunkY * 16, z + chunk.chunkZ * 16);
			}
		});
		
		for (int i = 0; i < Fluid.ALL_FLUIDS.length; i++)
		{
			FluidInstance instance = fluidInstances.get(Fluid.ALL_FLUIDS[i]);
			if (instance.isFluidTickPending() && --fluidTickSchedules[i] <= 0)
			{
				instance.doFluidTick(this);
				fluidTickSchedules[i] = instance.getFluid().updateRate;
			}
		}
	}
	
	private void processLightUpdate()
	{
		// Remove old sky light
		while (!pendingShadowRemoves.isEmpty())
		{
			LightUpdate update = pendingShadowRemoves.poll();
			byte lastLight = update.newLight;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				if (newPos.getY() < 0)
					continue;
				
				byte adjacentLight = getSkyLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				if ((adjacentLight != 0 && adjacentLight < lastLight)
					|| (lastLight == 15 && dir == Facing.DOWN))
				{
					// Propagate the emptiness...
					setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte)0);
					
					// Update the adjacent neighbor chunks
					Vec3i blockPos = toBlockChunkCoord(newPos);
					updateNeighboringChunks(toChunkCoord(newPos), blockPos.getX(), blockPos.getY(), blockPos.getZ());
					
					pendingShadowRemoves.add(new LightUpdate(newPos, adjacentLight));
				}
				else if (adjacentLight >= lastLight && adjacentLight > 0)
				{
					// Change to propagate, adjacent light is equal or bigger (and not zero)
					pendingShadowUpdates.add(new LightUpdate(newPos, adjacentLight));
				}
			}
		}
		
		// Propagate sky light
		while (pendingShadowRemoves.isEmpty() && !pendingShadowUpdates.isEmpty())
		{
			LightUpdate update = pendingShadowUpdates.poll();
			
			if (update.pos.getY() < 0)
				continue;
			
			int x = update.pos.getX();
			int y = update.pos.getY();
			int z = update.pos.getZ();
			
			// Fetch the light value
			byte currentLight = getSkyLight(x, y, z);
			
			// Don't propagate emptiness
			if (currentLight == 0)
				continue;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				Block adjacentBlock = getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				byte adjacentSkylight = getSkyLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				byte newLight = (byte)(currentLight - Math.max(1, adjacentBlock.getOpacity()));
				
				// For horizontal spreading : spread as normal
				// For vertical spreading :
				// - If the difference between adjacent skylight & current
				//   skylight is greater than the opacity, spread
				
				// Check if the adjacent block can propagate shadow
				if (adjacentBlock.isTransparent()
						&& (adjacentSkylight + 1 <= newLight
						|| (dir == Facing.DOWN && adjacentSkylight <= newLight && newLight > 0)))
				{
					// When propagating the maximum light down, only be affected by opacity
					if (dir == Facing.DOWN && currentLight == 15)
						setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte) (currentLight - adjacentBlock.getOpacity()));
					else
						setSkyLight(newPos.getX(), newPos.getY(), newPos.getZ(), newLight);
					
					// Update the adjacent neighbor chunks
					Vec3i blockPos = toBlockChunkCoord(newPos);
					updateNeighboringChunks(toChunkCoord(newPos), blockPos.getX(), blockPos.getY(), blockPos.getZ());
					
					pendingShadowUpdates.add(new LightUpdate(newPos, (byte)0));
				}
			}
		}
		
		// Remove old lights
		while (!pendingLightRemoves.isEmpty())
		{
			LightUpdate update = pendingLightRemoves.poll();
			byte lastLight = update.newLight;
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				byte adjacentLight = getBlockLight(newPos.getX(), newPos.getY(), newPos.getZ());
				
				if (adjacentLight != 0 && adjacentLight < lastLight)
				{
					// Propagate the emptiness...
					setBlockLight(newPos.getX(), newPos.getY(), newPos.getZ(), (byte)0);
					
					// Update the adjacent neighbor chunks
					Vec3i blockPos = toBlockChunkCoord(newPos);
					updateNeighboringChunks(toChunkCoord(newPos), blockPos.getX(), blockPos.getY(), blockPos.getZ());
					
					pendingLightRemoves.add(new LightUpdate(newPos, adjacentLight));
				}
				else if (adjacentLight >= lastLight)
				{
					// Change to propagate, adjacent light is bigger
					pendingLightUpdates.add(new LightUpdate(newPos, (byte)0));
				}
			}
		}
		
		// Propagate light
		while (!pendingLightUpdates.isEmpty())
		{
			LightUpdate update = pendingLightUpdates.poll();
			int x = update.pos.getX();
			int y = update.pos.getY();
			int z = update.pos.getZ();
			
			// Fetch the light value
			byte currentLight = getBlockLight(x, y, z);
			
			for (Facing dir : Facing.values())
			{
				Vec3i newPos = update.pos.add(dir);
				Block adjacentBlock = getBlock(newPos.getX(), newPos.getY(), newPos.getZ());
				// Allow block light to be affected by opacity
				byte newLight = (byte)(currentLight - 1);
				
				// Check if the adjacent block can propagate light
				if (adjacentBlock.isTransparent()
						&& getBlockLight(newPos.getX(), newPos.getY(), newPos.getZ()) + 1 <= newLight)
				{
					setBlockLight(newPos.getX(), newPos.getY(), newPos.getZ(), newLight);
					
					// Update the adjacent neighbor chunks
					Vec3i blockPos = toBlockChunkCoord(newPos);
					updateNeighboringChunks(toChunkCoord(newPos), blockPos.getX(), blockPos.getY(), blockPos.getZ());
					
					pendingLightUpdates.add(new LightUpdate(newPos, (byte)0));
				}
			}
		}
	}
	
	static class LightUpdate
	{
		final Vec3i pos;
		final byte newLight;
		
		public LightUpdate(Vec3i pos, byte newLight)
		{
			this.pos = pos;
			this.newLight = newLight;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof LightUpdate))
				return false;
			
			LightUpdate other = (LightUpdate)(obj);
			return this.pos.equals(other.pos);
		}
	}
	
}
