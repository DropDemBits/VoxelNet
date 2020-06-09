package ddb.io.voxelnet.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Helper class containing utilities
 */
public class SeUtil
{
	// Generic empty value
	public static final SeEmptyValue EMPTY_VALUE = new SeEmptyValue();
	// Maximum length of an encodable string
	public static final int MAX_STRLEN = 2 << (16 - 2);
	
	private SeUtil() {}
	
	/**
	 * Writes the serializable object to the output stream
	 * Prepends the type tag byte before the serialized data
	 * @param serialize The object to serialize
	 * @param output The output stream
	 */
	public static void writeTo(ISerialize serialize, DataOutputStream output) throws IOException
	{
		// Skip size before Type tag
		writeVarInt(serialize.getComputedSize(), output);
		// Type tag before data block
		output.write((byte)serialize.getSerializeType().ordinal());
		// Data block
		serialize.serializeTo(output);
	}
	
	/**
	 * Reads and deserializes the serializable object from the input stream
	 * and gets the associated value kind
	 * Assumes that the skip count (VarInt) has already been read
	 *
	 * @param input The input stream
	 * @throws IOException Any IO errors thrown during reading
	 * @return serialize The deserialized object
	 */
	public static SeValue readIntoValue(DataInputStream input) throws IOException
	{
		// (Skip has already been read)
		
		// Fetch the tag type
		SeDataTypes decodeType = SeDataTypes.values()[Byte.toUnsignedInt(input.readByte())];
		SeValue valueInstance = decodeType.getNewInstance();
		
		// Try to deserialize
		if (!valueInstance.deserializeFrom(input))
			return null;
		
		return valueInstance;
	}
	
	public static void writeString(String string, DataOutputStream output) throws IOException, IllegalArgumentException
	{
		// Warn of truncation (should be captured during setValue[At])
		if (string.length() > MAX_STRLEN)
			throw new IllegalArgumentException("String length exceeds maximum encoding length");
		
		int length = Math.min(string.length(), MAX_STRLEN);
		writeVarInt(length, output);
		
		// Write out the string in UTF-8 format
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		output.write(bytes);
	}
	
	public static String readString(DataInputStream input) throws IOException, IllegalArgumentException
	{
		// Read in the length
		int length = readVarInt(input);
		if (length > MAX_STRLEN || length < 0)
			throw new IllegalArgumentException("String length exceeds maximum encoding length");
			
		if (length == 0)
			return "";
		
		// Build the string
		byte[] bytes = new byte[length];
		int bytesRead = input.read(bytes);
		
		if (bytesRead == -1 || bytesRead < length)
			throw new IllegalArgumentException("String length is less than the storage length");
		
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	// Write out a VarInt
	public static void writeVarInt(int value, DataOutputStream output) throws IOException
	{
		// Handle trivial case
		if (value < 0x80)
		{
			output.writeByte(value);
			return;
		}
		
		// Encode the var int using the following format:
		// x000000 (x -> next byte)
		int encodeBits = value;
		int remainingEncode = 32;
		int shiftBits = 4;
		boolean overLeadingZeros = false;
		
		do
		{
			int mask = 0x7F >> (7 - shiftBits);
			int writeOut = (encodeBits >> (32 - shiftBits)) & mask;
			remainingEncode -= 32;
			encodeBits <<= 7;
			shiftBits = 7;
			
			if (!overLeadingZeros && writeOut != 0)
			{
				// Over the leading zeros
				overLeadingZeros = true;
			}
			
			if (remainingEncode != 0)
				// Make continue
				writeOut |= 0x80;
			
			if (overLeadingZeros)
				output.writeByte(writeOut);
		} while (remainingEncode > 0);
	}
	
	// Read a VarInt
	public static int readVarInt(DataInputStream input) throws IOException
	{
		int decodeByte, output = 0;
		
		do
		{
			decodeByte = Byte.toUnsignedInt(input.readByte());
			output <<= 7;
			output |= decodeByte & 0x7F;
		} while ((decodeByte & 0x80) != 0);
		
		return output;
	}
	
	// Computes the number of bytes that a VarInt will use
	public static int getVarIntSize(int value)
	{
		return (1 + Integer.numberOfLeadingZeros(value) / 7);
	}
	
}
