package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Base type for all array values
 */
public abstract class SeArrayValue extends SeValue
{
	private int length;
	
	protected SeArrayValue() {}
	
	protected SeArrayValue(int length)
	{
		this.length = length;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		output.writeInt(length);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		this.length = input.readInt();
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return Integer.BYTES;
	}
	
	/**
	 * Gets the length of the array
	 * @return The length of the array
	 */
	public int getLength()
	{
		return length;
	}
}
