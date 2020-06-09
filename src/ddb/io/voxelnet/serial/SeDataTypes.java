package ddb.io.voxelnet.serial;

import java.util.function.Supplier;

public enum SeDataTypes
{
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
	
	/*
	// Array Types
	BYTE_ARRAY,
	BOOLEAN_ARRAY,
	SHORT_ARRAY,
	CHAR_ARRAY,
	INT_ARRAY,
	FLOAT_ARRAY,
	LONG_ARRAY,
	DOUBLE_ARRAY,
	*/
	
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
	 * Gets the decoded value class
	 * @return The associated decoded value class
	 */
	public SeValue getNewInstance()
	{
		return valueCreator.get();
	}
}
