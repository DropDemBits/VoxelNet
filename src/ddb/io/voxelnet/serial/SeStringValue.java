package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Value container for strings
 */
class SeStringValue extends SeValue
{
	private String value;
	private int computedSize;
	
	// Empty constructor
	SeStringValue() {}
	
	SeStringValue(String value)
	{
		super();
		
		this.value = value;
		
		computedSize = SeUtil.getVarIntSize(value.length()) + value.getBytes(StandardCharsets.UTF_8).length;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write out the value
		SeUtil.writeString(value, output);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		value = SeUtil.readString(input);
		computedSize = SeUtil.getVarIntSize(value.length()) + value.getBytes(StandardCharsets.UTF_8).length;
		return true;
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.STRING;
	}
	
	@Override
	public int getComputedSize()
	{
		return computedSize;
	}
	
	@Override
	public String asString()
	{
		return value;
	}
}
