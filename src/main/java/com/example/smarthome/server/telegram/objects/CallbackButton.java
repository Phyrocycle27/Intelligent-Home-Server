package com.example.smarthome.server.telegram.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CallbackButton {
    private String text;
    private String callbackText;
}
