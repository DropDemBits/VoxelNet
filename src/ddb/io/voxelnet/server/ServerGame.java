package ddb.io.voxelnet.server;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.event.EventBus;
import ddb.io.voxelnet.fluid.Fluid;
import ddb.io.voxelnet.world.World;
import ddb.io.voxelnet.world.WorldSave;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerGame
{
	public boolean isRunning = false;
	
	private ServerSettings settings = null;
	
	WorldSave worldSave;
	public World world;
	
	double elapsed = 0.0d;
	
	double updTime = 0;
	double currentUPD = 0;
	
	// Global Event Bus
	public static final EventBus GLOBAL_BUS = new EventBus();
	
	// Networking Things //
	ServerNetworkManager networkManager;
	
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
			
			// Initialize the server network state
			networkManager = new ServerNetworkManager(instance, settings.hostPort);
			if (!networkManager.init())
			{
				// Shutdown and return
				cleanup();
				return;
			}
			
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
	
	private void init()
	{
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
		System.out.println("Shutting down");
		networkManager.shutdown();
		
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
		networkManager.update();
	}
	
	public ServerNetworkManager getNetworkManager()
	{
		return networkManager;
	}
}
