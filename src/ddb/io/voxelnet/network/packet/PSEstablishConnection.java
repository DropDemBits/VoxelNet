package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public class PSEstablishConnection implements Packet
{
	public int clientID;
	
	PSEstablishConnection() {}
	
	public PSEstablishConnection(int clientID)
	{
		this.clientID = clientID;
	}
	
	@SuppressWarnings("unused")
	@Override
	public Packet makeEmptyPacket()
	{
		return new PSEstablishConnection();
	}
	
	@Override
	public void decodePayload(ByteBuf data)
	{
		clientID = data.readInt();
	}
	
	@Override
	public void encodePayload(ByteBuf data)
	{
		data.writeInt(clientID);
	}
	
	@Override
	public int getPacketID()
	{
		return 0;
	}
	
}
