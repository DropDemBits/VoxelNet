package ddb.io.voxelnet.network.packet;

import ddb.io.voxelnet.entity.EntityPlayer;
import io.netty.buffer.ByteBuf;

public class PCSPosRotUpdate implements Packet
{
	public int clientID;
	public float xPos;
	public float yPos;
	public float zPos;
	public float xVel;
	public float yVel;
	public float zVel;
	public float xAccel;
	public float zAccel;
	public float pitch;
	public float yaw;
	
	// Player status
	public boolean isFlying;
	public boolean isSprinting;
	public boolean isSneaking;
	
	PCSPosRotUpdate() {}
	
	public PCSPosRotUpdate(int clientID, EntityPlayer entity)
	{
		this(clientID,
				entity.xPos, entity.yPos, entity.zPos,
				entity.xVel, entity.yVel, entity.zVel,
				entity.xAccel, entity.zAccel,
				entity.pitch, entity.yaw,
				entity.isFlying, entity.isSprinting, entity.isSneaking);
	}
	
	public PCSPosRotUpdate(int clientID,
	                       float xPos, float yPos, float zPos,
	                       float xVel, float yVel, float zVel,
	                       float xAccel, float zAccel,
	                       float pitch, float yaw,
	                       boolean isFlying, boolean isSprinting, boolean isSneaking)
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
		
		// Horizontal Acceleration
		this.xAccel = xAccel;
		this.zAccel = zAccel;
		
		// Rotation
		this.pitch = pitch;
		this.yaw = yaw;
		
		// Player status
		this.isFlying = isFlying;
		this.isSneaking = isSneaking;
		this.isSprinting = isSprinting;
	}
	
	@SuppressWarnings("unused")
	@Override
	public Packet makeEmptyPacket()
	{
		return new PCSPosRotUpdate();
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
		
		// Acceleration
		this.xAccel   = data.readFloat();
		this.zAccel   = data.readFloat();
		
		// Rotation
		this.pitch    = data.readFloat();
		this.yaw      = data.readFloat();
		
		this.isFlying = data.readBoolean();
		this.isSneaking = data.readBoolean();
		this.isSprinting = data.readBoolean();
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
		
		// Acceleration
		data.writeFloat(xAccel);
		data.writeFloat(zAccel);
		
		// Rotation
		data.writeFloat(pitch);
		data.writeFloat(yaw);
		
		data.writeBoolean(isFlying);
		data.writeBoolean(isSneaking);
		data.writeBoolean(isSprinting);
	}
	
	@Override
	public int getPacketID()
	{
		return 1;
	}
}
