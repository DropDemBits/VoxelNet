package ddb.io.voxelnet.world;

import ddb.io.voxelnet.network.packet.PSChunkData;
import ddb.io.voxelnet.util.Vec3i;

public class ClientChunkManager extends ChunkManager
{
	public ClientChunkManager(World world)
	{
		super(world);
	}
	
	@Override
	public Chunk loadChunk(Vec3i pos)
	{
		// TODO: Load chunk from the server
		return super.loadChunk(pos);
	}
	
	@Override
	public ChunkColumn loadColumn(Vec3i pos)
	{
		// TODO: Load chunk from the server
		return super.loadColumn(pos);
	}
	
	/**
	 * Processes a chunk data packet
	 * @param chunkData The chunk data packet to process
	 */
	public void processNetLoad(PSChunkData chunkData)
	{
		chunkData.setWorld(this.world);
		
		// Load the column
		ChunkColumn column = chunkData.column;
		chunkColumns.put(new Vec3i(chunkData.chunkX, 0, chunkData.chunkZ), column);
		
		// Load the rest of the chunk data
		for (Chunk chunk : chunkData.chunkList)
			loadedChunks.put(new Vec3i(chunk.chunkX, chunk.chunkY, chunk.chunkZ), chunk);
	}
	
}
