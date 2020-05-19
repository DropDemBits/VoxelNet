package ddb.io.voxelnet.client;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.event.network.ConnectionStateChangeEvent;
import ddb.io.voxelnet.network.NetworkManager;
import ddb.io.voxelnet.network.packet.*;
import ddb.io.voxelnet.util.EntityIDMap;
import ddb.io.voxelnet.util.RaycastResult;
import ddb.io.voxelnet.world.ClientChunkManager;
import ddb.io.voxelnet.world.World;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages the client's network state
 */
public class ClientNetworkManager implements NetworkManager
{
	// Associated client instance
	Game instance;
	
	// Netty IO things
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	public Channel clientChannel;
	
	// Pending packet processing queue
	public Queue<Packet> packetQueue = new ConcurrentLinkedQueue<>();
	
	// Current connection state to the server
	private ConnectionState connectionState = ConnectionState.CLOSED;
	
	EntityIDMap entityMap;
	
	// The connection id for the client
	private int clientID = -1;
	
	// Server connection address
	private int serverPort;
	private String serverAddress;
	
	public ClientNetworkManager(Game instance) { this.instance = instance; }
	
	/**
	 * Changes the connection address
	 * Must be called before (re)initializing the network manager
	 *
	 * @param address The address of the server
	 * @param port The port for the server
	 */
	public void setConnectionAddress(String address, int port)
	{
		this.serverAddress = address;
		this.serverPort = port;
	}
	
