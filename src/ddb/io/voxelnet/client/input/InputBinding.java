package ddb.io.voxelnet.client.input;

/**
 * Representation of an input binding
 *
 * An input binding can recieve input from a few input sources
 * - InputSource.KEYBOARD: Listens to key events
 * - InputSource.MOUSE: Listens to mouse button events
 *
 * If an input binding is disabled, the input binding will not be updated
 */
public class InputBinding
{
	
	private InputSource inputSource;
	private int inputCode;
	
	// Whether the input binding is enabled or not
	private boolean isEnabled = true;
	
	// Whether the input binding's input source code is active
	// For a key source, this means that the input binding's key is pressed
	// For a mouse button source, this means that the input binding's button is
	// pressed
	// Associated get is isInputActive
	private boolean isActive;
	
	// The last active state of the button (i.e, the input state one frame
	// before)
	// Used to handle wasInputActivated & wasInputDeactivated
	private boolean lastActiveState;
	
	// The name of the input binding
	private final String bindingName;
	
	/**
	 * Creates a new input binding
	 * @param name The name of the input binding
	 * @param source The input source
	 * @param code The source-specific input code
	 */
	InputBinding(String name, InputSource source, int code)
	{
		this.bindingName = name;
		this.inputSource = source;
		this.inputCode = code;
	}
	
	/**
	 * Changes the enabled state of the input binding
	 * @param isEnabled The new enabled state of the input binding
	 */
	public void setEnabled(boolean isEnabled)
	{
		this.isEnabled = isEnabled;
		
		if (!isEnabled)
		{
			// Reset input state upon being disabled
			this.isActive = false;
			this.lastActiveState = false;
		}
	}
	
	/**
	 * Checks if the input binding is enabled
	 * @return True if the input binding is enabled
	 */
	public boolean isEnabled()
	{
		return isEnabled;
	}
	
	/**
	 * Changes the active state of the binding
	 * @param isActive The new active state of the binding
	 */
	public void setActive(boolean isActive)
	{
		// Don't update while disabled
		if (!isEnabled)
			return;
		
		this.lastActiveState = this.isActive;
		this.isActive = isActive;
	}
	
	/**
	 * Gets if the input is active
	 * @return True if the input is active
	 */
	public boolean isInputActive()
	{
		return isActive;
	}
	
	/**
	 * Checks if the input was just activated on the current frame
	 * @return True if the input was just active
	 */
	public boolean wasInputActivated()
	{
		// Rising edge
		return !lastActiveState && isActive;
	}
	
	/**
	 * Checks if the input was just deactivated on the current frame
	 * @return True if the input was just disabled
	 */
	public boolean wasInputDeactivated()
	{
		// Falling edge
		return lastActiveState && !isActive;
	}
	
	/**
	 * Rebinds the input to a different source
	 * @param source The new input source of the input binding
	 * @param code The new input code of the input binding
	 */
	public void rebindInput(InputSource source, int code)
	{
		this.inputSource = source;
		this.inputCode = code;
	}
	
	/**
	 * Gets the input binding's associated input source
	 * @return The associated input source
	 */
	public InputSource getInputSource()
	{
		return inputSource;
	}
	
	/**
	 * Gets the input binding's associated input code
	 * @return The associated input code
	 */
	public int getInputCode()
	{
		return inputCode;
	}
	
	/**
	 * Gets the name of the binding
	 * @return The name of the binding
	 */
	public String getName()
	{
		return bindingName;
	}
	
	// ???: Should there be input handling analog inputs? (e.g. scroll wheel, mouse movement)
	
}
