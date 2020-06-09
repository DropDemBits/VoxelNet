package ddb.io.voxelnet.serial;

import java.util.function.Supplier;

/**
 * Collection of all of the serializable data types
 */
public enum SeDataTypes
{
	// Empty value
	EMPTY (SeEmptyValue::new),
	
	// Non-array types
	BYTE    (SeByteValue::new),
	BOOLEAN (SeBooleanValue::new),
	SHORT   (SeShortValue::new),
	CHAR    (SeCharValue::new),
	INT     (SeIntValue::new),
	FLOAT   (SeFloatValue::new),
	LONG    (SeLongValue::new),
	DOUBLE  (SeDoubleValue::new),
	STRING  (SeStringValue::new),
	
	// Array Types
	BYTE_ARRAY    (SeByteArrayValue::new),
	BOOLEAN_ARRAY (SeBooleanArrayValue::new),
	SHORT_ARRAY   (SeShortArrayValue::new),
	CHAR_ARRAY    (SeCharArrayValue::new),
	INT_ARRAY     (SeIntArrayValue::new),
	FLOAT_ARRAY   (SeFloatArrayValue::new),
	LONG_ARRAY    (SeLongArrayValue::new),
	DOUBLE_ARRAY  (SeDoubleArrayValue::new),
	
	// Compound types
	ROOT (() -> new SeWrapperValue<>(new SeRoot())),
	BLOCK(() -> new SeWrapperValue<>(new SeBlock())),
	LIST (() -> new SeWrapperValue<>(new SeList()));
	
	private final Supplier<? extends SeValue> valueCreator;
	
	SeDataTypes(Supplier<? extends SeValue> valueCreator)
	{
		this.valueCreator = valueCreator;
	}
	
	/**
	 * Gets a new instance of the associated class
	 * @return An instance of the associated value class
	 */
	public SeValue getNewInstance()
	{
		return valueCreator.get();
	}
}
