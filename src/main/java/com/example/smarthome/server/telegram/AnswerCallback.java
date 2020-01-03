package com.example.smarthome.server.telegram;

import lombok.Getter;

@Getter
public class AnswerCallback {
    private String text;
    private String callbackId;
    private boolean alert;

    public AnswerCallback(String callbackId) {
        this.callbackId = callbackId;
    }

    public AnswerCallback(String callbackId, String text) {
        this.text = text;
        this.callbackId = callbackId;
    }

    public AnswerCallback setAlert(boolean b) {
        this.alert = b;
        return this;
    }
}
