package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Base Serialization Interface
 */
public interface ISerialize
{
	void serializeTo(DataOutputStream output) throws IOException;
	boolean deserializeFrom(DataInputStream input) throws IOException;
	
	/**
	 * Compute the size of the serialized data block
	 * @return The computed size
	 */
	int getComputedSize();
	
	/**
	 * Gets the type that will be serialized into
	 * @return The serialized type
	 */
	SeDataTypes getSerializeType();
}
