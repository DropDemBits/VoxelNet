package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for doubles
 */
class SeDoubleValue extends SeValue
{
	private double value;
	
	// Empty constructor
	SeDoubleValue() {}
	
	SeDoubleValue(double value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeDouble(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readDouble();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.DOUBLE;
	}
	
	@Override
	public int getComputedSize()
	{
		return Double.BYTES;
	}
	
	@Override
	public double asDouble()
	{
		return value;
	}
}
