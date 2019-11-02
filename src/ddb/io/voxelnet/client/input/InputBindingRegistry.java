package ddb.io.voxelnet.client.input;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry of all the input bindings
 */
public class InputBindingRegistry
{
	
	public static final List<InputBinding> ALL_INPUT_BINDS = new ArrayList<>();
	
	/**
	 * Registers a new input binding
	 * @param name The name of the input binding
	 * @param source The source that the input lives in
	 * @param initialCode The initial source-specific code of the input binding
	 * @return The instance of the input binding
	 */
	public static InputBinding registerInputBinding(String name, InputSource source, int initialCode)
	{
		InputBinding binding = new InputBinding(name, source, initialCode);
		ALL_INPUT_BINDS.add(binding);
		return binding;
	}
	
}
