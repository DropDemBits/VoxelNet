package ddb.io.voxelnet.client;

public enum ConnectionState
{
	// Connection is closed or just created, communication cannot proceed
	CLOSED,
	// Connection process has started
	CONNECTING,
	// Connection process has finished, connection is ready for communication
	ESTABLISHED,
}
