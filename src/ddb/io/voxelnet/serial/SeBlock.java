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
	
	// Whether or not the block is being serialized
	// Used to prevent loops in the serialization tree
	// XXX: Make atomic if deserializing with multithreading
	private boolean isSerializing = false;
	
	// Whether or not the block is having the size recomputed
	// Used to prevent loops in the serialization tree
	private boolean isComputingSize = false;
	
	public SeBlock() {}
	
	// *--* Primitive values *--* //
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, byte value)
	{
		addValue(name, new SeByteValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, boolean value)
	{
		addValue(name, new SeBooleanValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, short value)
	{
		addValue(name, new SeShortValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, char value)
	{
		addValue(name, new SeCharValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, int value)
	{
		addValue(name, new SeIntValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, float value)
	{
		addValue(name, new SeFloatValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, long value)
	{
		addValue(name, new SeLongValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, double value)
	{
		addValue(name, new SeDoubleValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, String value)
	{
		addValue(name, new SeStringValue(value));
	}
	
	// *--* Array values *--* //
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, byte[] value)
	{
		addValue(name, new SeByteArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, boolean[] value)
	{
		addValue(name, new SeBooleanArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, short[] value)
	{
		addValue(name, new SeShortArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, char[] value)
	{
		addValue(name, new SeCharArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, int[] value)
	{
		addValue(name, new SeIntArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, float[] value)
	{
		addValue(name, new SeFloatArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, long[] value)
	{
		addValue(name, new SeLongArrayValue(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, double[] value)
	{
		addValue(name, new SeDoubleArrayValue(value));
	}
	
	// *--* Compound values *--* //
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, SeBlock value)
	{
		addValue(name, new SeWrapperValue<>(value));
	}
	
	/**
	 * Sets a new value in the block
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param name The name of the value
	 * @param value The associated value
	 */
	public void setValue(String name, SeList value)
	{
		addValue(name, new SeWrapperValue<>(value));
	}
	
	
	/**
	 * Gets a value in the block
	 *
	 * @param name The name of the value to fetch
	 * @return The value wrapper if found, or SeUtil.EMPTY_VALUE value if not
	 */
	public SeValue getValue(String name)
	{
		return valueMap.getOrDefault(name, SeUtil.EMPTY_VALUE);
	}
	
	// Adds a value to the map
	private void addValue(String name, SeValue value)
	{
		valueMap.put(name, value);
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// If already serializing, create an empty block and return
		// No harm in creating a duplicate empty block to break a cycle in the
		// serialization tree
		if (isSerializing)
		{
			output.writeInt(0);
			return;
		}
		
		isSerializing = true;
		
		// Write value count
		output.writeInt(valueMap.values().size());
		
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
		int valueCount = input.readInt();
		
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
		// Return the size
		if (isComputingSize || isSerializing)
			return Integer.BYTES;
		
		int computedSize = Integer.BYTES;
		isComputingSize = true;
		
		computedSize += valueMap.entrySet().parallelStream()
				.mapToInt((entry) -> {
					// Account for tag and skip bytes
					int size = entry.getValue().getComputedSize();
					size += SeUtil.getVarIntSize(size);
					size += 1;
					
					// Account for the string size
					size += SeUtil.getStringSize(entry.getKey());
					
					return size;
				})
				.reduce(Integer::sum)
				.orElse(0);
		
		isComputingSize = false;
		
		return computedSize;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BLOCK;
	}
}
