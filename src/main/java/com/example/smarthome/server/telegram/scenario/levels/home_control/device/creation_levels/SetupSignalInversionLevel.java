package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.EmojiCallback;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;

public class SetupSignalInversionLevel implements MessageProcessor {

    private static final SetupSignalInversionLevel instance = new SetupSignalInversionLevel();

    // ************************************* MESSAGES *************************************************
    private static final String inversionMsg = "Сделать инверсию сигнала для данного устройства?";

    // ************************************* BUTTONS **************************************************
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "yes"));
        add(new CallbackButton("Нет", "no"));
    }};

    private SetupSignalInversionLevel() {
    }

    @Override
    public boolean process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            user.getDeviceCreator().getCreationOutput().setReverse(msg.getText().equals("yes"));
            return true;
        }
        return false;
    }

    public static void goToSetupSignalInversionLevel(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), inversionMsg, yesOrNo)
                .setNumOfColumns(2)
                .setMessageId(msg.getId())
                .hasBackButton(true));
        EmojiCallback.next(msg.getCallbackId());
        user.getDeviceCreator().setCurrCreationLvl(instance);
    }
}
