package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.CallbackAction;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceCreationLevel implements AnswerCreator {

    @Getter
    private static final DeviceCreationLevel instance = new DeviceCreationLevel();
    private static final Logger log = LoggerFactory.getLogger(DeviceCreationLevel.class);

    @Override
    public boolean create(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK && msg.getText().equals("back")) {
            user.getDeviceCreator().goToPrev(msg, () -> EmojiCallback.back(msg.getCallbackId()));
            return true;
        } else {
            return user.getDeviceCreator().process(msg);
        }
    }

    public static void goToDeviceCreationLevel(UserInstance user, IncomingMessage msg, CallbackAction action) {
        user.setDeviceCreator(new DeviceCreator(user));
        user.getDeviceCreator().start(msg, () -> EmojiCallback.next(msg.getCallbackId()));
    }
}
