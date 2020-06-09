package ddb.io.voxelnet.serial;

/**
 * Common type for all things that can be values inside of 'SeList's and 'SeBlock's
 */
public abstract class SeValue implements ISerialize
{

	/*--* Conversions *--*/
	
	public byte      asByte()         { throw new IllegalStateException("Mismatched type"); }
	public boolean   asBoolean()      { throw new IllegalStateException("Mismatched type"); }
	public short     asShort()        { throw new IllegalStateException("Mismatched type"); }
	public char      asChar()         { throw new IllegalStateException("Mismatched type"); }
	public int       asInt()          { throw new IllegalStateException("Mismatched type"); }
	public long      asLong()         { throw new IllegalStateException("Mismatched type"); }
	public float     asFloat()        { throw new IllegalStateException("Mismatched type"); }
	public double    asDouble()       { throw new IllegalStateException("Mismatched type"); }
	
	public byte[]    asByteArray()    { throw new IllegalStateException("Mismatched type"); }
	public boolean[] asBooleanArray() { throw new IllegalStateException("Mismatched type"); }
	public short[]   asShortArray()   { throw new IllegalStateException("Mismatched type"); }
	public char[]    asCharArray()    { throw new IllegalStateException("Mismatched type"); }
	public int[]     asIntArray()     { throw new IllegalStateException("Mismatched type"); }
	public long[]    asLongArray()    { throw new IllegalStateException("Mismatched type"); }
	public float[]   asFloatArray()   { throw new IllegalStateException("Mismatched type"); }
	public double[]  asDoubleArray()  { throw new IllegalStateException("Mismatched type"); }
	
	public String    asString()       { throw new IllegalStateException("Mismatched type"); }
	public SeBlock   asBlock()        { throw new IllegalStateException("Mismatched type"); }
	public SeList    asList()         { throw new IllegalStateException("Mismatched type"); }
	
}
