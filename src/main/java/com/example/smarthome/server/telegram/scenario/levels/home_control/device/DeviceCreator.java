package com.example.smarthome.server.telegram.scenario.levels.home_control.device;

import com.example.smarthome.server.entity.Output;
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
import java.util.List;
import java.util.function.Consumer;

import static com.example.smarthome.server.connection.ClientAPI.*;
import static com.example.smarthome.server.telegram.MessageExecutor.delete;
import static com.example.smarthome.server.telegram.MessageExecutor.execute;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.HomeControlLevel.goToHomeControlLevel;
import static com.example.smarthome.server.telegram.scenario.levels.home_control.device.DevicesLevel.goToDevicesLevel;

public class DeviceCreator {

    private static final Logger log = LoggerFactory.getLogger(DeviceCreator.class);

    // ************************************* BUTTONS *************************************************
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "yes"));
        add(new CallbackButton("Нет", "no"));
    }};

    public void setCurrCreationLvl(AnswerCreator currCreationLvl) {
        this.currCreationLvl_2 = currCreationLvl;
    }

    private AnswerCreator currCreationLvl_2;

    private Consumer<IncomingMessage> currCreationLvl;
    private Consumer<IncomingMessage> stepOne;
    private Consumer<IncomingMessage> stepTwo;
    private Consumer<IncomingMessage> stepThree;
    private Consumer<IncomingMessage> stepFour;
    private UserInstance user;

    public Output getCreationOutput() {
        return creationOutput;
    }

    private Output creationOutput;

    DeviceCreator(UserInstance user) {
        creationOutput = new Output();
        this.user = user;
        init();
    }

    public Consumer<IncomingMessage> getCurrCreationLvl() {
        return currCreationLvl;
    }

    private void init() {
        stepOne = msg -> {
            setDeviceName(msg);
            delete(user.getChatId(), user.getLastMessageId());
        };

        stepTwo = this::setDeviceSignalType;

        stepThree = this::setDeviceGpio;

        stepFour = this::setDeviceReverse;
    }

    void goToPrev(IncomingMessage msg) {
        if (currCreationLvl == stepOne) {
            goToDevicesLevel(user, msg);
        } else if (currCreationLvl == stepTwo) {
            start(msg);
        } else if (currCreationLvl == stepThree) {
            goToStepTwo(msg);
        } else if (currCreationLvl == stepFour) {
            goToStepThree(msg);
        }
    }

    void start(IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), "Пожалуйста, введите имя нового устройства", null)
                .setMessageId(msg.getId())
                .hasBackButton(true));
        user.setLastMessageId(msg.getId());
        currCreationLvl = stepOne;
    }

    // Step one - SET UP DEVICE NAME
    private void setDeviceName(IncomingMessage msg) {
        creationOutput.setName(msg.getText());
        goToStepTwo(msg);
    }

    private void goToStepTwo(IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(), "Выберите тип сигнала, который " +
                "может принимать устройство", typesOfSignal)
                .setMessageId(msg.getId())
                .setNumOfColumns(2)
                .hasBackButton(true));
        currCreationLvl = stepTwo;
    }

    // Step two - SET UP SIGNAL TYPE
    private void setDeviceSignalType(IncomingMessage msg) {
        switch (msg.getText()) {
            case "pwm":
            case "digital":
                creationOutput.setType(msg.getText());
                goToStepThree(msg);
                break;
        }
    }

    private void goToStepThree(IncomingMessage msg) {
        try {
            execute(new InlineKeyboardMessage(user.getChatId(),
                    "Теперь выберите пин, к которому вы хотите " +
                            "подключить новое устройство", new ArrayList<CallbackButton>() {{
                for (String s : getAvailableOutputs(getChannel(user.getChatId()), creationOutput.getType()))
                    add(new CallbackButton(s, s));
            }})
                    .setMessageId(msg.getId())
                    .setNumOfColumns(6)
                    .hasBackButton(true));
            EmojiCallback.next(msg.getCallbackId());

            currCreationLvl = stepThree;
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
            destroy();
        }
    }

    // Step three - SET UP GPIO
    private void setDeviceGpio(IncomingMessage msg) {
        try {
            if (getAvailableOutputs(getChannel(user.getChatId()), creationOutput.getType()).contains(msg.getText())) {
                creationOutput.setGpio(Integer.valueOf(msg.getText()));
                goToStepFour(msg);
            }
        } catch (NumberFormatException e) {
            log.error(e.getMessage());
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
            destroy();
        }
    }

    private void goToStepFour(IncomingMessage msg) {
        execute(new InlineKeyboardMessage(user.getChatId(),
                "Сделать инверсию сигнала для данного устройства?", yesOrNo)
                .setNumOfColumns(2)
                .setMessageId(msg.getId())
                .hasBackButton(true));
        EmojiCallback.next(msg.getCallbackId());
        currCreationLvl = stepFour;
    }

    // Step four - SET UP INVERSION
    private void setDeviceReverse(IncomingMessage msg) {
        if (msg.getType() == MessageType.CALLBACK) {
            creationOutput.setReverse(msg.getText().equals("yes"));
            createDevice(msg);
        }
    }

    private void createDevice(IncomingMessage msg) {
        try {
            createOutput(getChannel(user.getChatId()), creationOutput);
            goToDevicesLevel(user, msg);
            EmojiCallback.success(msg.getCallbackId());
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(user, msg);
        } finally {
            destroy();
        }
    }

    private void destroy() {
        creationOutput = null;
        user.setDeviceCreator(null);
    }
}
