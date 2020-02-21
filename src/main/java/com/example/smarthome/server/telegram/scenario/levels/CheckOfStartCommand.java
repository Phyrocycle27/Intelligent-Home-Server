package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;

public class CheckOfStartCommand {

    private static final CheckOfStartCommand instance = new CheckOfStartCommand();

    private CheckOfStartCommand() {
    }

    public static CheckOfStartCommand getInstance() {
        return instance;
    }

    public boolean check(UserInstance userInstance, IncomingMessage msg) {
        if (msg.getText().equals("/start")) {
            MenuLevel.goToMenuLevel(userInstance, msg);
            return true;
        } else {
            return false;
        }
    }
}
