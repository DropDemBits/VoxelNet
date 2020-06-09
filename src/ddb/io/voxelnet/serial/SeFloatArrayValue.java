package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for float arrays
 */
public class SeFloatArrayValue extends SeArrayValue
{
	private float[] value;
	
	// Empty constructor
	SeFloatArrayValue() {}
	
	public SeFloatArrayValue(float[] value)
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
		
		for (float value : value)
			output.writeFloat(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new float[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readFloat();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength() * Float.BYTES;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.FLOAT_ARRAY;
	}
	
	@Override
	public float[] asFloatArray()
	{
		return value;
	}
}
