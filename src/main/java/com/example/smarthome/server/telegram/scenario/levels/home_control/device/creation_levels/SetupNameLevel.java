package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;

public class SetupNameLevel implements MessageProcessor {

    private static final SetupNameLevel instance = new SetupNameLevel();

    // ************************************* MESSAGES *************************************************
    private static final String enterName = "Пожалуйста, введите имя нового устройства";

    private SetupNameLevel() {
    }

    @Override
    public boolean process(UserInstance user, IncomingMessage msg) {
        user.getDeviceCreator().getCreationOutput().setName(msg.getText());
        return true;
    }

    public static void goToSetupNameLevel(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), enterName, null).setMessageId(msg.getId())
                .hasBackButton(true));
        user.setLastMessageId(msg.getId());
        user.getDeviceCreator().setCurrCreationLvl(instance);
    }
}
