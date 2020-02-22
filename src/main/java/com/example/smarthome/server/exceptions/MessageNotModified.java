package com.example.smarthome.server.exceptions;

public class MessageNotModified extends RuntimeException {

    public MessageNotModified() {
        super("Message is not modified");
    }
}
