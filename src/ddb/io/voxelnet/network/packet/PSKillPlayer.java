package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public class PSKillPlayer extends Packet
{
	public int clientID;
	
	public PSKillPlayer() {}
	
	public PSKillPlayer(int clientID)
	{
		this.clientID = clientID;
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
