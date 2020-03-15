package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
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
        if (msg.getType() == MessageType.CALLBACK && msg.getText().equals("back")) {
            user.getDeviceCreator().goToPrev(msg);
            EmojiCallback.back(msg.getCallbackId());
        } else user.getDeviceCreator().process(msg);
    }

    public static void goToDeviceCreationLevel(UserInstance user, IncomingMessage msg) {
        user.setDeviceCreator(new DeviceCreator(user));
        user.getDeviceCreator().start(msg);
        user.setCurrentLvl(instance);
    }
}
