package ddb.io.voxelnet.server;

import ddb.io.voxelnet.network.PCSPosRotUpdate;
import ddb.io.voxelnet.network.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.List;

public class PacketCodec extends ByteToMessageCodec<Packet>
{
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception
	{
		// Convert the msg to a byte stream
		// Length (2B) | PacketID (2B) | Payload
		// Length:   Main Packet Length (2+), excludes packet length bytes
		// PacketID: NumID of the packet
		// Payload:  Packet data
		// Length is already handled by LengthFieldPrepender
		out.writeShort(msg.getPacketID()); // Packet ID
		msg.encodePayload(out);
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
	{
		int packetLength = in.readShort();
		
		if (packetLength < 2 || in.getShort(in.readerIndex()) != 1)
		{
			in.skipBytes(packetLength);
			return;
		}
		
		int packetID = in.readShort();
		Packet packet = new PCSPosRotUpdate();
		packet.decodePayload(in);
		out.add(packet);
	}
	
}
