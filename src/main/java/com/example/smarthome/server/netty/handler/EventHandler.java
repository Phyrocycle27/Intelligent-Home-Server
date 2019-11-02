package com.example.smarthome.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EventHandler extends ChannelDuplexHandler {

    private static Logger LOGGER = Logger.getLogger(EventHandler.class.getName());

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Channel ch = ctx.channel();
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                LOGGER.log(Level.INFO, String.format("Close channel with %s; Reason: idle state for %d seconds",
                        ch.remoteAddress(), ((IdleStateHandler) ch.pipeline().get("idleHandler"))
                                .getAllIdleTimeInMillis() / 1000));
                ctx.close();
            }
        }
    }
}
