package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.CheckOfStartCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserInstance {
    // *********************************** HOME CONTROL ***************************************
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "yes"));
        add(new CallbackButton("Нет", "no"));
    }};
    // ******************************** STATIC FINAL VARIABLES **********************************************
    private static final Logger log = LoggerFactory.getLogger(UserInstance.class);

    /* ************************************ TEMP VARIABLES ************************************************** */
    // private DeviceCreator creator;

    private long chatId;
    private int lastMessageId;

    // Scenery

    public long getChatId() {
        return chatId;
    }

    public void setCurrentLvl(AnswerCreator currentLvl) {
        this.currentLvl = currentLvl;
    }

    public AnswerCreator getCurrentLvl() {
        return this.currentLvl;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(int lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    /**
     * Текущее состояние чата пользователя
     */
    private AnswerCreator currentLvl;

    UserInstance(long chatId) {
        this.chatId = chatId;
    }

    private void init() {

        /*deviceCreationLvl = msg -> {
            log.info("Device creation level");
            if (msg.getText().equals("back")) {
                creator.goToPrev(msg);
            } else creator.currCreationLvl.accept(msg);
        };*/
    }

    void sendAnswer(IncomingMessage msg) {
        if (!CheckOfStartCommand.getInstance().check(this, msg)) {
            if (currentLvl != null) {
                currentLvl.create(this, msg);
            }
        }
    }

    /* **************************************************************************************************************
     ************************************** СОЗДАНИЕ УСТРОЙСТВА *****************************************************
     ************************************************************************************************************** */

    /*@Getter(value = AccessLevel.PRIVATE)
    private class DeviceCreator {

        private Consumer<IncomingMessage> currCreationLvl;
        private Consumer<IncomingMessage> stepOne;
        private Consumer<IncomingMessage> stepTwo;
        private Consumer<IncomingMessage> stepThree;
        private Consumer<IncomingMessage> stepFour;

        private Output creationOutput;

        private DeviceCreator() {
            creationOutput = new Output();
            init();
        }

        private void init() {
            stepOne = msg -> {
                setDeviceName(msg);
                MessageExecutor.delete(bot, chatId, lastMessageId);
            };

            stepTwo = this::setDeviceSignalType;

            stepThree = this::setDeviceGpio;

            stepFour = this::setDeviceReverse;
        }

        private void goToPrev(IncomingMessage msg) {
            if (currCreationLvl == stepOne) {
                goToDevicesLevel(null, msg);
            } else if (currCreationLvl == stepTwo) {
                start(msg);
            } else if (currCreationLvl == stepThree) {
                goToStepTwo(msg);
            } else if (currCreationLvl == stepFour) {
                goToStepThree(msg);
            }
        }

        private void start(IncomingMessage msg) {
            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId,
                    "Пожалуйста, введите имя нового устройства", null)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            lastMessageId = msg.getId();
            currCreationLvl = stepOne;
        }

        // Step one - SET UP DEVICE NAME
        private void setDeviceName(IncomingMessage msg) {
            creationOutput.setName(msg.getText());
            goToStepTwo(msg);
        }

        private void goToStepTwo(IncomingMessage msg) {
            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, "Выберите тип сигнала, который " +
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
                MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId,
                        "Теперь выберите пин, к которому вы хотите " +
                                "подключить новое устройство", new ArrayList<CallbackButton>() {{
                    for (String s : getAvailableOutputs(creationOutput.getType()))
                        add(new CallbackButton(s, s));
                }})
                        .setMessageId(msg.getId())
                        .setNumOfColumns(6)
                        .hasBackButton(true));
                currCreationLvl = stepThree;
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(msg);
                creationOutput = null;
            }
        }

        // Step three - SET UP GPIO
        private void setDeviceGpio(IncomingMessage msg) {
            try {
                if (getAvailableOutputs(creationOutput.getType()).contains(msg.getText())) {
                    creationOutput.setGpio(Integer.valueOf(msg.getText()));
                    goToStepFour(msg);
                }
            } catch (NumberFormatException e) {
                log.error(e.getMessage());
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(msg);
            }
        }

        private void goToStepFour(IncomingMessage msg) {
            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId,
                    "Сделать инверсию сигнала для данного устройства?", yesOrNo)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            currCreationLvl = stepFour;
        }

        // Step four - SET UP INVERSION
        private void setDeviceReverse(IncomingMessage msg) {
            switch (msg.getText()) {
                case "yes":
                    creationOutput.setReverse(true);
                    createDevice(msg);
                    break;
                case "no":
                    creationOutput.setReverse(false);
                    createDevice(msg);
                    break;
            }
        }

        private void createDevice(IncomingMessage msg) {
            try {
                createOutput(creationOutput);
                goToDevicesLevel("Устройство создано", msg);
            } catch (ChannelNotFoundException e) {
                log.warn(e.getMessage());
                goToHomeControlLevel(msg);
            } finally {
                creationOutput = null;
                creator = null;
            }
        }
    }*/
}