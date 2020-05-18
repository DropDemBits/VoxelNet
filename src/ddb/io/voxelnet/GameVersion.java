package ddb.io.voxelnet;

public class GameVersion
{
	public static final int MAJOR_VERSION = 0;
	public static final int MINOR_VERSION = 0;
	public static final int PATCH_VERSION = 0;
	public static final String VERSION_KIND = "lite-client";
	
	public static String asText()
	{
		return String.format("%d.%d.%d-%s", MAJOR_VERSION, MINOR_VERSION, PATCH_VERSION, VERSION_KIND);
	}
	
}
