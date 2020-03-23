package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SetupSignalInversionLevel implements MessageProcessor {

    @Getter
    private static final SetupSignalInversionLevel instance = new SetupSignalInversionLevel();

    // ************************************* MESSAGES *************************************************
    private static final String inversionMsg = "Сделать инверсию сигнала для данного устройства?";

    // ************************************* BUTTONS **************************************************
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "true"));
        add(new CallbackButton("Нет", "false"));
    }};

    @Override
    public Object process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            return Boolean.valueOf(msg.getText());
        }
        return null;
    }

    public static void goToSetupSignalInversionLevel(UserInstance user, IncomingMessage msg) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), inversionMsg, yesOrNo)
                .setNumOfColumns(2)
                .setMessageId(msg.getId())
                .hasBackButton(true), null);
    }
}
