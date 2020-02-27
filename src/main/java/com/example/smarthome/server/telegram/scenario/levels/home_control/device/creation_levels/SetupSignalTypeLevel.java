package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

public class SetupSignalTypeLevel implements AnswerCreator {

    private static final SetupSignalTypeLevel instance = new SetupSignalTypeLevel();


    public static SetupSignalTypeLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {

    }

    public static void goToSetupSignalTypeLevel(UserInstance user, IncomingMessage msg) {

    }
}
