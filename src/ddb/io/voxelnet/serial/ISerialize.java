package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Base Serialization Interface
 */
public interface ISerialize
{
	/**
	 * Serializes the object to the output stream
	 * @param output The stream to serialize to
	 * @throws IOException If any errors occur during serialization
	 */
	void serializeTo(DataOutputStream output) throws IOException;
	
	/**
	 * Deserialzes the object's data from the input stream
	 * @param input The stream to serialize from
	 * @return True if deserialization was successful
	 * @throws IOException If any errors occur during serialization
	 */
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
