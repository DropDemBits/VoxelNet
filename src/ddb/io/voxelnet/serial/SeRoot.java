package ddb.io.voxelnet.serial;

import java.io.*;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Root of a serialization tree
 */
public class SeRoot implements ISerialize
{
	private static final byte[] HEADER_MAGIC = "SERT".getBytes();
	private static final byte[] HEADER_END_MAGIC = "TRES".getBytes();
	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 0;
	public static final int MAX_DATA_SIZE = 4 * 1024 * 1024;
	
	// Flags
	public static final int FLAG_COMPRESSED = 0x00000001;
	
	// Actual data block
	private final SeBlock data;
	
	/**
	 * Creates a new serialization tree
	 */
	public SeRoot()
	{
		this.data = new SeBlock();
	}
	
	@Override
	public void serializeTo(DataOutputStream output) throws IOException
	{
		// Write the header
		// | header_start_magic (int)
		// | padding
		// | majorVersion (int)
		// | minorVersion (int)
		// | flags (int, bitfield)
		
		// If bit 0 (compressed) is set:
		// | decompressedSize (int)
		
		// | header_end_magic (int)
		// | padding
		
		// Magic
		output.write(HEADER_MAGIC);
		output.writeInt(0);
		
		// Version
		output.writeInt(MAJOR_VERSION);
		output.writeInt(MINOR_VERSION);
		
		// Flags (none)
		output.writeInt(0);
		
		// Footer Magic
		output.write(HEADER_END_MAGIC);
		output.writeInt(0);
		
		// Write the serial data
		output.writeByte((byte)data.getSerializeType().ordinal());
		data.serializeTo(output);
	}
	
	@Override
	public boolean deserializeFrom(DataInputStream input) throws IOException
	{
		// Verify the header
		byte[] headerBuf = new byte[4];
		input.read(headerBuf);
		input.readInt();
		
		// Headers do not match
		if (!Arrays.equals(headerBuf, HEADER_MAGIC))
			return false;
		
		// Verify version
		if (input.readInt() != MAJOR_VERSION ||
		    input.readInt() != MINOR_VERSION)
			return false;
		
		// Read in the flags
		int flags = input.readInt();
		
		// Read in decompression
		int decompressedSize = 0;
		if ((flags & FLAG_COMPRESSED) != 0)
			decompressedSize = input.readInt();
		
		if (decompressedSize > MAX_DATA_SIZE)
			throw new IllegalArgumentException("Decompressed data size is greater than the maximum data size of 4 MiB");
		
		// Verify the footer
		input.read(headerBuf);
		input.readInt();
		
		// Footers do not match
		if (!Arrays.equals(headerBuf, HEADER_END_MAGIC))
			return false;
		
		// Decompress (if needed)
		DataInputStream dataStream = input;
		if ((flags & FLAG_COMPRESSED) != 0)
		{
			// Decompress the data
			GZIPInputStream istream = new GZIPInputStream(input);
			byte[] buffer = new byte[decompressedSize];
			int read = istream.read(buffer);
			istream.close();
			
			if (read < 0)
				throw new EOFException();
			
			// Redirect to the new stream
			dataStream = new DataInputStream(new ByteArrayInputStream(buffer));
		}
		
		// Deserialize data root
		if (dataStream.readByte() != SeDataTypes.BLOCK.ordinal())
			return false;
		
		return data.deserializeFrom(dataStream);
	}
	
	@Override
	public int getComputedSize()
	{
		return data.getComputedSize();
	}
	
	@Override
	public SeDataTypes getSerializeType()
	{
		return SeDataTypes.ROOT;
	}
	
	/**
	 * Gets the data block at the root of the tree
	 * @return The root data block
	 */
	public SeBlock getDataBlock()
	{
		return data;
	}
	
}
