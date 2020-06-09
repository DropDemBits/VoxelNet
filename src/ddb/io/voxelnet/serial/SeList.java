package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Generic array list of serializable values
 */
public class SeList implements ISerialize
{
	public static final int MAX_LIST_SIZE = 2 << (24 - 3);
	
	// Empty Constructor (for serialization)
	SeList() {}
	
	private SeValue[] values;
	private int length = 0;
	
	// Whether or not the list is being serialized
	// Used to prevent loops in the serialization tree
	// XXX: Make atomic if deserializing with multithreading
	private boolean isSerializing = false;
	
	// Whether or not the list is having the size recomputed
	// Used to prevent loops in the serialization tree
	private boolean isComputingSize = false;
	
	/**
	 * Creates a new array list
	 * @param maxSize The maximum number of elements in the array
	 */
	public SeList(int maxSize)
	{
		if (maxSize > MAX_LIST_SIZE)
			throw new IllegalArgumentException("Max size is greater than the maximum number of elements");
		
		this.length = maxSize;
		this.values = new SeValue[maxSize];
	}
	
	// *--* Primitive values *--* //
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, byte value)
	{
		replaceValueAt(index, new SeByteValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, boolean value)
	{
		replaceValueAt(index, new SeBooleanValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, short value)
	{
		replaceValueAt(index, new SeShortValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, char value)
	{
		replaceValueAt(index, new SeCharValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, int value)
	{
		replaceValueAt(index, new SeIntValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, float value)
	{
		replaceValueAt(index, new SeFloatValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, long value)
	{
		replaceValueAt(index, new SeLongValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * Indexes are not checked
	 * 
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, double value)
	{
		replaceValueAt(index, new SeDoubleValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 * 
	 * Indexes are not checked
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, String value)
	{
		replaceValueAt(index, new SeStringValue(value));
	}
	
	// *--* Array values *--* //
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, byte[] value)
	{
		replaceValueAt(index, new SeByteArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, boolean[] value)
	{
		replaceValueAt(index, new SeBooleanArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, short[] value)
	{
		replaceValueAt(index, new SeShortArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, char[] value)
	{
		replaceValueAt(index, new SeCharArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, int[] value)
	{
		replaceValueAt(index, new SeIntArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, float[] value)
	{
		replaceValueAt(index, new SeFloatArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, long[] value)
	{
		replaceValueAt(index, new SeLongArrayValue(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, double[] value)
	{
		replaceValueAt(index, new SeDoubleArrayValue(value));
	}
	
	// *--* Compound values *--* //
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 * 
	 * Indexes are not checked
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, SeBlock value)
	{
		replaceValueAt(index, new SeWrapperValue<>(value));
	}
	
	/**
	 * Sets a new value in the list
	 * If there is a value with the existing name, it will overwrite that value
	 * 
	 * Indexes are not checked
	 *
	 * @param index The index of the value
	 * @param value The associated value
	 */
	public void setValueAt(int index, SeList value)
	{
		replaceValueAt(index, new SeWrapperValue<>(value));
	}
	
	
	/**
	 * Gets a value in the list
	 * Indexes are not checked
	 *
	 * @param index The index of the value to fetch
	 * @return The value wrapper if found, or null if not
	 */
	public SeValue getValueAt(int index)
	{
		return (values[index] == null) ? SeUtil.EMPTY_VALUE : values[index];
	}
	
	
	private void replaceValueAt(int index, SeValue value)
	{
		values[index] = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Break recursive loops
		if(isSerializing)
		{
			output.writeInt(0);
			return;
		}
		
		isSerializing = true;
		
		// Write the length
		output.writeInt(length);
		
		// Empty arrays get nothing
		if (length == 0)
			return;
		
		// Write out the values
		for (SeValue value : values)
		{
			if (value == null)
				SeUtil.writeTo(new SeEmptyValue(), output);
			else
				SeUtil.writeTo(value, output);
		}
		
		isSerializing = false;
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		this.length = input.readInt();
		
		// If over the array size, don't continue
		if (this.length > MAX_LIST_SIZE)
			return false;
		
		this.values = new SeValue[this.length];
		
		// No size array, while invalid, should not cause an error
		if (this.length == 0)
			return true;
		
		// Read in the values
		for (int i = 0; i < this.length; i++)
		{
			// Skip over skipper
			SeUtil.readVarInt(input);
			SeValue value = SeUtil.readIntoValue(input);
			
			// Skip over empty values
			if (value instanceof SeEmptyValue)
				continue;
			
			replaceValueAt(i, value);
		}
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		// If cycled over, only return the size of the length
		if(isComputingSize)
			return Integer.BYTES;
		
		int computedSize = Integer.BYTES;
		isComputingSize = true;
		
		computedSize += Arrays.stream(values).filter(Objects::nonNull)
				.mapToInt((i) -> {
					// Account for tag & skip bytes
					int size = i.getComputedSize();
					size += SeUtil.getVarIntSize(size);
					size += 1;
					
					return size;
				})
				.reduce(Integer::sum)
				.orElse(0);
		
		computedSize += Arrays.stream(values).filter(Objects::isNull).count() * 3;
		
		isComputingSize = false;
		
		return computedSize;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.LIST;
	}
	
	/**
	 * Gets the length of the list
	 * @return The length of the list
	 */
	public int getLength()
	{
		return length;
	}
}
