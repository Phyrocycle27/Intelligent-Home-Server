package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.NullLevel;
import com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceCreator;
import com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceEditor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
public class UserInstance {

    @FieldNameConstants.Exclude
    private static final Logger log = LoggerFactory.getLogger(UserInstance.class);

    @Setter
    private DeviceCreator deviceCreator;
    @Setter
    private DeviceEditor deviceEditor;
    @Setter
    private AnswerCreator currentLvl;
    private final long chatId;

    @Setter
    private int lastMessageId;
    @Setter
    private boolean processing;
    private int spamCount;

    UserInstance(long chatId) {
        this.chatId = chatId;
    }

    public synchronized void spam() {
        spamCount++;
    }

    public synchronized void clearSpamCount() {
        spamCount = 0;
    }

    synchronized void sendAnswer(IncomingMessage msg) {
        if (!NullLevel.getInstance().check(this, msg)) {
            if (currentLvl != null) {
                log.info("Creating answer");
                processing = currentLvl.create(this, msg);
            }
        }
    }
}