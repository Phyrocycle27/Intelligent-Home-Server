package com.example.smarthome.server.exceptions;

public class ChannelNotFoundException extends Exception {

    public ChannelNotFoundException(long userId) {
        super(String.format("The user's (id %d) token haven't been bound to its channel yet", userId));
    }
}
