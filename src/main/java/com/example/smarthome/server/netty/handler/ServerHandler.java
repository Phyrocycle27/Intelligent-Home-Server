package com.example.smarthome.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.ssl.SslHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerHandler extends SimpleChannelInboundHandler<String> {

    private static Logger LOGGER;
    private static Map<String, Channel> tokenToChannel;

    static {
        LOGGER = Logger.getLogger(ServerHandler.class.getName());
        tokenToChannel = new HashMap<>();
    }

    public ServerHandler(String token, Channel ch) {
        tokenToChannel.put(token, ch);
    }

    public static Channel getChannel(String token) {
        return tokenToChannel.get(token);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        LOGGER.log(Level.INFO, "Main Handler added");

        ch.writeAndFlush("Your protection is: " +
                        ctx.pipeline().get(SslHandler.class).engine().getSession().getCipherSuite() + "\r\n");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();

        LOGGER.log(Level.INFO, "Channel inactive");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
        Channel ch = ctx.channel();

        LOGGER.log(Level.INFO, "New message on channel");

        ch.writeAndFlush("[you] " + s + "\r\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}