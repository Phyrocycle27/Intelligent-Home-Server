package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.CheckOfStartCommand;
import com.example.smarthome.server.telegram.scenario.levels.home_control.device.DeviceCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInstance {

    private static final Logger log = LoggerFactory.getLogger(UserInstance.class);

    private DeviceCreator deviceCreator;
    private long chatId;
    private int lastMessageId;

    private AnswerCreator currentLvl;

    UserInstance(long chatId) {
        this.chatId = chatId;
    }

    public DeviceCreator getDeviceCreator() {
        return deviceCreator;
    }

    public void setDeviceCreator(DeviceCreator creator) {
        deviceCreator = creator;
    }

    public long getChatId() {
        return chatId;
    }

    public AnswerCreator getCurrentLvl() {
        return this.currentLvl;
    }

    public void setCurrentLvl(AnswerCreator currentLvl) {
        this.currentLvl = currentLvl;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    void sendAnswer(IncomingMessage msg) {
        if (!CheckOfStartCommand.getInstance().check(this, msg)) {
            if (currentLvl != null) {
                currentLvl.create(this, msg);
            }
        }
    }
}