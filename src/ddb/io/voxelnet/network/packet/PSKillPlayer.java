package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public class PSKillPlayer implements Packet
{
	public int clientID;
	
	PSKillPlayer() {}
	
	public PSKillPlayer(int clientID)
	{
		this.clientID = clientID;
	}
	
	@SuppressWarnings("unused")
	@Override
	public Packet makeEmptyPacket()
	{
		return new PSKillPlayer();
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
		return 3;
	}

}
