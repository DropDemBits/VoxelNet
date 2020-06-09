package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for booleans
 */
class SeBooleanValue extends SeValue
{
	private boolean value;
	
	// Empty constructor
	SeBooleanValue() {}
	
	SeBooleanValue(boolean value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeBoolean(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readBoolean();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BOOLEAN;
	}
	
	@Override
	public int getComputedSize()
	{
		return 1;
	}
	
	@Override
	public boolean asBoolean()
	{
		return value;
	}
}
