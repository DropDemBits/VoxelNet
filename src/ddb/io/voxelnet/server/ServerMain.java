package ddb.io.voxelnet.server;

public class ServerMain
{
	private static ServerSettings parseArgs(String[] args)
	{
		ServerSettings settings = new ServerSettings();
		settings.hostPort = 7997;
		
		return settings;
	}
	
	public static void main(String... args)
	{
		// Launch the server game into a new thread
		final ServerGame server = new ServerGame();
		
		ServerSettings settings = parseArgs(args);
		server.setSettings(settings);
		new Thread(server::run, "Server-Main").start();
	}
}
