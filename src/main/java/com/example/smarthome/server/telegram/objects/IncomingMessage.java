package com.example.smarthome.server.telegram.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IncomingMessage {
    private Integer id;
    private String text;
    private String callbackId;
    private MessageType type;
}
