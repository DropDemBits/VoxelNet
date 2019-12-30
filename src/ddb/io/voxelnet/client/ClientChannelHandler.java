package ddb.io.voxelnet.client;

import ddb.io.voxelnet.Game;
import ddb.io.voxelnet.network.Packet;
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
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		Packet packet = (Packet)msg;
		clientInstance.packetQueue.offer(packet);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
}
