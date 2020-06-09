package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for longs
 */
class SeLongValue extends SeValue
{
	private long value;
	
	// Empty constructor
	SeLongValue() {}
	
	SeLongValue(long value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeLong(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readLong();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.LONG;
	}
	
	@Override
	public int getComputedSize()
	{
		return Long.BYTES;
	}
	
	@Override
	public long asLong()
	{
		return value;
	}
}
