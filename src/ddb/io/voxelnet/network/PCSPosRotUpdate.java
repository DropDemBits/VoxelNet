package ddb.io.voxelnet.network;

import ddb.io.voxelnet.entity.EntityPlayer;
import io.netty.buffer.ByteBuf;

public class PCSPosRotUpdate extends Packet
{
	public int clientID;
	public float xPos;
	public float yPos;
	public float zPos;
	public float pitch;
	public float yaw;
	
	public PCSPosRotUpdate() {}
	
	public PCSPosRotUpdate(int clientID, EntityPlayer entity)
	{
		this(clientID, entity.xPos, entity.yPos, entity.zPos, entity.pitch, entity.yaw);
	}
	
	public PCSPosRotUpdate(int clientID,
	                       float xPos, float yPos, float zPos,
	                       float pitch, float yaw)
	{
		this.clientID = clientID;
		this.xPos = xPos;
		this.yPos = yPos;
		this.zPos = zPos;
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	@Override
	public void decodePayload(ByteBuf data)
	{
		this.clientID = data.readInt();
		this.xPos     = data.readFloat();
		this.yPos     = data.readFloat();
		this.zPos     = data.readFloat();
		this.pitch    = data.readFloat();
		this.yaw      = data.readFloat();
	}
	
	@Override
	public void encodePayload(ByteBuf data)
	{
		data.writeFloat(clientID);
		data.writeFloat(xPos);
		data.writeFloat(yPos);
		data.writeFloat(zPos);
		data.writeFloat(pitch);
		data.writeFloat(yaw);
	}
	
	@Override
	public int getPacketID()
	{
		return 1;
	}
}
