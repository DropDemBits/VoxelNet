package ddb.io.voxelnet.network;

import io.netty.buffer.ByteBuf;

public class PSEstablishConnection extends Packet
{
	public int clientID;
	
	public PSEstablishConnection() {}
	
	public PSEstablishConnection(int clientID)
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
		return 0;
	}
	
}
