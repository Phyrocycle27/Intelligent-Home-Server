package com.example.smarthome.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class SessionHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SessionHandler.class);
    private static Map<String, Channel> tokenToChannel;
    private static Map<Channel, String> channelToToken;

    static {
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        if (msg.toString().equals("ping")) {
            ctx.writeAndFlush("pong");
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        log.info("Unregistered");
        removeChannel(ctx.channel());
        ctx.close().sync();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        log.info("Session handler has added");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.info("Exception " + cause.getMessage());
        cause.printStackTrace();
    }
}