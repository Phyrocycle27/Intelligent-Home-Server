package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

public class DeviceCreationLevel implements AnswerCreator {

    private static final DeviceCreationLevel instance = new DeviceCreationLevel();

    private DeviceCreationLevel() {
    }

    public static DeviceCreationLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (msg.getText().equals("back")) {
            user.getDeviceCreator().goToPrev(msg);
        } else user.getDeviceCreator().getCurrCreationLvl().accept(msg);
    }

    public static void goToDeviceCreationLevel(UserInstance user, IncomingMessage msg) {
        user.setDeviceCreator(new DeviceCreator(user));
        user.getDeviceCreator().start(msg);
        user.setCurrentLvl(instance);
    }
}
