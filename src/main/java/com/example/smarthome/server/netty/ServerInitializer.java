package com.example.smarthome.server.netty;

import com.example.smarthome.server.netty.handler.AuthEventHandler;
import com.example.smarthome.server.netty.handler.security.AuthHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ServerInitializer extends ChannelInitializer<SocketChannel> {

    public static final Logger log;

    static {
        log = LoggerFactory.getLogger(ServerInitializer.class);
    }

    private final SslContext sslCtx;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();

        log.debug("New client connected from " + ch.remoteAddress());

        p.addLast(sslCtx.newHandler(ch.alloc()));
        p.addLast("frameDecoder",
                new LengthFieldBasedFrameDecoder(
                        1048576, 0, 4, 0, 4));//16KB
        p.addLast("frameEncoder", new LengthFieldPrepender(4));
        p.addLast("bytesDecoder", new ByteArrayDecoder());
        p.addLast("bytesEncoder", new ByteArrayEncoder());
        p.addLast("idleHandler",
                new IdleStateHandler(0, 0, 60));
        p.addLast("eventHandler", new AuthEventHandler());
        p.addLast("authHandler", new AuthHandler());
    }
}
