package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for an int
 */
class SeIntValue extends SeValue
{
	private int value;
	
	// Empty constructor
	SeIntValue() {}
	
	SeIntValue(int value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the int
		output.writeInt(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readInt();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.INT;
	}
	
	@Override
	public int getComputedSize()
	{
		return Integer.BYTES;
	}
	
	@Override
	public int asInt()
	{
		return value;
	}
}
