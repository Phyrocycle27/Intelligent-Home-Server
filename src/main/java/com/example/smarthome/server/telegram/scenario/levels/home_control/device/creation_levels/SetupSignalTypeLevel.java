package com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels;

import com.example.smarthome.server.telegram.MessageExecutor;
import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
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
public class SetupSignalTypeLevel implements MessageProcessor {

    @Getter
    private static final SetupSignalTypeLevel instance = new SetupSignalTypeLevel();

    // ************************************* MESSAGES *************************************************
    private static final String chooseSignalType = "Выберите тип сигнала, который может принимать устройство";
    private static final String signalTypeNotSupport = "Поддержка этого типа сигнала ещё не добавлена";

    // ************************************* BUTTONS **************************************************
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};

    @Override
    public Object process(UserInstance user, IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            switch (msg.getText()) {
                case "pwm":
                    MessageExecutor.executeAsync(new AnswerCallback(msg.getCallbackId(), signalTypeNotSupport));
                    break;
                case "digital":
                    return msg.getText();
            }
        }
        return null;
    }

    public static void goToSetupSignalTypeLevel(UserInstance user, IncomingMessage msg) {
        executeAsync(new InlineKeyboardMessage(user.getChatId(), chooseSignalType, typesOfSignal)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true), null);
    }
}
