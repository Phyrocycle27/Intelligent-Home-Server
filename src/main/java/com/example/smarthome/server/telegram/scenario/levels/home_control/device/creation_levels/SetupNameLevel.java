package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalTypeLevel.goToSetupSignalTypeLevel;

public class SetupNameLevel implements AnswerCreator {

    private static final SetupNameLevel instance = new SetupNameLevel();

    // ************************************* MESSAGES *************************************************
    private static final String enterName = "Пожалуйста, введите имя нового устройства";

    public static SetupNameLevel getInstance() {
        return instance;
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        user.getDeviceCreator().getCreationOutput().setName(msg.getText());
        goToSetupSignalTypeLevel(user, msg);
    }

    public static void goToSetupNameLevel(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), enterName, null).setMessageId(msg.getId())
                .hasBackButton(true));
        user.setLastMessageId(msg.getId());
        user.getDeviceCreator().setCurrCreationLvl(instance);
    }
}
