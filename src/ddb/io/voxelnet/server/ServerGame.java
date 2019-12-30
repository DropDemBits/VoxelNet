package ddb.io.voxelnet.server;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.entity.EntityPlayer;
import ddb.io.voxelnet.event.EventBus;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.network.PCSPosRotUpdate;
import ddb.io.voxelnet.network.PacketCodec;
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

public class ServerGame
{
	public boolean isRunning = true;
	
	private ServerSettings settings = null;
	
	WorldSave worldSave;
	World world;
	
	EntityPlayer otherPlayer;
	EntityPlayer player;
	
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
		world = new World();
		worldSave = new WorldSave(world, "world.dat");
		
		// Load / Generate the world
		/*if (worldSave.canLoad())
			worldSave.load();
		else
			world.generate();*/
		
		spawnPlayers();
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
	
	private void spawnPlayers()
	{
		// Setup the player
		player = new EntityPlayer();
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
		
		// Add another player
		otherPlayer = new EntityPlayer();
		world.addEntity(otherPlayer);
		otherPlayer.setPos(0.5f, spawnY + 0.5f, 0.5f);
	}
	
	private void loop()
	{
		int ups = 0;
		double last = getSystemTime();
		double lag = 0;
		
		double secondTimer = getSystemTime();
		final double MS_PER_PHYSICS_TICK = 1.0 / 60.0;
		
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
			
			if (now - secondTimer > 1)
			{
				// Update the things
				currentUPD = updTime / ups;
				updTime = 0;
				ups = 0;
				
				System.out.println("UPS: " + ups + " (Tick time " + (currentUPD * 1000.0D) + " ms)");
				
				// Broadcast player position
				//clientChannels.writeAndFlush(new PCSPosRotUpdate(0, player));
				secondTimer = now;
			}
		}
	}
	
	private double getSystemTime()
	{
		return System.currentTimeMillis() / 1000.0D;
	}
	
	private void cleanup()
	{
		// Wait until the server socket is closed
		try
		{
			serverChannel.closeFuture().sync();
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
		
		// Save the world
		//worldSave.save();
	}
	
	private void update(float delta)
	{
		player.move(0.0f, 1.0f);
		otherPlayer.rotate(1.0f * delta, 1.0f * delta);
		world.update(delta);
	}
	
	public int addClient(Channel channel)
	{
		clientChannels.add(channel);
		return nextClientID++;
	}
	
	public void removeClient(Channel channel, int clientID)
	{
		clientChannels.add(channel);
	}
	
}
