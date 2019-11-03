package ddb.io.voxelnet.fluid;

import ddb.io.voxelnet.util.Vec3i;

public class FluidPlacement
{
	public final Vec3i pos;
	public byte newMeta;
	
	public FluidPlacement(Vec3i pos, byte newMeta)
	{
		this.pos = pos;
		this.newMeta = newMeta;
	}
	
	@Override
	public int hashCode()
	{
		return pos.hashCode();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof FluidPlacement))
			return false;
		return pos.equals(obj);
	}
}
