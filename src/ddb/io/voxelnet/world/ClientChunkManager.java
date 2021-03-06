package ddb.io.voxelnet.world;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.client.ClientNetworkManager;
import ddb.io.voxelnet.network.packet.PCLoadChunkColumn;
import ddb.io.voxelnet.network.packet.PSChunkData;
import ddb.io.voxelnet.util.Vec3i;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ClientChunkManager extends ChunkManager
{
	// Positions of in progress loading chunks (not thread safe!)
	private final Set<Vec3i> pendingColumnLoads = new HashSet<>();
	// Positions which have placeholders chunks (Not thread safe as well!)
	private final Set<Vec3i> placeholderChunks = new HashSet<>();
	
	// CChunkQuery
	// int chunkX, chunkY, chunkZ
	// int queryId (id to keep track of response (sent back))
	// byte queryType (chunk modification, chunk exists)
	
	// SChunkQueryResponse
	// int chunkX, chunkY, chunkZ
	
	public ClientChunkManager(World world)
	{
		super(world);
		
		// Automatically fill in the initial 7-by-7 area (expected)
		int radius = 3;
		for (int z = -radius; z <= radius; z++)
		{
			for (int x = -radius; x <= radius; x++)
			{
				pendingColumnLoads.add(new Vec3i(x, 0, z));
			}
		}
	}
	
	@Override
	protected Chunk doLoadChunk(Vec3i pos)
	{
		Chunk chunk;
		
		// Check if the chunk exists in the chunk cache
		if (loadFromChunkCache(pos))
			return loadedChunks.get(pos);
		
		// Chunk doesn't exist yet, create an empty one
		chunk = new Chunk(world, pos.getX(), pos.getY(), pos.getZ());
		loadedChunks.put(pos, chunk);
		
		// Rebuild the fields
		chunk.chunkField.rebuildField();
		chunk.chunkField.rebuildNeighborFields();
		
		Vec3i columnPos = new Vec3i(pos.getX(), 0, pos.getZ());
		
		// If the column is already loaded, pass the empty chunk
		// This is either an empty column, or a new chunk is created in the column
		if (isColumnLoaded(columnPos))
			return chunk;
		
		// Chunk's column has not been loaded yet
		// Check if a request for the column has been made yet
		if (!pendingColumnLoads.contains(columnPos))
		{
			// Initiate a column load
			loadColumn(columnPos);
		}
		
		// Mark current chunk as a placeholder and add to the placeholder chunks
		chunk.markPlaceholder();
		placeholderChunks.add(pos);
		
		return chunk;
	}
	
	@Override
	protected ChunkColumn doColumnLoad(Vec3i pos)
	{
		// Don't load columns that already have data in flight
		if (pendingColumnLoads.contains(pos))
		{
			return chunkColumns.get(pos);
		}
		
		// Check if the column exists in the chunk column cache
		if (loadFromChunkCache(pos))
		{
			// Column is now loaded
			return chunkColumns.get(pos);
		}
		
		// Send over a request to load the column
		ClientNetworkManager networkManager = Game.getInstance().getNetworkManager();
		PCLoadChunkColumn request = new PCLoadChunkColumn(pos.getX(), pos.getZ());
		networkManager.sendPacket(request);
		
		// Notify of the load
		pendingColumnLoads.add(pos);
		
		// Use a placeholder column for now
		ChunkColumn column = new ChunkColumn(pos.getX(), pos.getZ());
		chunkColumns.put(pos, column);
		return column;
	}
	
	/**
	 * Processes a chunk data packet
	 * @param chunkData The chunk data packet to process
	 */
	public void processNetLoad(PSChunkData chunkData)
	{
		// TODO: Deal with existing chunk data
		chunkData.setWorld(this.world);
		
		// Load the column
		Vec3i columnPos = new Vec3i(chunkData.chunkX, 0, chunkData.chunkZ);
		ChunkColumn column = chunkData.column;
		chunkColumns.put(columnPos, column);
		
		// Load the rest of the chunk data
		for (Chunk chunk : chunkData.chunkList)
		{
			Vec3i chunkPos = new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ);
			loadedChunks.put(chunkPos, chunk);
			placeholderChunks.remove(chunkPos);
			
			// Rebuild the fields
			chunk.chunkField.rebuildField();
			chunk.chunkField.rebuildNeighborFields();
		}
		
		// Column is now loaded
		pendingColumnLoads.remove(columnPos);
		
		// Search for missing chunk loads
		Predicate<Vec3i> inSameColumn = pos -> pos.getX() == columnPos.getX() && pos.getZ() == columnPos.getZ();
		long missingChunks = placeholderChunks.parallelStream()
				.filter(inSameColumn)
				.peek((pos) -> {
					// Mark placeholder chunk as real, no data exists
					Optional.ofNullable(loadedChunks.get(pos)).ifPresent(Chunk::markNotPlaceholder);
				})
				.count();
		
		// Remove the old placeholder counts
		placeholderChunks.removeIf(inSameColumn);
		
		// Notify of missing chunks
		if (missingChunks > 0)
			System.out.println("Found " + missingChunks + " missing chunks for column " + columnPos);
	}
	
}
