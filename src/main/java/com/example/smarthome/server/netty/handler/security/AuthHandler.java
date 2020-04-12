package com.example.smarthome.server.netty.handler.security;

import com.example.smarthome.server.netty.handler.SessionHandler;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    private static final DeviceAccessService service;
    private final Encryption enc;

    static {
        service = DeviceAccessService.getInstance();
    }

    public AuthHandler() {
        enc = new Encryption();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        log.info("Channel is active");

        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(future ->
                log.info("SSL handshake is success!"));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (!enc.isKeySet()) {
            // тут нам клиент прислал свой публичный ключ
            byte[] pKeyEnc = enc.getPublicKey((byte[]) msg);
            ch.writeAndFlush(pKeyEnc);

            ch.pipeline().addAfter("frameEncoder", "cipherDecoder", new CipherDecoder(enc));
            ch.pipeline().addAfter("cipherDecoder", "cipherEncoder", new CipherEncoder(enc));
            ch.pipeline().remove("bytesDecoder");
            ch.pipeline().remove("bytesEncoder");

            log.info("Encryption AES key is: " + enc.isKeySet());
        } else {
            // а тут он высылает токен, который был получен пользователем в телеграме
            log.info(String.format("Token from channel %s is %s", ch.remoteAddress(), msg));

            if (service.isExists(msg.toString())) {
                log.info("Token is right");

                ch.pipeline().remove("idleHandler");
                ch.pipeline().remove("eventHandler");
                ch.pipeline().remove(this);
                ch.pipeline().addLast("sessionHandler", new SessionHandler(msg.toString(), ch));
            } else {
                log.info("Token is wrong");
                ctx.close();
            }
        }
    }
}
