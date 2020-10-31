package ddb.io.voxelnet.network.packet;

import io.netty.buffer.ByteBuf;

public class PCLoadChunkColumn implements Packet
{
	public int columnX;
	public int columnZ;
	
	PCLoadChunkColumn() {}
	
	public PCLoadChunkColumn(int x, int z)
	{
		// Coordinates for the request
		this.columnX = x;
		this.columnZ = z;
	}
	
	@SuppressWarnings("unused")
	@Override
	public Packet makeEmptyPacket()
	{
		return new PCLoadChunkColumn();
	}
	
	@Override
	public void decodePayload(ByteBuf data)
	{
		columnX = data.readInt();
		columnZ = data.readInt();
	}
	
	@Override
	public void encodePayload(ByteBuf data)
	{
		data.writeInt(columnX);
		data.writeInt(columnZ);
	}
	
	@Override
	public int getPacketID()
	{
		return 7;
	}
}
