package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for long arrays
 */
public class SeLongArrayValue extends SeArrayValue
{
	private long[] value;
	
	// Empty constructor
	SeLongArrayValue() {}
	
	public SeLongArrayValue(long[] value)
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
		
		for (long value : value)
			output.writeLong(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new long[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readLong();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength() * Long.BYTES;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.LONG_ARRAY;
	}
	
	@Override
	public long[] asLongArray()
	{
		return value;
	}
}
