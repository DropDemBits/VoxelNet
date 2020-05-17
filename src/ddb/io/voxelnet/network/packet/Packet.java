package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public abstract class Packet
{
	
	public abstract void decodePayload(ByteBuf data) throws Exception;
	public abstract void encodePayload(ByteBuf data) throws Exception;
	public abstract int getPacketID();
	
}
