package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for boolean arrays
 */
public class SeBooleanArrayValue extends SeArrayValue
{
	private boolean[] value;
	
	// Empty constructor
	SeBooleanArrayValue() {}
	
	public SeBooleanArrayValue(boolean[] value)
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
		
		for (boolean value : value)
			output.writeBoolean(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new boolean[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readBoolean();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength();
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.BOOLEAN_ARRAY;
	}
	
	@Override
	public boolean[] asBooleanArray()
	{
		return value;
	}
}
