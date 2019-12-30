package ddb.io.voxelnet.server;

import ddb.io.voxelnet.network.PCSPosRotUpdate;
import ddb.io.voxelnet.network.PSEstablishConnection;
import ddb.io.voxelnet.network.Packet;
import io.netty.channel.*;
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
	public void flush(ChannelHandlerContext ctx) throws Exception
	{
		System.out.println("Broadcast");
		super.flush(ctx);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
	{
		cause.printStackTrace();
		ctx.close();
	}
}
