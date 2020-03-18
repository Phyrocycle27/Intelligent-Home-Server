package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.CheckOfStartCommand;
import com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceCreator;
import com.example.smarthome.server.telegram.scenario.levels.home_control.device.creation_levels.DeviceEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInstance {

    private static final Logger log = LoggerFactory.getLogger(UserInstance.class);

    private DeviceCreator deviceCreator;
    private DeviceEditor deviceEditor;
    private AnswerCreator currentLvl;
    private long chatId;

    private int lastMessageId;
    private boolean processing;
    private int spamCount;

    UserInstance(long chatId) {
        this.chatId = chatId;
    }

    public DeviceCreator getDeviceCreator() {
        return deviceCreator;
    }

    public void setDeviceCreator(DeviceCreator creator) {
        deviceCreator = creator;
    }

    public DeviceEditor getDeviceEditor() {
        return deviceEditor;
    }

    public void setDeviceEditor(DeviceEditor deviceEditor) {
        this.deviceEditor = deviceEditor;
    }

    public long getChatId() {
        return chatId;
    }

    public AnswerCreator getCurrentLvl() {
        return this.currentLvl;
    }

    public void setCurrentLvl(AnswerCreator currentLvl) {
        this.currentLvl = currentLvl;
        processing = false;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }

    public synchronized int getSpamCount() {
        return spamCount;
    }

    public synchronized void spam() {
        spamCount++;
    }

    public synchronized void clearSpamCount() {
        spamCount = 0;
    }

    synchronized void sendAnswer(IncomingMessage msg) {
        processing = true;
        if (!CheckOfStartCommand.getInstance().check(this, msg)) {
            if (currentLvl != null) {
                currentLvl.create(this, msg);
            }
        }
    }
}