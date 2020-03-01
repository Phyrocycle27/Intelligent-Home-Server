package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;

public class SetupNameLevel implements MessageProcessor {

    private static final SetupNameLevel instance = new SetupNameLevel();
    private static final Logger log = LoggerFactory.getLogger(SetupNameLevel.class);

    // ************************************* MESSAGES *************************************************
    private static final String enterName = "Пожалуйста, введите имя нового устройства";

    private SetupNameLevel() {
    }

    public static SetupNameLevel getInstance() {
        return instance;
    }

    @Override
    public Object process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.TEXT) {
            return msg.getText();
        }
        return null;
    }

    public static void goToSetupNameLevel(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), enterName, null).setMessageId(msg.getId())
                .hasBackButton(true));
        user.setLastMessageId(msg.getId());
    }
}
