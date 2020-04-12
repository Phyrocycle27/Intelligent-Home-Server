package com.example.smarthome.server.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthEventHandler extends ChannelDuplexHandler {

    private static Logger log = LoggerFactory.getLogger(AuthEventHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Channel ch = ctx.channel();
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                log.info(String.format("Close channel with %s; Reason: idle state for %d seconds",
                        ch.remoteAddress(), ((IdleStateHandler) ch.pipeline().get("idleHandler"))
                                .getAllIdleTimeInMillis() / 1000));
                ctx.close().sync();
            }
        }
    }
}
