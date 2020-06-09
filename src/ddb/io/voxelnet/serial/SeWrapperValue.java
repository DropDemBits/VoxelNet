package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Wrapper value for a serializable type
 * @param <T> The serializable type to wrap in a value
 */
class SeWrapperValue<T extends ISerialize> extends SeValue
{
	private final T wrappedValue;
	
	SeWrapperValue(T wrappedValue)
	{
		this.wrappedValue = wrappedValue;
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		wrappedValue.serializeTo(output);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		return wrappedValue.deserializeFrom(input);
	}
	
	@Override
	public int getComputedSize()
	{
		return wrappedValue.getComputedSize();
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return wrappedValue.getSerializeType();
	}
	
	@Override
	public SeBlock asBlock()
	{
		if (wrappedValue instanceof SeBlock)
			return (SeBlock) wrappedValue;
		
		// Fail
		return super.asBlock();
	}
	
	@Override
	public SeList asList()
	{
		if (wrappedValue instanceof SeList)
			return (SeList) wrappedValue;
		
		// Fail
		return super.asList();
	}
	
}
