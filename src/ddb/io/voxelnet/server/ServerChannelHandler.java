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
	public void channelActive(ChannelHandlerContext ctx)
	{
		Channel channel = ctx.channel();
		clientID = serverInstance.getNetworkManager().addClient(channel);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		serverInstance.getNetworkManager().removeClient(ctx.channel(), clientID);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		try
		{
			serverInstance.getNetworkManager().handlePacket((Packet)msg, clientID);
		}
		finally
		{
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		cause.printStackTrace();
		ctx.close();
	}
}
