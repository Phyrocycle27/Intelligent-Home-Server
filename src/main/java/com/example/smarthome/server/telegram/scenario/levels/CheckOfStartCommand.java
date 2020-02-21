package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;

import static com.example.smarthome.server.telegram.scenario.levels.MenuLevel.goToMenuLevel;

public class CheckOfStartCommand {

    private static final CheckOfStartCommand instance = new CheckOfStartCommand();

    private CheckOfStartCommand() {
    }

    public static CheckOfStartCommand getInstance() {
        return instance;
    }

    public boolean check(UserInstance userInstance, IncomingMessage msg) {
        if (msg.getText().equals("/start")) {
            goToMenuLevel(userInstance, msg);
            return true;
        } else {
            return false;
        }
    }
}
