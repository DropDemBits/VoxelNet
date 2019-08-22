package ddb.io.voxelnet.event.input;

import ddb.io.voxelnet.event.Event;

public class KeyEvent extends Event
{
	/**
	 * Key pressed / released event
	 */
	public static class Button extends KeyEvent
	{
		public static final int RELEASED = 0;
		public static final int PRESSED = 1;
		public static final int REPEATED = 2;
		
		// The keyboard-specific keycode of the event
		public int keycode;
		// The scancode of the event
		public int scancode;
		// The new state of the key
		public int state;
		// The modifiers pressed at the time of the event
		public int mods;
		
		public Button(int keycode, int scancode, int state, int mods)
		{
			this.keycode = keycode;
			this.scancode = scancode;
			this.state = state;
			this.mods = mods;
		}
	}
	
}
