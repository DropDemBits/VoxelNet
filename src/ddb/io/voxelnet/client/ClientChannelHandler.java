package ddb.io.voxelnet.client;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.network.packet.Packet;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class ClientChannelHandler extends ChannelDuplexHandler
{
	Game clientInstance;
	
	public ClientChannelHandler(Game instance)
	{
		clientInstance = instance;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		Packet packet = (Packet)msg;
		clientInstance.getNetworkManager().handlePacket(packet);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
		ctx.close();
	}
}
