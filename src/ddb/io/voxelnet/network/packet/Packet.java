package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

/**
 * A Packet that can be sent over the network
 */
public abstract class Packet
{
	// Required constructor, PacketCodec generates empty packets
	public Packet() {}
	
	public abstract void decodePayload(ByteBuf data) throws Exception;
	public abstract void encodePayload(ByteBuf data) throws Exception;
	public abstract int getPacketID();
	
}
