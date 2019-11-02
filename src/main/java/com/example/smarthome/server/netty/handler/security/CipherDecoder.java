package com.example.smarthome.server.netty.handler.security;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

public class CipherDecoder extends ByteToMessageDecoder {

    private final Encryption enc;

    CipherDecoder(Encryption enc) {
        this.enc = enc;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) throws Exception {
        byte[] dataWithParams = new byte[buf.readableBytes()];
        buf.readBytes(dataWithParams);

        byte[] params = Arrays.copyOf(dataWithParams, 18);
        byte[] data = Arrays.copyOfRange(dataWithParams, 18, dataWithParams.length);

        list.add(enc.decode(data, params));
    }
}
