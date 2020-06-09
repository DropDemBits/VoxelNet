package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Group of named serializable values
 */
public class SeBlock implements ISerialize
{
	// Map of values
	private final Map<String, SeValue> valueMap = new LinkedHashMap<>();
	
	// Computed serialized size of the block (including the tag)
	private int computedSize = Integer.BYTES;
	
	// Whether or not the block is being serialized
	// Used to prevent loops in the serialization tree
	// XXX: Make atomic if deserializing with multithreading
	private boolean isSerializing = false;
	
	public SeBlock() {}
	
	// If there is a value with the existing name, it will overwrite that value
	public void setValue(String name, int value)
	{
		addValue(name, new SeIntValue(value));
	}
	
	// Gets the value, returns null if not found
	public SeValue getValue(String name)
	{
		return valueMap.get(name);
	}
	
	// Adds a value to the map, computing the new computed size
	private void addValue(String name, SeValue value)
	{
		computedSize += value.getComputedSize();
		SeValue oldValue = valueMap.put(name, value);
		
		// Recalculate the computed size
		if (oldValue != null)
			computedSize -= oldValue.getComputedSize();
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// If already serializing, create an empty block and return
		// No harm in creating a duplicate empty block to break a cycle in the
		// serialization tree
		if (isSerializing)
		{
			output.write(0);
			return;
		}
		
		isSerializing = true;
		
		// Write value count
		SeUtil.writeVarInt(valueMap.values().size(), output);
		
		// Write out values
		for (Map.Entry<String, SeValue> entry : valueMap.entrySet())
		{
			String key = entry.getKey();
			SeValue value = entry.getValue();
			
			SeUtil.writeTo(value, output);
			SeUtil.writeString(key, output);
		}
		
		isSerializing = false;
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		// Fetch value count
		int valueCount = SeUtil.readVarInt(input);
		
		// If empty, done processing
		if (valueCount == 0)
			return true;
		
		for (int i = 0; i < valueCount; i++)
		{
			// Read the skip (do nothing with it)
			SeUtil.readVarInt(input);
			
			// Produce the key-value pair
			SeValue value = SeUtil.readIntoValue(input);
			String key = SeUtil.readString(input);
			
			// Skip over bad values
			if (value == null)
				continue;
			
			addValue(key, value);
		}
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		// If serializing, a cycle was detected
		// Return nothing
		if (isSerializing)
			return 0;
		
		return computedSize;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BLOCK;
	}
}
