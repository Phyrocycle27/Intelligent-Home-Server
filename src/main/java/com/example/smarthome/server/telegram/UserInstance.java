package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.IncomingMessage;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import com.example.smarthome.server.telegram.scenario.levels.CheckOfStartCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UserInstance {
    // *********************************** HOME CONTROL ***************************************
    private static final String devicesNotFound = "Устройства не обнаружены. Вы можете добавить их прямо сейчас";
    private static final String removeConfirmationDevice = "Вы действительно хотите удалить это устройство?";
    private static final String deviceOff = "Устройство выключено";
    private static final String deviceOn = "Устройство включено";
    private static final String deviceDeleted = "Устройство удалено";
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "yes"));
        add(new CallbackButton("Нет", "no"));
    }};
    private static final String sensorsMsg = "Нажмите на существующий датчик или добавьте новый";
    private static final String devicesMsg = "Нажмите на существующее устройство или добавьте новое";
    private static final String buttonInvalid = "Кнопка недействительна";
    // ******************************** STATIC FINAL VARIABLES **********************************************
    private static final Logger log;
    private static Bot bot;

    /* ************************************ TEMP VARIABLES ************************************************** */
    // private DeviceCreator creator;

    private long chatId;
    private int lastMessageId;

    // Scenery

    public long getChatId() {
        return chatId;
    }

    public void setCurrentLvl(AnswerCreator currentLvl) {
        this.currentLvl_2 = currentLvl;
    }

    public AnswerCreator getCurrentLvl() {
        return this.currentLvl_2;
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
    private Consumer<IncomingMessage> currentLvl;
    private AnswerCreator currentLvl_2;
    /**
     * Уровень по умолчанию
     *
     * @apiNote Уровень, который обрабатывает команду /start. Позволяет перейти на уровень menu
     */
    private Predicate<IncomingMessage> defaultLvl;

    static {
        log = LoggerFactory.getLogger(Bot.class);
    }

    UserInstance(long chatId) {
        this.chatId = chatId;
    }

    public static void setBot(Bot bot) {
        UserInstance.bot = bot;
    }

    private void init() {

        /*devicesLvl = msg -> {
            log.info("Devices level");
            if (msg.getText().equals("add")) {
                creator = new DeviceCreator();
                creator.start(msg);
                currentLvl = deviceCreationLvl;
            } else if (msg.getText().equals("back")) {
                goToHomeControlLevel(msg);
            }
            // Если ответ содержит только число, то пытаемся получить устройство с таким id
            else if (msg.getText().matches("[-+]?\\d+")) {
                try {
                    goToDeviceLevel(msg, Integer.parseInt(msg.getText()));
                    currentLvl = deviceControlLvl;
                } catch (ChannelNotFoundException e) {
                    log.warn(e.getMessage());
                    goToHomeControlLevel(msg);
                }
            } else {
                if (!msg.getCallbackId().isEmpty())
                    MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
        };

        deviceControlLvl = msg -> {
            if (msg.getType() == MessageType.CALLBACK) {
                if (msg.getText().equals("back")) {
                    goToDevicesLevel(null, msg);
                    return;
                }

                String[] arr = msg.getText().split("[_]");
                String cmd = arr[0];
                int deviceId = Integer.parseInt(arr[1]);

                switch (cmd) {
                    case "off":
                        try {
                            setDigitalState(deviceId, false);
                            goToDeviceLevel(msg, deviceId);
                            MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), deviceOff));
                        } catch (ChannelNotFoundException e) {
                            log.warn(e.getMessage());
                            goToHomeControlLevel(msg);
                        }
                        break;
                    case "on":
                        try {
                            setDigitalState(deviceId, true);
                            goToDeviceLevel(msg, deviceId);
                            MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), deviceOn));
                        } catch (ChannelNotFoundException e) {
                            log.warn(e.getMessage());
                            goToHomeControlLevel(msg);
                        }
                        break;
                    case "remove":
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, removeConfirmationDevice,
                                new ArrayList<CallbackButton>() {{
                                    add(new CallbackButton("Подтвердить", "confirmRemove_device_" + deviceId));
                                    add(new CallbackButton("Отмена", "cancel_device_" + deviceId));
                                }})
                                .setMessageId(msg.getId()));
                        currentLvl = confirmRemove;
                        break;
                    default:
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
                }
            }
        };

        confirmRemove = msg -> {
            log.info("Remove confirmation level");
            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];
            String type = arr[1];

            switch (type) {
                case "device":
                    int deviceId = Integer.parseInt(arr[2]);
                    if (cmd.equals("confirmRemove")) {
                        try {
                            deleteOutput(deviceId);
                            goToDevicesLevel(deviceDeleted, msg);
                        } catch (ChannelNotFoundException e) {
                            log.warn(e.getMessage());
                            goToHomeControlLevel(msg);
                        }
                    } else if (cmd.equals("cancel")) {
                        try {
                            goToDeviceLevel(msg, deviceId);
                            currentLvl = deviceControlLvl;
                        } catch (ChannelNotFoundException e) {
                            log.warn(e.getMessage());
                            goToHomeControlLevel(msg);
                        }
                    }
                    break;
            }
        };

        deviceCreationLvl = msg -> {
            log.info("Device creation level");
            if (msg.getText().equals("back")) {
                creator.goToPrev(msg);
            } else creator.currCreationLvl.accept(msg);
        };*/
    }

    void sendAnswer(IncomingMessage msg) {
        if (!CheckOfStartCommand.getInstance().check(this, msg)) {
            if (currentLvl_2 != null) {
                currentLvl_2.create(this, msg);
            } else MessageExecutor.delete(bot, chatId, msg.getId());
        }
    }

    /*private void goToDevicesLevel(String text, IncomingMessage msg) {
        try {
            List<CallbackButton> devices = new ArrayList<>();

            for (Output output : getOutputs()) {
                devices.add(new CallbackButton(output.getName(), String.valueOf(output.getOutputId())));
            }
            String answerText;
            if (devices.isEmpty()) {
                answerText = devicesNotFound;
            } else answerText = devicesMsg;

            if (text != null) MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), text));

            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, answerText, devices)
                    .hasAddButton(true)
                    .hasBackButton(true)
                    .setMessageId(msg.getId())
                    .setNumOfColumns(2));
            currentLvl = devicesLvl;
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(msg);
        }
    }*/

    /*private void goToDeviceLevel(IncomingMessage msg, int deviceId) throws ChannelNotFoundException {
        Output output = getOutput(deviceId);

        if (output == null) goToDevicesLevel("Устройство c таким id не найдено", msg);
        else {
            boolean currState = getDigitalState(output.getOutputId());
            String currStateText = currState ? "включено" : "выключено";
            String inversion = output.getReverse() ? "включена" : "выключена";
            String signalType = "";

            switch (output.getType()) {
                case "digital":
                    signalType = "цифовой";
                    break;
                case "pwm":
                    signalType = "ШИМ";
            }

            List<CallbackButton> buttons = new ArrayList<CallbackButton>() {{
                if (currState) add(new CallbackButton("Выключить", "off_" + output.getOutputId()));
                else add(new CallbackButton("Включить", "on_" + output.getOutputId()));
                add(new CallbackButton("Удалить", "remove_" + output.getOutputId()));
            }};

            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, String.format("<b>%s</b>\n" +
                            "Текущее состояние: <i>%s</i>\n" +
                            "Тип сигнала: <i>%s</i>\n" +
                            "Инверсия: <i>%s</i>\n" +
                            "GPIO-пин: <i>%d</i>",
                    output.getName(), currStateText, signalType, inversion,
                    output.getGpio()), buttons)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
        }
    }*/

    // ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI

    /* **************************************************************************************************************
     ************************* ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI *******************************************
     ************************************************************************************************************** */

    /*private boolean getDigitalState(@NonNull Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/control/digital?id=" + outputId));

        return JsonRequester.execute(request, getChannel()).getJSONObject("body").getJSONObject("entity")
                .getBoolean("digitalState");
    }

    private void setDigitalState(@NonNull Integer outputId, boolean state) throws ChannelNotFoundException {
        log.info("Id: " + outputId + " signal: " + state);
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "PUT")
                        .put("uri", "http://localhost:8080/outputs/control/digital")
                        .put("request_body", new JSONObject()
                                .put("outputId", outputId)
                                .put("digitalState", state)));

        JsonRequester.execute(request, getChannel());
    }

    // DELETE
    private void deleteOutput(Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "DELETE")
                        .put("uri", "http://localhost:8080/outputs/one/" + outputId));

        JsonRequester.execute(request, getChannel());
    }

    // CREATE
    private void createOutput(Output newOutput) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "POST")
                        .put("uri", "http://localhost:8080/outputs/create")
                        .put("request_body", new JSONObject(newOutput)));

        JsonRequester.execute(request, getChannel());
    }

    // GET
    private Output getOutput(Integer outputId) throws ChannelNotFoundException {
        Output output = new Output();

        // ***********************************************************
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/one/" + outputId));
        // ***********************************************************

        JSONObject response = JsonRequester.execute(request, getChannel())
                .getJSONObject("body");

        if (response.getInt("code") == 200) {
            response = response.getJSONObject("entity");
            output.setOutputId(response.getInt("outputId"));
            output.setName(response.getString("name"));
            output.setGpio(response.getInt("gpio"));
            output.setReverse(response.getBoolean("reverse"));
            output.setType(response.getString("type"));
            output.setCreationDate(LocalDateTime.parse(
                    response.getString("creationDate"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return output;
        } else {
            return null;
        }
    }

    // GET LIST
    private List<Output> getOutputs() throws ChannelNotFoundException {
        List<Output> outputs = new ArrayList<>();

        // ***********************************************************
        JSONObject reqest = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/all"));
        // ***********************************************************
        JSONArray response = JsonRequester.execute(reqest, getChannel())
                .getJSONObject("body").getJSONArray("entity");

        if (response.length() != 0) {
            // Создаём из объектов массива JSON объекты Output
            // и вносим их в List outputs
            for (int i = 0; i < response.length(); i++) {
                JSONObject outputJson = response.getJSONObject(i);
                Output output = new Output();

                output.setName(outputJson.getString("name"));
                output.setOutputId(outputJson.getInt("outputId"));

                outputs.add(output);
            }
        }
        return outputs;
    }

    // GET LIST CONTAINS NUMBERS OF FREE GPIOS
    private List<String> getAvailableOutputs(String type) throws ChannelNotFoundException {
        List<String> gpios = new ArrayList<>();

        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/available?type=" + type));

        JSONArray array = JsonRequester.execute(request, getChannel())
                .getJSONObject("body")
                .getJSONObject("entity")
                .getJSONArray("available_gpios");
        for (int i = 0; i < array.length(); i++) {
            gpios.add(String.valueOf(array.getInt(i)));
        }

        return gpios;
    }*/

    /*private Channel getChannel() throws ChannelNotFoundException {
        return service.getChannel(chatId);
    }*/

    // **************************************************************************************************************

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