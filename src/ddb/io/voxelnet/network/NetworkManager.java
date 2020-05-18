package ddb.io.voxelnet.network;

import ddb.io.voxelnet.network.packet.Packet;
import ddb.io.voxelnet.util.EntityIDMap;

/**
 * Common network manager interface
 */
public interface NetworkManager
{
	/**
	 * Initializes the network manager to be ready for network communication
	 *
	 * A network manager can be reinitialized after it has been shutdown
	 * @return True if the initialization process was successful, false otherwise
	 */
	boolean init();
	
	/**
	 * Shuts down the network manager
	 */
	void shutdown();
	
	/**
	 * Updates the network manager functionality
	 */
	void update();
	
	/**
	 * Gets the mapping for the network ids
	 * @return The mappings between network ids and entities
	 */
	EntityIDMap getNetworkIDMap();
	
	/**
	 * Checks if the network manager is for a client instance
	 * @return True if the network manager is associated with a client instance
	 */
	boolean isClient();
	
	/**
	 * Handles the given packet
	 * @param packet The packet to handle
	 */
	void handlePacket(Packet packet);
	
}
