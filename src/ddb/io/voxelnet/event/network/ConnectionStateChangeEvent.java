package ddb.io.voxelnet.event.network;

import ddb.io.voxelnet.event.Event;
import ddb.io.voxelnet.client.ConnectionState;

/**
 * Represents a change in the network state
 * Sent for all changes to the client connection state
 */
public class ConnectionStateChangeEvent extends Event
{
	// The new state for the connection
	public final ConnectionState newState;
	
	public ConnectionStateChangeEvent(ConnectionState newState)
	{
		this.newState = newState;
	}
	
}
