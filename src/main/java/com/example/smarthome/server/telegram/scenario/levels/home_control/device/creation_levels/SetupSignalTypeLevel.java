package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.MessageProcessor;

import java.util.ArrayList;
import java.util.List;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;

public class SetupSignalTypeLevel implements MessageProcessor {

    private static final SetupSignalTypeLevel instance = new SetupSignalTypeLevel();

    // ************************************* MESSAGES *************************************************
    private static final String chooseSignalType = "Выберите тип сигнала, который может принимать устройство";

    // ************************************* BUTTONS **************************************************
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};

    private SetupSignalTypeLevel() {
    }

    public static SetupSignalTypeLevel getInstance() {
        return instance;
    }

    @Override
    public Object process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            switch (msg.getText()) {
                case "pwm":
                case "digital":
                    return msg.getText();
            }
        }
        return null;
    }

    public static void goToSetupSignalTypeLevel(UserInstance user, IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), chooseSignalType, typesOfSignal)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true));
    }
}
