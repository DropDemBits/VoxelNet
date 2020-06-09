package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Generic array list of serializable values
 */
public class SeList implements ISerialize
{
	@Override
	public void serializeTo(DataOutputStream output)
	{
	
	}
	
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
		return SeDataTypes.LIST;
	}
}
