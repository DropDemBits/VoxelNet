package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for shorts
 */
class SeShortValue extends SeValue
{
	private short value;
	
	// Empty constructor
	SeShortValue() {}
	
	SeShortValue(short value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeShort(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readShort();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.SHORT;
	}
	
	@Override
	public int getComputedSize()
	{
		return Short.BYTES;
	}
	
	@Override
	public short asShort()
	{
		return value;
	}
}
