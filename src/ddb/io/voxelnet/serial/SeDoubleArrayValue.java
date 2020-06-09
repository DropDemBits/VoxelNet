package ddb.io.voxelnet.serial;
 
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
 
/**
 * Wrapper for double arrays
 */
public class SeDoubleArrayValue extends SeArrayValue
{
	private double[] value;
	
	// Empty constructor
	SeDoubleArrayValue() {}
	
	public SeDoubleArrayValue(double[] value)
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
		
		for (double value : value)
			output.writeDouble(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		super.deserializeFrom(input);
		
		value = new double[getLength()];
		
		if (getLength() == 0)
			return true;
		
		for (int i = 0; i < getLength(); i++)
			value[i] = input.readDouble();
		
		return true;
	}
	
	@Override
	public int getComputedSize()
	{
		return super.getComputedSize() + getLength() * Double.BYTES;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.DOUBLE_ARRAY;
	}
	
	@Override
	public double[] asDoubleArray()
	{
		return value;
	}
}
