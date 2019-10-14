package ddb.io.voxelnet.event.input;

import ddb.io.voxelnet.event.Event;

/**
 * Base class for all mouse movement events
 */
public abstract class MouseEvent extends Event
{
	
	/**
	 * Representation of a mouse movement event
	 */
	public static class Move extends MouseEvent
	{
		// Mouse location on the screen at the time of the event, in sub-pixels
		public final double x;
		public final double y;
		// Change in the mouse location relative to the last event
		public final double dx;
		public final double dy;
		// Whether the mouse was dragged at the time of this event
		public final boolean wasDragged;
		
		public Move(double x, double y, double dx, double dy, boolean dragged)
		{
			this.x = x;
			this.y = y;
			this.dx = dx;
			this.dy = dy;
			this.wasDragged = dragged;
		}
	}
	
	/**
	 * Representation of a mouse button event
	 */
	public static class Button extends MouseEvent
	{
		public static final int RELEASED = 0;
		public static final int PRESSED  = 1;
		public static final int CLICKED  = 2;
		
		// The mouse button that is related to this event. Maps to the
		// GLFW_BUTTON_ enums
		public final int button;
		// The new state of the mouse button (RELEASED, PRESSED, CLICKED)
		public final int state;
		// The key modifiers that were pressed at the time of the event
		public final int mods;
		// The time of the current mouse event
		public final long time;
		
		public Button(int button, int state, int mods, long time)
		{
			this.button = button;
			this.state = state;
			this.mods = mods;
			this.time = time;
		}
	}
	
}
