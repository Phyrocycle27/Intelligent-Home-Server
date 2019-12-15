package com.example.smarthome.server.connection;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.*;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonRequester {
    private static final DeviceAccessService service;
    private static final Logger LOGGER;

    static {
        service = DeviceAccessService.getInstance();
        LOGGER = Logger.getLogger(JsonRequester.class.getName());
    }

    public static JSONObject execute(JSONObject request, Channel ch) throws ChannelNotFoundException {
        JSONObject obj = new JSONObject();

        ChannelFuture f = ch.writeAndFlush(request.toString()).addListener((ChannelFutureListener) channelFuture -> {

            if (ch.pipeline().names().contains("msgTmpReader"))
                ch.pipeline().remove("msgTmpReader");

            ch.pipeline().addBefore("sessionHandler", "msgTmpReader", new ChannelInboundHandlerAdapter() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    JSONObject tmp = new JSONObject(msg.toString());

                    LOGGER.log(Level.INFO, "Incoming data: " + tmp.toString());

                    if (tmp.getString("type").equals("data"))
                        obj.put("body", tmp.getJSONObject("body"));
                }

                @Override
                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                    synchronized (obj) {
                        obj.notify();
                    }

                    ch.pipeline().remove(this);
                }
            });
        });

        try {
            synchronized (obj) {
                obj.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
