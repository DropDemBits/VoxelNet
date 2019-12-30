package ddb.io.voxelnet.network;

import ddb.io.voxelnet.entity.EntityPlayer;
import io.netty.buffer.ByteBuf;

public class PCSPosRotUpdate extends Packet
{
	public int clientID;
	public float xPos;
	public float yPos;
	public float zPos;
	public float xVel;
	public float yVel;
	public float zVel;
	public float pitch;
	public float yaw;
	
	public PCSPosRotUpdate() {}
	
	public PCSPosRotUpdate(int clientID, EntityPlayer entity)
	{
		this(clientID,
				entity.xPos, entity.yPos, entity.zPos,
				entity.xVel, entity.yVel, entity.zVel,
				entity.pitch, entity.yaw);
	}
	
	public PCSPosRotUpdate(int clientID,
	                       float xPos, float yPos, float zPos,
	                       float xVel, float yVel, float zVel,
	                       float pitch, float yaw)
	{
		this.clientID = clientID;
		
		// Position
		this.xPos = xPos;
		this.yPos = yPos;
		this.zPos = zPos;
		
		// Velocity
		this.xVel = xVel;
		this.yVel = yVel;
		this.zVel = zVel;
		
		// Rotation
		this.pitch = pitch;
		this.yaw = yaw;
	}
	
	@Override
	public void decodePayload(ByteBuf data)
	{
		this.clientID = data.readInt();
		
		// Position
		this.xPos     = data.readFloat();
		this.yPos     = data.readFloat();
		this.zPos     = data.readFloat();
		
		// Velocity
		this.xVel     = data.readFloat();
		this.yVel     = data.readFloat();
		this.zVel     = data.readFloat();
		
		// Rotation
		this.pitch    = data.readFloat();
		this.yaw      = data.readFloat();
	}
	
	@Override
	public void encodePayload(ByteBuf data)
	{
		data.writeInt(clientID);
		
		// Position
		data.writeFloat(xPos);
		data.writeFloat(yPos);
		data.writeFloat(zPos);
		
		// Velocity
		data.writeFloat(xVel);
		data.writeFloat(yVel);
		data.writeFloat(zVel);
		
		// Rotation
		data.writeFloat(pitch);
		data.writeFloat(yaw);
	}
	
	@Override
	public int getPacketID()
	{
		return 1;
	}
}
