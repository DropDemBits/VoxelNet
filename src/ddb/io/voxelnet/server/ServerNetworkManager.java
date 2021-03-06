package ddb.io.voxelnet.server;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.network.NetworkManager;
import ddb.io.voxelnet.network.packet.*;
import ddb.io.voxelnet.util.EntityIDMap;
import ddb.io.voxelnet.util.RaycastResult;
import ddb.io.voxelnet.world.ChunkColumn;
import ddb.io.voxelnet.world.ChunkManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the server's network state
 */
public class ServerNetworkManager implements NetworkManager
{
	// Associated server instance
	ServerGame instance;
	
	// Groups
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	Channel serverChannel;
	private final int hostPort;

	// Packets to process
	public Queue<ProcessEntry> processQueue = new ConcurrentLinkedQueue<>();
	
	// Entity - ClientID Mapping
	EntityIDMap entityMap;
	// ClientID - ChannelID Mapping
	private final Map<Integer, ChannelId> clientToChannelId = new LinkedHashMap<>();
	
	public ServerNetworkManager(ServerGame instance, int hostPort)
	{
		this.instance = instance;
		this.hostPort = hostPort;
	}
	
	@Override
	public boolean init()
	{
		// Initialize the network id map
		entityMap = new EntityIDMap();
		
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class) // Channel type
				.childHandler(new ChannelInitializer<SocketChannel>() // Setup ChannelInitializer
				{
					@Override
					protected void initChannel(SocketChannel ch)
					{
						// On client connect:
						// Send back client-id
						
						// Client side:
						// Send back position of player
						// client id | pos x | pos y | pos z
						
						// Server side:
						// Broadcast all player positions
						ch.pipeline().addLast(new LengthFieldPrepender(2));
						ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(0xFFFF, 0, 2));
						ch.pipeline().addLast(new PacketCodec());
						ch.pipeline().addLast(new ServerChannelHandler(instance));
					}
				})
				.option(ChannelOption.SO_BACKLOG, 128)
				.childOption(ChannelOption.SO_KEEPALIVE, true);
		
		// Bind & start accepting connections
		try
		{
			ChannelFuture f = bootstrap.bind(this.hostPort).sync();
			serverChannel = f.channel();
		}
		catch (InterruptedException e)
		{
			System.err.println("Unable to bind to the requested port");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	@Override
	public void shutdown()
	{
		try
		{
			if (serverChannel != null)
				serverChannel.close().sync();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
	
	@Override
	public void update()
	{
		// Process 16 of the packets at a time
		// TODO: SLOW, delegate to dedicated worker threads (keeping concurrency in mind)
		for (int i = 0; i < 16 && !processQueue.isEmpty(); i++)
		{
			ProcessEntry entry = processQueue.remove();
			processPacket(entry.packet, entry.sourceClient);
		}
		
		// Flush pending packets
		clientChannels.flush();
	}
	
	@Override
	public void handlePacket(Packet msg, int sourceClientID)
	{
		// Add to process queue
		processQueue.add(new ProcessEntry(msg, sourceClientID));
	}
	
	private void processPacket(Packet msg, int sourceClientID)
	{
		// Execute Server vs Execute Client
		if (msg.getPacketID() == 1)
		{
			// Position updates, broadcast to everyone else
			PCSPosRotUpdate posUpdate = (PCSPosRotUpdate)msg;
			
			// CSPosRotUpdate
			// Get the specific entity to update
			EntityPlayer player = (EntityPlayer) entityMap.getEntity(posUpdate.clientID, EntityPlayer.class);
			
			// If non-existant, move on (probably a stale id)
			if (player == null)
				return;
			
			System.out.printf("\t Ply%d-Pos: (%f, %f, %f) - (%f, %f)\n", posUpdate.clientID, posUpdate.xPos, posUpdate.yPos, posUpdate.zPos, posUpdate.pitch, posUpdate.yaw);
			player.setPos(posUpdate.xPos, posUpdate.yPos, posUpdate.zPos);
			player.setVelocity(posUpdate.xVel, posUpdate.yVel, posUpdate.zVel);
			player.setOrientation(posUpdate.pitch, posUpdate.yaw);
			player.xAccel = posUpdate.xAccel;
			player.zAccel = posUpdate.zAccel;
			
			player.isFlying = posUpdate.isFlying;
			player.isSneaking = posUpdate.isSneaking;
			player.isSprinting = posUpdate.isSprinting;
			
			clientChannels.write(posUpdate);
		}
		else if (msg.getPacketID() == 5)
		{
			// Block place, handle that
			PCSPlaceBlock blockPlace = (PCSPlaceBlock)msg;
			RaycastResult lastHit = blockPlace.hitResult;
			Block block = blockPlace.placingBlock;
			
			// If the block can't be placed, don't place it
			if(!block.canPlaceBlock(
					instance.world,
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ()))
				return;
			
			instance.world.setBlock(
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ(),
					block);
			block.onBlockPlaced(
					instance.world,
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ());
			
			// Broadcast to the other players
			// TODO: Correct for misplaces
			clientChannels.write(blockPlace);
		}
		else if (msg.getPacketID() == 6)
		{
			// Block break, handle that
			PCSBreakBlock blockBreak = (PCSBreakBlock)msg;
			RaycastResult lastHit = blockBreak.hitResult;
			
			// Break the block, with the appropriate block callbacks being called
			Block block = instance.world.getBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			block.onBlockBroken(instance.world, lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			instance.world.setBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ, Blocks.AIR);
			
			// Broadcast to the other players
			// TODO: Correct for mis-breaks
			clientChannels.write(blockBreak);
		}
		else if (msg.getPacketID() == 7)
		{
			// Load the requested chunk column
			PCLoadChunkColumn loadRequest = (PCLoadChunkColumn)msg;
			// Get the channel id
			ChannelId channelId = clientToChannelId.get(sourceClientID);
			
			// No id found means stale id
			if (channelId == null)
				return;
			
			Channel clientChannel = clientChannels.find(channelId);
			
			// Null channel means stale id
			if (clientChannel == null)
				return;
			
			// Send back the chunk column
			if (sendChunkColumnTo(loadRequest.columnX, loadRequest.columnZ, clientChannel))
			{
				// TODO: Queue up requests
				System.out.println("Fulfilled rq for " + loadRequest.columnX + ", " + loadRequest.columnZ + " to Ply" + sourceClientID);
				// Flush pending requests
				clientChannel.flush();
			}
		}
	}
	
	@Override
	public EntityIDMap getNetworkIDMap()
	{
		return entityMap;
	}
	
	@Override
	public boolean isClient()
	{
		return false;
	}
	
	private EntityPlayer spawnPlayer()
	{
		// Setup the player
		EntityPlayer player = new EntityPlayer();
		instance.world.addEntity(player);
		
		// Spawn the player at the surface
		int spawnY = 256;
		
		for (; spawnY >= 0; spawnY--)
		{
			boolean isSolid = instance.world.getBlock(0, spawnY - 1, 0).isSolid();
			if (isSolid)
				break;
		}
		
		player.setPos(0.5f, spawnY + 0.5F, 0.5f);
		
		return player;
	}
	
	public int addClient(Channel channel)
	{
		// Spawn the client's player
		EntityPlayer player = spawnPlayer();
		
		// Add to the mappings
		int clientID = entityMap.addEntity(player);
		
		clientChannels.add(channel);
		clientToChannelId.put(clientID, channel.id());
		
		// Spawn the client on the other channels
		PSSpawnPlayer packet = new PSSpawnPlayer(clientID);
		// Flush later
		clientChannels.write(packet, (otherChannel) -> otherChannel != channel);
		
		// Send the surrounding chunks over
		int radius = 3;
		for (int z = -radius; z <= radius; z++)
		{
			for (int x = -radius; x <= radius; x++)
			{
				sendChunkColumnTo(x, z, channel);
			}
		}
		
		// Send back client id / Start client comms
		channel.write(new PSEstablishConnection(clientID));
		
		// Spawn the other players on this channel
		for (Integer id : entityMap.getAllEntityIds())
		{
			if (id == clientID)
				continue;
			
			channel.write(new PSSpawnPlayer(id));
		}
		channel.flush();
		
		return clientID;
	}
	
	public void removeClient(Channel channel, int clientID)
	{
		System.out.println("Goodbye, client #" + clientID);
		
		// Kill the player
		EntityPlayer player = (EntityPlayer) entityMap.getEntity(clientID, EntityPlayer.class);
		player.setDead();
		
		clientChannels.remove(channel);
		clientToChannelId.remove(clientID);
		entityMap.removeEntity(clientID);
		
		// Kill the client on the other channels
		PSKillPlayer packet = new PSKillPlayer(clientID);
		clientChannels.write(packet);
	}
	
	private boolean sendChunkColumnTo(int x, int z, Channel channel)
	{
		System.out.format("Sending (%d, %d)\n", x, z);
		ChunkManager chunkManager = instance.world.chunkManager;
		ChunkColumn column = chunkManager.getColumn(x, z).orElseGet(() -> {
			// Generate chunk if null
			// TODO: Only allow one thread to generate at a time
			return chunkManager.loadColumn(x, z);
		});
		
		// Construct a new chunk data packet
		PSChunkData chunkData = new PSChunkData(x, z, column);
		
		for (int y = 0; y < 256 / 16; y++)
		{
			chunkManager.getChunk(x, y, z, false).ifPresent(chunkData::addChunk);
		}
		
		// Send out the chunk
		channel.write(chunkData);
		return true;
	}
	
	private static class ProcessEntry
	{
		public int sourceClient;
		public Packet packet;
		
		public ProcessEntry(Packet packet, int sourceClient)
		{
			this.sourceClient = sourceClient;
			this.packet = packet;
		}
	}
	
}
