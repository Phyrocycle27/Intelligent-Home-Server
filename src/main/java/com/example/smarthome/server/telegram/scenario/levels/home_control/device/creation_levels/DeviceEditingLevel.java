package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceLevel.goToDeviceLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupNameLevel.goToSetupNameLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.SetupSignalInversionLevel.goToSetupSignalInversionLevel;

public class DeviceEditingLevel implements AnswerCreator {

    private static final DeviceEditingLevel instance = new DeviceEditingLevel();

    private static final Logger log = LoggerFactory.getLogger(DeviceEditingLevel.class);

    // ************************************* MESSAGES *************************************************
    private static final String chooseToEdit = "Выберите параметр, который хотите изменить";

    private DeviceEditingLevel() {
    }

    @Override
    public void create(UserInstance user, IncomingMessage msg) {
        if (user.getDeviceEditor().getCurrEditingLvl() == null) {
            if (msg.getType() == MessageType.CALLBACK) {
                switch (msg.getText()) {
                    case "name":
                        goToSetupNameLevel(user, msg);
                        user.getDeviceEditor().setCurrEditingLvl(SetupNameLevel.getInstance());
                        EmojiCallback.next(msg.getCallbackId());
                        break;
                    case "inversion_of_signal":
                        goToSetupSignalInversionLevel(user, msg);
                        user.getDeviceEditor().setCurrEditingLvl(SetupSignalInversionLevel.getInstance());
                        EmojiCallback.next(msg.getCallbackId());
                        break;
                    case "back":
                        goToDeviceLevel(user, msg, user.getDeviceEditor().getEditingOutput().getOutputId());
                        user.getDeviceEditor().destroy();
                        EmojiCallback.back(msg.getCallbackId());
                        break;
                }
            }
        } else {
            if (msg.getType() == MessageType.CALLBACK && msg.getText().equals("back")) {
                goToChoice(user, msg);
                user.getDeviceEditor().setCurrEditingLvl(null);
                EmojiCallback.back(msg.getCallbackId());
            } else {
                user.getDeviceEditor().process(msg);
            }
        }
    }

    public static void goToDeviceEditingLevel(UserInstance user, IncomingMessage msg, int deviceId) {
        try {
            user.setDeviceEditor(new DeviceEditor(user, deviceId));
            user.setCurrentLvl(instance);
            goToChoice(user, msg);
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
        }
    }

    public static void goToChoice(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), chooseToEdit, new ArrayList<CallbackButton>() {{
            add(new CallbackButton("Имя", "name"));
            add(new CallbackButton("Инверсия сигнала", "inversion_of_signal"));
        }}).setMessageId(msg.getId())
                .hasBackButton(true)
                .setNumOfColumns(2));
    }
}
