package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for short arrays
 */
public class SeShortArrayValue extends SeArrayValue
{
	private short[] value;
	
	// Empty constructor
	SeShortArrayValue() {}
	
	public SeShortArrayValue(short[] value)
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
		
		for (short value : value)
			output.writeShort(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new short[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readShort();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength() * Short.BYTES;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.SHORT_ARRAY;
	}
	
	@Override
	public short[] asShortArray()
	{
		return value;
	}
}
