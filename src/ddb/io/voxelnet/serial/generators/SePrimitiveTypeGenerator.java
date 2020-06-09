package ddb.io.voxelnet.serial.generators;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SePrimitiveTypeGenerator
{
	public static void main(String[] args)
	{
		List<ValueTypeInfo> types = Arrays.asList(
				new ValueTypeInfo("byte",   "Byte",    "BYTE", "Byte.BYTES"),
				new ValueTypeInfo("boolean","Boolean", "BOOLEAN", "1"),
				new ValueTypeInfo("short",  "Short",   "SHORT", "Short.BYTES"),
				new ValueTypeInfo("char",   "Char",    "CHAR", "Character.BYTES"),
				new ValueTypeInfo("int",    "Int",     "INT", "Integer.BYTES"),
				new ValueTypeInfo("float",  "Float",   "FLOAT", "Float.BYTES"),
				new ValueTypeInfo("long",   "Long",    "LONG", "Long.BYTES"),
				new ValueTypeInfo("double", "Double",  "DOUBLE", "Double.BYTES")
				);
		
		for (ValueTypeInfo info : types)
		{
			String filename = String.format("Se%sValue.java", info.className);
			try(FileWriter writter = new FileWriter(filename))
			{
				writter.append(generateSource(info));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private static class ValueTypeInfo
	{
		final String typeName, className, enumName, size;
		
		private ValueTypeInfo(String typeName, String className, String enumName, String size)
		{
			this.typeName = typeName;
			this.className = className;
			this.enumName = enumName;
			this.size = size;
		}
	}
	
	private static String generateSource(ValueTypeInfo info)
	{
		String typeName = info.typeName;
		String className = info.className;
		String enumName = info.enumName;
		String size = info.size;
		
		return "package ddb.io.voxelnet.serial;\n" +
				"\n" +
				"import java.io.DataInputStream;\n" +
				"import java.io.DataOutputStream;\n" +
				"import java.io.IOException;\n" +
				"\n" +
				"/**\n" +
				String.format(" * Value container for %ss\n", typeName) +
				" */\n" +
				String.format("class Se%sValue extends SeValue\n", className) +
				"{\n" +
				String.format("	private %s value;\n", typeName) +
				"	\n" +
				"	// Empty constructor\n" +
				String.format("	Se%sValue() {}\n", className) +
				"	\n" +
				String.format("	Se%sValue(%s value)\n", className, typeName) +
				"	{\n" +
				"		super();\n" +
				"		\n" +
				"		this.value = value;\n" +
				"	}\n" +
				"	\n" +
				"	@Override\n" +
				"	public void serializeTo(DataOutputStream output) throws IOException\n" +
				"	{\n" +
				"		// Write out the value\n" +
				String.format("		output.write%s(value);\n", className) +
				"	}\n" +
				"	\n" +
				"	@Override\n" +
				"	public boolean deserializeFrom(DataInputStream input) throws IOException\n" +
				"	{\n" +
				String.format("		value = input.read%s();\n", className) +
				"		return true;\n" +
				"	}\n" +
				"	\n" +
				"	@Override\n" +
				"	public SeDataTypes getSerializeType()\n" +
				"	{\n" +
				String.format("		return SeDataTypes.%s;\n", enumName) +
				"	}\n" +
				"	\n" +
				"	@Override\n" +
				"	public int getComputedSize()\n" +
				"	{\n" +
				String.format("		return %s;\n", size) +
				"	}\n" +
				"	\n" +
				"	@Override\n" +
				String.format("	public %s as%s()\n", typeName, className) +
				"	{\n" +
				"		return value;\n" +
				"	}\n" +
				"}\n";
	}
}

