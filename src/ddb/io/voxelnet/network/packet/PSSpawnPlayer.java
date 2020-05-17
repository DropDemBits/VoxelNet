package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public class PSSpawnPlayer extends Packet
{
	public int clientID;
	
	public PSSpawnPlayer() {}
	
	public PSSpawnPlayer(int clientID)
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
		return 2;
	}
}
