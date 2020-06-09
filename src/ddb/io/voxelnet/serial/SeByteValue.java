package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for bytes
 */
class SeByteValue extends SeValue
{
	private byte value;
	
	// Empty constructor
	SeByteValue() {}
	
	SeByteValue(byte value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeByte(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readByte();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BYTE;
	}
	
	@Override
	public int getComputedSize()
	{
		return Byte.BYTES;
	}
	
	@Override
	public byte asByte()
	{
		return value;
	}
}
