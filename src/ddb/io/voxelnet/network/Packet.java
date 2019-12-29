package ddb.io.voxelnet.network;

import io.netty.buffer.ByteBuf;

public abstract class Packet
{
	
	public abstract void decodePayload(ByteBuf data);
	public abstract void encodePayload(ByteBuf data);
	public abstract int getPacketID();
	
}
