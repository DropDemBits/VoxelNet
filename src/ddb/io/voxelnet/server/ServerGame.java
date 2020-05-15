package ddb.io.voxelnet.server;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.block.Blocks;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.event.EventBus;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.network.*;
import ddb.io.voxelnet.util.RaycastResult;
import ddb.io.voxelnet.world.Chunk;
import ddb.io.voxelnet.world.ChunkColumn;
import ddb.io.voxelnet.world.World;
import ddb.io.voxelnet.world.WorldSave;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGame
{
	public boolean isRunning = false;
	
	private ServerSettings settings = null;
	
	WorldSave worldSave;
	World world;
	
	// Player - ClientID Mapping
	private Map<Integer, EntityPlayer> playerIDMappings = new ConcurrentHashMap<>();
	
	double elapsed = 0.0d;
	
	double updTime = 0;
	double currentUPD = 0;
	
	// Global Event Bus
	public static final EventBus GLOBAL_BUS = new EventBus();
	
	// Networking Things //
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	ChannelGroup clientChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	Channel serverChannel;
	int nextClientID = 0;
	
	ServerGame instance;
	
	public ServerGame()
	{
		instance = this;
	}
	
	public void setSettings(ServerSettings settings)
	{
		this.settings = settings;
	}
	
	public void run()
	{
		assert settings != null : "Settings must be set";
		
		// Catch all the bad exceptions
		try
		{
			/// Init ///
			init();
			
			/// Main Loop ///
			isRunning = true;
			loop();
		}
		catch (Exception e)
		{
			System.err.println("Oops, a bad thing happened!");
			e.printStackTrace();
		}
		finally
		{
			/// Cleanup ///
			cleanup();
		}
	}
	
	private void init() throws InterruptedException
	{
		// Initialize the server connections
		networkInit();
		
		// Initialize the blocks
		Block.init();
		Fluid.init();
		
		///////////////////////////////////////////////
		
		// Setup the world, world save/loader, and world renderer
		// "world-allthings" is main world
		world = new World(false);
		worldSave = new WorldSave(world, "world-server.dat");
		
		// Load / Generate the world
		if (worldSave.canLoad())
			worldSave.load();
		else
			world.generate();
	}
	
	private void networkInit() throws InterruptedException
	{
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class) // Channel type
				.childHandler(new ChannelInitializer<SocketChannel>() // Setup ChannelInitializer
				{
					@Override
					protected void initChannel(SocketChannel ch) throws Exception
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
		ChannelFuture f = bootstrap.bind(settings.hostPort).sync();
		serverChannel = f.channel();
	}
	
	private void loop()
	{
		int ups = 0;
		double last = getSystemTime();
		double lag = 0;
		double nextNetworkTick = 0.0D;
		
		double secondTimer = getSystemTime();
		final double MS_PER_PHYSICS_TICK = 1.0 / 60.0;
		final double MS_PER_NETWORK_TICK = 1.0 / 10.0; // 10 Hz / 100 ms interval
		
		while(isRunning)
		{
			double now = getSystemTime();
			double elapsed = now - last;
			last = now;
			lag += elapsed;
			this.elapsed += elapsed;
			
			// Event Stage
			GLOBAL_BUS.processEvents();
			
			// Update Stage
			// Catchup loop
			boolean didUpdate = false;
			double updTick = getSystemTime();
			
			// Cap the lag amount
			if (lag > 1f)
				lag = 1f;
			
			while(lag >= MS_PER_PHYSICS_TICK)
			{
				update((float)MS_PER_PHYSICS_TICK);
				ups++;
				lag -= MS_PER_PHYSICS_TICK;
				didUpdate = true;
			}
			
			if(didUpdate)
				updTime += getSystemTime() - updTick;
			
			// Network Stage
			now = getSystemTime();
			if (now >= nextNetworkTick)
			{
				networkTick();
				nextNetworkTick = now + MS_PER_NETWORK_TICK;
			}
			
			if (now - secondTimer > 1)
			{
				// Update the things
				currentUPD = updTime / ups;
				updTime = 0;
				ups = 0;
				secondTimer = now;
			}
		}
	}
	
	private double getSystemTime()
	{
		return System.currentTimeMillis() / 1000.0D;
	}
	
	private void beginShutdown()
	{
		this.isRunning = false;
	}
	
	private void cleanup()
	{
		// Wait until the server socket is closed
		try
		{
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
		
		System.out.println("Shutting down");
		
		// Save the world
		worldSave.save();
	}
	
	private void update(float delta)
	{
		// Process command input from command line
		processCommandLine();
		
		/// Process World Things ///
		world.update(delta);
	}
	
	private void processCommandLine()
	{
		try
		{
			final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			while (System.in.available() > 0)
			{
				// Keep processing lines
				String inputLine = reader.readLine();
				
				if (inputLine.equals("stop") || inputLine.equals("exit"))
				{
					// Gracefully exit
					beginShutdown();
					break;
				}
				else if (inputLine.equals("status"))
				{
					System.out.println("Last tick time " + (currentUPD * 1000.0D) + " ms");
				}
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void networkTick()
	{
		// Flush pending packets
		clientChannels.flush();
	}
	
	public int addClient(Channel channel)
	{
		int clientID = nextClientID++;
		clientChannels.add(channel);
		
		// Spawn the client's player
		spawnPlayer(clientID);
		
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
				ChunkColumn column = world.chunkManager.getColumn(x, z);
				
				// Skip column if null (No chunks there)
				if (column == null)
					continue;
				
				// Construct a new chunk data packet
				PSChunkData chunkData = new PSChunkData(x, z, world.chunkManager.getColumn(x, z));
				
				for (int y = 0; y < 256 / 16; y++)
				{
					Chunk chunk = world.chunkManager.getChunk(x, y, z, false);
					
					if (chunk == world.chunkManager.EMPTY_CHUNK)
						continue;
					
					chunkData.addChunk(chunk);
				}
				
				// Send out the chunk
				channel.write(chunkData);
			}
		}
		
		// Send back client id / Start client comms
		channel.write(new PSEstablishConnection(clientID));
		
		// Spawn the other players on this channel
		for (Integer id : playerIDMappings.keySet())
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
		clientChannels.add(channel);
		despawnPlayer(clientID);
		
		// Kill the client on the other channels
		PSKillPlayer packet = new PSKillPlayer(clientID);
		clientChannels.write(packet);
	}
	
	private void spawnPlayer(int clientID)
	{
		// Setup the player
		EntityPlayer player = new EntityPlayer();
		world.addEntity(player);
		
		// Spawn the player at the surface
		int spawnY = 256;
		
		for (; spawnY >= 0; spawnY--)
		{
			boolean isSolid = world.getBlock(0, spawnY - 1, 0).isSolid();
			if (isSolid)
				break;
		}
		
		player.setPos(0.5f, spawnY + 0.5F, 0.5f);
		
		// Add to the mappings
		playerIDMappings.put(clientID, player);
	}
	
	private void despawnPlayer(int clientID)
	{
		System.out.println("Goodbye, client #" + clientID);
		// Remove the player from the mappings
		EntityPlayer oldPlayer = playerIDMappings.remove(clientID);
		// Remove the player entity
		oldPlayer.setDead();
	}
	
	public void handlePacket(Packet msg)
	{
		if (msg.getPacketID() == 1)
		{
			// Position updates, broadcast to everyone else
			PCSPosRotUpdate posUpdate = (PCSPosRotUpdate)msg;
			
			// CSPosRotUpdate
			// Get the specific player to update
			
			EntityPlayer player = playerIDMappings.getOrDefault(posUpdate.clientID, null);
			
			// If non-existant move on to the next packet
			if (player == null)
				return;
			
			System.out.printf("\t Ply%d-Pos: (%f, %f, %f) - (%f, %f)\n", posUpdate.clientID, posUpdate.xPos, posUpdate.yPos, posUpdate.zPos, posUpdate.pitch, posUpdate.yaw);
			player.setPos(posUpdate.xPos, posUpdate.yPos, posUpdate.zPos);
			player.setVelocity(posUpdate.xVel, posUpdate.yVel, posUpdate.zVel);
			player.setOrientation(posUpdate.pitch, posUpdate.yaw);
			
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
			Block block = world.getBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			block.onBlockBroken(world, lastHit.blockX, lastHit.blockY, lastHit.blockZ);
			world.setBlock(lastHit.blockX, lastHit.blockY, lastHit.blockZ, Blocks.AIR);
			
			// Broadcast to the other players
			// TODO: Correct for mis-breaks
			clientChannels.write(blockBreak);
		}
	}
	
}
