package com.example.smarthome.server.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class OutputAlreadyExistException extends Exception {

    public OutputAlreadyExistException(int gpio) {
        super(String.format("The output with gpio '%d' have already created", gpio));
    }
}
