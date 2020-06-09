package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for byte arrays
 */
public class SeByteArrayValue extends SeArrayValue
{
	private byte[] value;
	
	// Empty constructor
	SeByteArrayValue() {}
	
	public SeByteArrayValue(byte[] value)
	{
		super(value.length);
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		super.serializeTo(output);
		
		if (getLength() == 0)
			return;
		
		for (byte value : value)
			output.writeByte(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new byte[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readByte();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength() * Byte.BYTES;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BYTE_ARRAY;
	}
	
	@Override
	public byte[] asByteArray()
	{
		return value;
	}
}
