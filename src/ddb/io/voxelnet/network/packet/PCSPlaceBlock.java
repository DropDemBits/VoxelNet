package ddb.io.voxelnet.network.packet;

import ddb.io.voxelnet.block.Block;
import ddb.io.voxelnet.util.Facing;
import ddb.io.voxelnet.util.RaycastResult;
import io.netty.buffer.ByteBuf;

public class PCSPlaceBlock implements Packet
{
	public int clientID;
	public RaycastResult hitResult;
	public Block placingBlock;
	
	PCSPlaceBlock() {}
	
	public PCSPlaceBlock(int clientID, RaycastResult hitResult, Block placeBlock)
	{
		this.clientID = clientID;
		this.hitResult = hitResult;
		this.placingBlock = placeBlock;
	}
	
	@SuppressWarnings("unused")
	@Override
	public Packet makeEmptyPacket()
	{
		return new PCSPlaceBlock();
	}
	
	@Override
	public void decodePayload(ByteBuf data) throws Exception
	{
		hitResult = new RaycastResult();
		
		clientID = data.readInt();
		
		hitResult.hitX = data.readDouble();
		hitResult.hitY = data.readDouble();
		hitResult.hitZ = data.readDouble();
		
		hitResult.blockX = data.readInt();
		hitResult.blockY = data.readInt();
		hitResult.blockZ = data.readInt();
		
		hitResult.face = Facing.directions()[data.readUnsignedByte()];
		
		placingBlock = Block.idToBlock(data.readUnsignedShort());
	}
	
	@Override
	public void encodePayload(ByteBuf data) throws Exception
	{
		data.writeInt(clientID);
		
		// Sub-block position
		data.writeDouble(hitResult.hitX);
		data.writeDouble(hitResult.hitY);
		data.writeDouble(hitResult.hitZ);
		
		// Block position
		data.writeInt(hitResult.blockX);
		data.writeInt(hitResult.blockY);
		data.writeInt(hitResult.blockZ);
		
		// Hit face
		data.writeByte(hitResult.face.ordinal());
		
		// Block ID (as short)
		data.writeShort(placingBlock.getId());
	}
	
	@Override
	public int getPacketID()
	{
		return 5;
	}
}
