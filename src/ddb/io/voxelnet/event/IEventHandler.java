package ddb.io.voxelnet.event;

/**
 * Represents a single event handler
 */
public interface IEventHandler
{
	/**
	 * Handles a single event
	 * @param e The event to handle
	 */
	void handleEvent(Event e);
}
