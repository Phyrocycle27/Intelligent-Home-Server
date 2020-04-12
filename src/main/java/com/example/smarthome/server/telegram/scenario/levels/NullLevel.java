package com.example.smarthome.server.telegram.scenario.levels;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.MessageType;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.example.smarthome.server.telegram.MessageExecutor.executeAsync;
import static com.example.smarthome.server.telegram.scenario.levels.MenuLevel.goToMenuLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NullLevel {

    @Getter
    private static final NullLevel instance = new NullLevel();

    // ************************************* MESSAGES ************************************************
    private static final String buttonInvalid = "Кнопка недействительна";

    public boolean check(UserInstance userInstance, IncomingMessage msg) {
        if (msg.getText() != null) {
            if (msg.getType() == MessageType.TEXT && msg.getText().equals("/start")) {
                goToMenuLevel(userInstance, msg, null);
                return true;
            } else if (userInstance.getCurrentLvl() == null && msg.getType() == MessageType.CALLBACK) {
                executeAsync(new AnswerCallback(msg.getCallbackId(), buttonInvalid),
                        () -> userInstance.setProcessing(false));
                return true;
            }
        } return false;
    }
}
