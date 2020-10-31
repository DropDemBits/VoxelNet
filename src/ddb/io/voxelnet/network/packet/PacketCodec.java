package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PacketCodec extends ByteToMessageCodec<Packet>
{
	// TODO: Create PacketRegistry
	private static final Map<Integer, Class<? extends Packet>> idToPacket = new LinkedHashMap<>();
	
	static
	{
		idToPacket.put(0, PSEstablishConnection.class);
		idToPacket.put(1, PCSPosRotUpdate.class);
		idToPacket.put(2, PSSpawnPlayer.class);
		idToPacket.put(3, PSKillPlayer.class);
		idToPacket.put(4, PSChunkData.class);
		idToPacket.put(5, PCSPlaceBlock.class);
		idToPacket.put(6, PCSBreakBlock.class);
		idToPacket.put(7, PCLoadChunkColumn.class);
	}
	
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
		
		try
		{
			msg.encodePayload(out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			ctx.fireExceptionCaught(e);
		}
	}
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
	{
		int packetLength = Short.toUnsignedInt(in.readShort());
		
		if (packetLength < 2)
		{
			System.out.println("Skipping bad packet (" + packetLength + ")");
			// Bad length, skip
			in.skipBytes(packetLength);
			return;
		}
		
		// Create the appropriate packet
		int packetID = in.readShort();
		Class<? extends Packet> packetInstance = idToPacket.get(packetID);
		Packet packet;
		
		if (packetInstance == null)
			throw new IllegalArgumentException("Unknown PacketID " + packetID);
		packet = packetInstance.newInstance();
		
		packet.decodePayload(in);
		out.add(packet);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
		ctx.close();
	}
}
