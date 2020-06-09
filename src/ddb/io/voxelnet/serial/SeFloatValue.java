package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for floats
 */
class SeFloatValue extends SeValue
{
	private float value;
	
	// Empty constructor
	SeFloatValue() {}
	
	SeFloatValue(float value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeFloat(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readFloat();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.FLOAT;
	}
	
	@Override
	public int getComputedSize()
	{
		return Float.BYTES;
	}
	
	@Override
	public float asFloat()
	{
		return value;
	}
}
