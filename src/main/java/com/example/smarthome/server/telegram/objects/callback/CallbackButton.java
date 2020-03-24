package com.example.smarthome.server.telegram.objects.callback;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode(of = {"text"})
public class CallbackButton {

    private String text;
    private String callbackText;
}
