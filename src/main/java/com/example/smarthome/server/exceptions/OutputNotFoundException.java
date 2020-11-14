package com.example.smarthome.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class OutputNotFoundException extends Exception {

    public OutputNotFoundException(long id) {
        super(String.format("Not found output with id '%d'", id));
    }
}
