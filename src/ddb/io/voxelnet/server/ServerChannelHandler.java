package ddb.io.voxelnet.server;

import ddb.io.voxelnet.network.PCSPosRotUpdate;
import ddb.io.voxelnet.network.PSEstablishConnection;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

public class ServerChannelHandler extends ChannelInboundHandlerAdapter
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
		
		// Send back client id
		PSEstablishConnection packet = new PSEstablishConnection(clientID);
		
		channel.write(packet);
		channel.flush();
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception
	{
		serverInstance.clientChannels.remove(ctx.channel());
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
	{
		try
		{
			// Position updates, feed to main server
			PCSPosRotUpdate packet = (PCSPosRotUpdate) msg;
			System.out.printf("\t Ply%d-Pos: (%f, %f, %f) - (%f, %f)\n", packet.clientID, packet.xPos, packet.yPos, packet.zPos, packet.pitch, packet.yaw);
		}
		finally
		{
			//todo: player pos data
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
