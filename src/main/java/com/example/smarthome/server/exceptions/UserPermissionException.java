package com.example.smarthome.server.exceptions;

public class UserPermissionException extends Exception {

    public UserPermissionException() {
        super("You don't have exception to do this action");
    }
}
