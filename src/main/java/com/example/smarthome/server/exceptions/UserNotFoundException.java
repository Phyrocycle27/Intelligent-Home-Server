package com.example.smarthome.server.exceptions;

public class UserNotFoundException extends Exception {

    public UserNotFoundException(long userId) {
        super(String.format("The user with id %d haven't his token", userId));
    }
}
