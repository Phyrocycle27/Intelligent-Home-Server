package com.example.smarthome.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SessionHandler extends ChannelInboundHandlerAdapter {

    private static Logger LOGGER;
    private static Map<String, Channel> tokenToChannel;
    private static Map<Channel, String> channelToToken;

    static {
        LOGGER = Logger.getLogger(SessionHandler.class.getName());

        tokenToChannel = new HashMap<>();
        channelToToken = new HashMap<>();
    }

    public SessionHandler(String token, Channel ch) {
        tokenToChannel.put(token, ch);
        channelToToken.put(ch, token);
    }

    public static Channel getChannel(String token) {
        return tokenToChannel.get(token);
    }

    private static void removeChannel(Channel channel) {
        tokenToChannel.remove(channelToToken.get(channel));
        channelToToken.remove(channel);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        removeChannel(ctx.channel());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        LOGGER.log(Level.INFO, "Session handler has added");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}