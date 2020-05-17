package ddb.io.voxelnet.network.packet;

import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.RaycastResult;
import io.netty.buffer.ByteBuf;

public class PCSBreakBlock extends Packet
{
	public int clientID;
	public RaycastResult hitResult;
	
	public PCSBreakBlock() {}
	
	public PCSBreakBlock(int clientID, RaycastResult hitResult)
	{
		this.clientID = clientID;
		this.hitResult = hitResult;
	}
	
	@Override
	public void decodePayload(ByteBuf data) throws Exception
	{
		hitResult = new RaycastResult();
		
		clientID = data.readInt();
		
		hitResult.hitX = data.readDouble();
		hitResult.hitY = data.readDouble();
		hitResult.hitZ = data.readDouble();
		
		hitResult.blockX = data.readInt();
		hitResult.blockY = data.readInt();
		hitResult.blockZ = data.readInt();
		
		hitResult.face = Facing.values()[data.readUnsignedByte()];
	}
	
	@Override
	public void encodePayload(ByteBuf data) throws Exception
	{
		data.writeInt(clientID);
		
		// Sub-block position
		data.writeDouble(hitResult.hitX);
		data.writeDouble(hitResult.hitY);
		data.writeDouble(hitResult.hitZ);
		
		// Block position
		data.writeInt(hitResult.blockX);
		data.writeInt(hitResult.blockY);
		data.writeInt(hitResult.blockZ);
		
		// Hit face
		data.writeByte(hitResult.face.ordinal());
	}
	
	@Override
	public int getPacketID()
	{
		return 6;
	}
	
}
