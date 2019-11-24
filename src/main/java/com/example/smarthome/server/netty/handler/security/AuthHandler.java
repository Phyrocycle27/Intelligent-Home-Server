package com.example.smarthome.server.netty.handler.security;

import com.example.smarthome.server.netty.handler.SessionHandler;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER;
    private static final DeviceAccessService service;
    private final Encryption enc;

    static {
        service = DeviceAccessService.getInstance();
        LOGGER = Logger.getLogger(AuthHandler.class.getName());
    }

    public AuthHandler() {
        enc = new Encryption();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        LOGGER.log(Level.INFO, "Channel is active");

        ctx.pipeline().get(SslHandler.class).handshakeFuture().addListener(future ->
                LOGGER.log(Level.INFO, "SSL handshake is success!")
        );
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

            LOGGER.log(Level.INFO, "Encryption AES key is: " + enc.isKeySet());
        } else {
            // а тут он высылает токен, который был получен пользователем в телеграме
            LOGGER.log(Level.INFO, String.format("Token from channel %s is %s", ch.remoteAddress(), msg));

            if (service.isExists(msg.toString())) {
                LOGGER.log(Level.INFO, "Token is right");

                ch.pipeline().remove("idleHandler");
                ch.pipeline().remove("eventHandler");
                ch.pipeline().remove(this);
                ch.pipeline().addLast("sessionHandler", new SessionHandler(msg.toString(), ch));
            } else {
                LOGGER.log(Level.INFO, "Token is wrong");
                ctx.close();
            }
        }
    }
}
