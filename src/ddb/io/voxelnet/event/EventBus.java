package ddb.io.voxelnet.event;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Bus for all events
 * Distributes events to event handlers
 */
public class EventBus
{
	// List of all event handlers for the event queue
	private Map<Class<? extends Event>, List<IEventHandler>> eventHandlers;
	private Queue<Event> pendingEvents;
	
	public EventBus()
	{
		eventHandlers = new LinkedHashMap<>();
		pendingEvents = new ConcurrentLinkedQueue<>();
	}
	
	/**
	 * Registers an event with the event bus
	 * @param evt The event class to register
	 */
	public void registerEvent(Class<? extends Event> evt)
	{
		// Don't re-register events
		if (eventHandlers.containsKey(evt))
			return;
		
		// Add the handler list
		eventHandlers.put(evt, new ArrayList<>());
	}
	
	/**
	 * Adds a handler to an event
	 * @param evt The event to handle
	 * @param handler The handler for the specified event
	 */
	public void addHandler(Class<? extends Event> evt, IEventHandler handler)
	{
		// TODO: Add priorities for event handling
		if (!eventHandlers.containsKey(evt))
			throw new IllegalStateException("Event " + evt.getName() + " has not been registered with this event queue");
		
		eventHandlers.get(evt).add(handler);
	}
	
	/**
	 * Adds an event to the event bus
	 * @param e The event to add
	 */
	public void postEvent(Event e)
	{
		pendingEvents.add(e);
	}
	
	/**
	 * Processes all events in the event bus and fires up the associated handlers
	 */
	public void processEvents()
	{
		while(!pendingEvents.isEmpty())
		{
			Event e = pendingEvents.poll();
			List<IEventHandler> handlers = eventHandlers.get(e.getClass());
			
			// Don't send events for empty event queues
			if (handlers == null || handlers.size() == 0)
				continue;
			
			// Send the event to all handlers
			for (IEventHandler handler : handlers)
			{
				handler.handleEvent(e);
			}
		}
	}
	
}
