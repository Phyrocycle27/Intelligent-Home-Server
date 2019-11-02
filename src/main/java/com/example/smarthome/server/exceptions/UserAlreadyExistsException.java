package com.example.smarthome.server.exceptions;

public class UserAlreadyExistsException extends Exception {

    public UserAlreadyExistsException(long userId) {
        super(String.format("The user with id %d was already give token", userId));
    }
}
