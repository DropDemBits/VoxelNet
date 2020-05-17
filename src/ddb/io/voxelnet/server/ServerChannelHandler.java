package ddb.io.voxelnet.server;

import ddb.io.voxelnet.network.packet.Packet;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;

public class ServerChannelHandler extends ChannelDuplexHandler
{
	
	int clientID;
	ServerGame serverInstance;
	
	public ServerChannelHandler(ServerGame instance)
	{
		this.serverInstance = instance;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception
	{
		Channel channel = ctx.channel();
		clientID = serverInstance.addClient(channel);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		serverInstance.removeClient(ctx.channel(), clientID);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		try
		{
			serverInstance.handlePacket((Packet)msg);
		}
		finally
		{
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
}
