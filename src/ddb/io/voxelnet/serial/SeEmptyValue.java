package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public class SeEmptyValue extends SeValue
{
	SeEmptyValue() {}
	
	@Override
	public void serializeTo(DataOutputStream output) {}
	
	// Fail to serialize if attempted
	@Override
	public boolean deserializeFrom(DataInputStream input)
	{
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return 0;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.EMPTY;
	}
	
}