	@Override
	public boolean init()
	{
		if (connectionState != ConnectionState.CLOSED)
			return false;
		changeConnectionState(ConnectionState.CONNECTING);
		
		// Initialize the entity id map
		entityMap = new EntityIDMap();
		
		// Connect to the server
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(bossGroup)
				.channel(NioSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(SocketChannel ch)
					{
						ch.pipeline().addLast(new LengthFieldPrepender(2));
						ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(0xFFFF, 0, 2));
						ch.pipeline().addLast(new PacketCodec());
						ch.pipeline().addLast(new ClientChannelHandler(instance));
					}
				})
				.option(ChannelOption.SO_KEEPALIVE, true);
		
		try
		{
			// Try to connect to the server
			ChannelFuture f = bootstrap.connect(serverAddress, serverPort).sync();
			clientChannel = f.channel();
		}
		catch (InterruptedException e)
		{
			// Failed to make a connection, close the port
			changeConnectionState(ConnectionState.CLOSED);
			
			System.err.println("Unable to connect to the address");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	@Override
	public void shutdown()
	{
		changeConnectionState(ConnectionState.CLOSED);
		
		try
		{
			if (clientChannel != null)
				clientChannel.close().sync();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		finally
		{
			bossGroup.shutdownGracefully();
		}
	}
	
	@Override
	public void update()
	{
		for (int count = 0; count < 10 && !packetQueue.isEmpty(); count++)
		{
			Packet packet = packetQueue.poll();
			
			this.processPacket(packet);
		}
		
		if (connectionState == ConnectionState.ESTABLISHED)
		{
			// Send position updates to the server
			PCSPosRotUpdate posUpdate = new PCSPosRotUpdate(clientID, instance.player);
			clientChannel.write(posUpdate);
			clientChannel.flush();
		}
	}
	
	@Override
	public void handlePacket(Packet packet)
	{
		// Add the packet to the process queue
		packetQueue.offer(packet);
	}
	
	@Override
	public EntityIDMap getNetworkIDMap()
	{
		return entityMap;
	}
	
	@Override
	public boolean isClient()
	{
		return true;
	}
	
	/*--* Implementation specific code start *--*/
	
	public int getClientID()
	{
		return clientID;
	}
	
	/**
	 * Gets the current connection state
	 * @return The current connection state
	 */
	public ConnectionState getConnectionState()
	{
		return connectionState;
	}
	
	/**
	 * Adds a packet to the server send queue
	 * @param packet The packet to send to the server
	 */
	public void sendPacket(Packet packet)
	{
		clientChannel.write(packet);
	}
	
	/**
	 * Changes the connection state and broadcasts a ConnectionStateChangeEvent
	 * @param newState The new connection state
	 */
	private void changeConnectionState(ConnectionState newState)
	{
		// Post state change to the event bus
		Game.GLOBAL_BUS.postEvent(new ConnectionStateChangeEvent(newState));
		connectionState = newState;
	}
	
	// Processes and executes a client bound packet
	private void processPacket(Packet packet)
	{
		World world = instance.world;
		
		if (packet.getPacketID() == 0)
		{
			// PSEstablishConnection
			clientID = ((PSEstablishConnection)packet).clientID;
			System.out.println("NewID: " + clientID);
			
			changeConnectionState(ConnectionState.ESTABLISHED);
		}
		else if (packet.getPacketID() == 1)
		{
			// CSPosRotUpdate
			// Get the specific player to update
			PCSPosRotUpdate posUpdate = (PCSPosRotUpdate)packet;
			
			// Skip movement updates for this local player
			if (clientID == posUpdate.clientID)
				return;
			
			EntityPlayer player = (EntityPlayer) entityMap.getEntity(posUpdate.clientID, EntityPlayer.class);
			
			// If non-existant move on to the next packet
			if (player == null)
				return;
			
			player.setPos(posUpdate.xPos, posUpdate.yPos, posUpdate.zPos);
			player.setVelocity(posUpdate.xVel, posUpdate.yVel, posUpdate.zVel);
			player.setOrientation(posUpdate.pitch, posUpdate.yaw);
			player.xAccel = posUpdate.xAccel;
			player.zAccel = posUpdate.zAccel;
			
			player.isFlying = posUpdate.isFlying;
			player.isSneaking = posUpdate.isSneaking;
			player.isSprinting = posUpdate.isSprinting;
		}
		else if (packet.getPacketID() == 2)
		{
			// SSpawnPlayer
			// Spawn new player in the world
			PSSpawnPlayer spawn = (PSSpawnPlayer)packet;
			System.out.println("PSpawn (" + spawn.clientID + ")");
			
			// Add the player to the mapping
			EntityPlayer player = new EntityPlayer();
			world.addEntity(player);
			entityMap.addExistingEntity(player, spawn.clientID);
		}
		else if (packet.getPacketID() == 3)
		{
			// SKillPlayer
			// Remove the player from the world
			PSKillPlayer kill = (PSKillPlayer)packet;
			System.out.println("PSpawn (" + kill.clientID + ")");
			
			EntityPlayer player = (EntityPlayer) entityMap.getEntity(kill.clientID, EntityPlayer.class);
			
			// If non-existant move on (probably a stale id)
			if (player == null)
				return;
			
			player.setDead();
		}
		else if (packet.getPacketID() == 4)
		{
			// SChunkData
			// Process the chunk data
			PSChunkData chunkData = (PSChunkData)packet;
			((ClientChunkManager)world.chunkManager).processNetLoad(chunkData);
		}
		else if (packet.getPacketID() == 5)
		{
			// Block place, handle that
			PCSPlaceBlock blockPlace = (PCSPlaceBlock)packet;
			RaycastResult lastHit = blockPlace.hitResult;
			Block block = blockPlace.placingBlock;
			
			if (clientID == blockPlace.clientID)
				return;
			
			// If the block can't be placed, don't place it
			if(!block.canPlaceBlock(
					world,
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ()))
				return;
			
			world.setBlock(
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ(),
					block);
			block.onBlockPlaced(
					world,
					lastHit.blockX + lastHit.face.getOffsetX(),
					lastHit.blockY + lastHit.face.getOffsetY(),
					lastHit.blockZ + lastHit.face.getOffsetZ());
		}
		else if (packet.getPacketID() == 6)
		{
			// Block break, handle that
			PCSBreakBlock blockBreak = (PCSBreakBlock)packet;
			RaycastResult lastHit = blockBreak.hitResult;
			
			if (clientID == blockBreak.clientID)
				return;
			
			// Break the block, with the appropriate block callbacks being called
			Block block = world.getBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			block.onBlockBroken(world, lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			world.setBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ, Blocks.AIR);
		}
	}
}
