package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Value container for chars
 */
class SeCharValue extends SeValue
{
	private char value;
	
	// Empty constructor
	SeCharValue() {}
	
	SeCharValue(char value)
	{
		super();
		
		this.value = value;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		output.writeChar(value);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = input.readChar();
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.CHAR;
	}
	
	@Override
	public int getComputedSize()
	{
		return Character.BYTES;
	}
	
	@Override
	public char asChar()
	{
		return value;
	}
}
