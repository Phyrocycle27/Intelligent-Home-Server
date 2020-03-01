package com.example.smarthome.server.connection;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRequester {

    private static final DeviceAccessService service = DeviceAccessService.getInstance();
    private static final Logger log = LoggerFactory.getLogger(JsonRequester.class);

    public static JSONObject execute(JSONObject request, Channel ch) throws ChannelNotFoundException {
        JSONObject obj = new JSONObject();

        log.info("Request: " + request.toString());

        ChannelFuture f = ch.writeAndFlush(request.toString()).addListener((ChannelFutureListener) channelFuture -> {

            if (ch.pipeline().names().contains("msgTmpReader"))
                ch.pipeline().remove("msgTmpReader");

            ch.pipeline().addBefore("sessionHandler", "msgTmpReader", new ChannelInboundHandlerAdapter() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    JSONObject tmp = new JSONObject(msg.toString());

                    log.info("Incoming data: " + tmp.toString());

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
        return obj.getJSONObject("body");
    }
}
