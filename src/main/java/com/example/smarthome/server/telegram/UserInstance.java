package com.example.smarthome.server.telegram;

import com.example.smarthome.server.connection.JsonRequester;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

class UserInstance {
    // ************************************* MENU ***********************************************
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";
    private static final List<CallbackButton> menuButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Управление домом", "home_control"));
        add(new CallbackButton("Информация", "information"));
    }};
    // ************************************* INFO ************************************************
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final List<CallbackButton> infoButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Погода", "weather"));
        add(new CallbackButton("Время", "time"));
    }};
    // *********************************** HOME CONTROL ***************************************
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final String homeControl = "Выберите Устройства, чтобы управлять устройствами или добавить новое, или " +
            "выберите Датчики чтобы посмотреть показания или добавить новый датчик";
    private static final List<CallbackButton> typesOfSignal = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Цифровой", "digital"));
        add(new CallbackButton("ШИМ", "pwm"));
    }};
    private static final List<CallbackButton> yesOrNo = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Да", "yes"));
        add(new CallbackButton("Нет", "no"));
    }};
    private static final List<CallbackButton> homeControlButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Устройства", "devices"));
        add(new CallbackButton("Датчики", "sensors"));
    }};
    private static final String sensorsMsg = "Нажмите на существующий датчик или добавьте новый";
    private static final String devicesMsg = "Нажмите на существующее устройство или добавьте новое";
    // ****************************************** TOKEN ***********************************************************
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашей " +
            "Raspberry PI к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении." +
            "\n\n\u21E7\u21E7\u21E7 ВАШ ТОКЕН \u21E7\u21E7\u21E7";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final List<CallbackButton> tokenGenButton = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Сгенерировать токен", "token_gen"));
    }};

    // ******************************** STATIC FINAL VARIABLES **********************************************
    private static final Logger log;
    private static final Weather weatherService;
    private static final DeviceAccessService service;
    private static Bot bot;

    /* ************************************ TEMP VARIABLES ************************************************** */
    private DeviceCreator creator;
    private long chatId;
    private int lastMessageId;

    // Scenery
    private Consumer<IncomingMessage> currentLvl;
    private Predicate<IncomingMessage> defaultLvl;
    private Consumer<IncomingMessage> menuLvl;
    private Consumer<IncomingMessage> infoLvl;
    private Consumer<IncomingMessage> homeControlLvl;
    private Consumer<IncomingMessage> devicesLvl;
    private Consumer<IncomingMessage> deviceControlLvl;
    private Consumer<IncomingMessage> deviceCreationLvl;

    static {
        log = LoggerFactory.getLogger(Bot.class);
        weatherService = new Weather();
        service = DeviceAccessService.getInstance();
    }

    UserInstance(long chatId) {
        this.chatId = chatId;
        init();
    }

    public static void setBot(Bot bot) {
        UserInstance.bot = bot;
    }

    private void init() {
        defaultLvl = msg -> {
            log.info("Default level");
            if (msg.getText().toLowerCase().equals("меню") || msg.getText().equals("/start")) {
                goToMain(lastMessageId);
                lastMessageId = 0;
                return true;
            } else {
                return false;
            }
        };

        menuLvl = msg -> {
            log.info("Menu level");
            switch (msg.getText()) {
                case "home_control":
                    if (service.isExists(chatId))
                        goToHomeControlLevel(msg);
                    else {
                        execute(new InlineKeyboardMessage(chatId, tokenNotFound, tokenGenButton)
                                .setMessageId(msg.getId())
                                .hasBackButton(true));
                        currentLvl = homeControlLvl;
                    }
                    break;
                case "information":
                    execute(new InlineKeyboardMessage(chatId, infoMsg, infoButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2)
                            .hasBackButton(true));
                    currentLvl = infoLvl;
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        execute(new AnswerCallback(msg.getCallbackId(), "Кнопка недействительна"));
            }
        };

        infoLvl = msg -> {
            log.info("Info level");
            switch (msg.getText()) {
                case "weather":
                    String weather = weatherService.getWeather();
                    String answer;

                    if (weather == null) answer = "Ошибка при получении погоды";
                    else answer = weather;

                    try {
                        execute(new InlineKeyboardMessage(chatId, answer, infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        execute(new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "time":
                    try {
                        execute(new InlineKeyboardMessage(chatId, String.format("Химкинское время %s",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))), infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        execute(new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "Назад":
                    goToMain(msg.getId());
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
            }
        };

        homeControlLvl = msg -> {
            log.info("HomeControl level");
            switch (msg.getText()) {
                case "devices":
                    goToDevices(null, msg);
                    break;
                case "sensors":
                    break;
                case "token_gen":
                    execute(new Message(chatId, tokenSuccessGen).setMessageId(msg.getId()));
                    execute(new Message(chatId, service.createToken(chatId)));
                    goToMain(0);
                    break;
                case "Назад":
                    goToMain(msg.getId());
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
            }
        };

        devicesLvl = msg -> {
            log.info("Devices level");
            if (msg.getText().equals("Добавить")) {
                creator = new DeviceCreator();
                creator.start(msg);
                currentLvl = deviceCreationLvl;
            } else if (msg.getText().equals("Назад")) {
                goToHomeControlLevel(msg);
            }
            // Если ответ содержит только число, то пытаемся получить устройство с таким id
            else if (msg.getText().matches("[-+]?\\d+")) {
                try {
                    goToDevice(msg, Integer.parseInt(msg.getText()));
                    currentLvl = deviceControlLvl;
                } catch (ChannelNotFoundException e) {
                    log.error(e.getMessage());
                    goToHomeControlLevel(msg);
                }
            } else {
                if (!msg.getCallbackId().isEmpty())
                    execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
            }
        };

        deviceControlLvl = msg -> {
            String[] arr = msg.getText().split("[_]");
            String cmd = arr[0];
            int deviceId = 0;
            if (arr.length > 1) {
                deviceId = Integer.parseInt(arr[1]);
            }

            switch (cmd) {
                case "off":
                    try {
                        setDigitalState(deviceId, false);
                        goToDevice(msg, deviceId);
                        execute(new AnswerCallback(msg.getCallbackId(), "Устройство выключено"));
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "on":
                    try {
                        setDigitalState(deviceId, true);
                        goToDevice(msg, deviceId);
                        execute(new AnswerCallback(msg.getCallbackId(), "Устройство включено"));
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "remove":
                    // Нужно добавить подтверждение удаления
                    try {
                        deleteOutput(Integer.parseInt(arr[1]));
                        goToDevices("Устройство успешно удалено", msg);
                    } catch (ChannelNotFoundException e) {
                        log.error(e.getMessage());
                        goToHomeControlLevel(msg);
                    }
                    break;
                case "Назад":
                    goToDevices(null, msg);
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        execute(new AnswerCallback(msg.getCallbackId(), "Команда не найдена в разделе"));
            }
        };

        deviceCreationLvl = msg -> {
            if (msg.getText().equals("Назад")) {
                creator.goToPrev(msg);
            } else creator.currCreationLvl.accept(msg);
        };
    }

    void sendAnswer(IncomingMessage msg) {
        if (!defaultLvl.test(msg)) {
            if (currentLvl != null) {
                currentLvl.accept(msg);
            } else {
                delete(msg.getId());
            }
        }
    }

    // LEVELS
    private void goToMain(int messageId) {
        execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                .setMessageId(messageId)
                .setNumOfColumns(2));
        currentLvl = menuLvl;
    }

    private void goToHomeControlLevel(IncomingMessage msg) {
        if (service.isChannelExist(chatId)) {
            execute(new InlineKeyboardMessage(chatId, homeControl, homeControlButtons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            currentLvl = homeControlLvl;
        } else {
            execute(new AnswerCallback(msg.getCallbackId(), channelNotFound).setAlert(true));
            if (currentLvl != menuLvl) {
                execute(new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2));
                currentLvl = menuLvl;
            }
        }
    }

    private void goToDevices(String text, IncomingMessage msg) {
        try {
            List<CallbackButton> devices = new ArrayList<>();

            for (Output output : getOutputs()) {
                devices.add(new CallbackButton(output.getName(), String.valueOf(output.getOutputId())));
            }
            String answerText;
            if (devices.isEmpty()) {
                answerText = "Устройства не обнаружены. Вы можете добавить их прямо сейчас";
            } else answerText = devicesMsg;

            if (text != null) execute(new AnswerCallback(msg.getCallbackId(), text));

            execute(new InlineKeyboardMessage(chatId, answerText, devices)
                    .hasAddButton(true)
                    .hasBackButton(true)
                    .setMessageId(msg.getId())
                    .setNumOfColumns(2));
            currentLvl = devicesLvl;
        } catch (ChannelNotFoundException e) {
            log.warn(e.getMessage());
            goToHomeControlLevel(msg);
        }
    }

    private void goToDevice(IncomingMessage msg, int deviceId) throws ChannelNotFoundException {
        Output output = getOutput(deviceId);

        if (output == null) goToDevices("Устройство c таким id не найдено", msg);
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

            execute(new InlineKeyboardMessage(chatId, String.format("<b>%s</b>\n" +
                            "Текущее состояние: <i>%s</i>\n" +
                            "Тип сигнала: <i>%s</i>\n" +
                            "Инверсия: <i>%s</i>\n" +
                            "GPIO-пин: <i>%d</i>",
                    output.getName(), currStateText, signalType, inversion,
                    output.getGpio()), buttons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
        }
    }

    // ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI

    /* **************************************************************************************************************
     ************************* ПОЛУЧЕНИЕ И ОТПРАВКА ДАННЫХ С Raspberry PI *******************************************
     ************************************************************************************************************** */

    private boolean getDigitalState(@NonNull Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/control/digital?id=" + outputId));

        return JsonRequester.execute(request, getChannel()).getJSONObject("body").getJSONObject("entity").getBoolean("digitalState");
    }

    private int getPwmSignal(@NonNull Integer outputId) throws ChannelNotFoundException {
        JSONObject request = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs/control/pwm?id=" + outputId));

        return JsonRequester.execute(request, getChannel()).getJSONObject("body").getJSONObject("entity").getInt("pwmSignal");
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
    }

    private Channel getChannel() throws ChannelNotFoundException {
        return service.getChannel(chatId);
    }

    // **************************************************************************************************************

    /* **************************************************************************************************************
     ************************************** СОЗДАНИЕ УСТРОЙСТВА *****************************************************
     ************************************************************************************************************** */

    @Getter(value = AccessLevel.PRIVATE)
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
                delete(lastMessageId);
            };

            stepTwo = this::setDeviceSignalType;

            stepThree = this::setDeviceGpio;

            stepFour = this::setDeviceReverse;
        }

        private void goToPrev(IncomingMessage msg) {
            if (currCreationLvl == stepOne) {
                goToDevices(null, msg);
            } else if (currCreationLvl == stepTwo) {
                start(msg);
            } else if (currCreationLvl == stepThree) {
                goToStepTwo(msg);
            } else if (currCreationLvl == stepFour) {
                goToStepThree(msg);
            }
        }

        private void start(IncomingMessage msg) {
            execute(new InlineKeyboardMessage(chatId, "Пожалуйста, введите имя нового устройства", null)
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
            execute(new InlineKeyboardMessage(chatId, "Выберите тип сигнала, который " +
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
                execute(new InlineKeyboardMessage(chatId, "Теперь выберите пин, к которому вы хотите " +
                        "подключить новое устройство", new ArrayList<CallbackButton>() {{
                    for (String s : getAvailableOutputs(creationOutput.getType()))
                        add(new CallbackButton(s, s));
                }})
                        .setMessageId(msg.getId())
                        .setNumOfColumns(6)
                        .hasBackButton(true));
                currCreationLvl = stepThree;
            } catch (ChannelNotFoundException e) {
                log.error(e.getMessage());
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
                log.error(e.getMessage());
                goToHomeControlLevel(msg);
            }
        }

        private void goToStepFour(IncomingMessage msg) {
            execute(new InlineKeyboardMessage(chatId, "Сделать инверсию сигнала для данного устройства?", yesOrNo)
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
                goToDevices("Устройство создано", msg);
            } catch (ChannelNotFoundException e) {
                log.error(e.getMessage());
                goToHomeControlLevel(msg);
            } finally {
                creationOutput = null;
                creator = null;
            }
        }
    }

    private void delete(int messageId) {
        log.info("Removing message...");
        DeleteMessage message = new DeleteMessage(chatId, messageId);
        try {
            bot.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void execute(AnswerCallback callback) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery()
                .setCallbackQueryId(callback.getCallbackId())
                .setText(callback.getText())
                .setShowAlert(callback.isAlert());
        try {
            bot.execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void execute(Message msg) {
        log.info("Sending message...");
        if (msg.getMessageId() == 0) {
            SendMessage answer = new SendMessage(msg.getChatId(), msg.getText())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = new EditMessageText()
                    .setChatId(msg.getChatId())
                    .setText(msg.getText())
                    .setMessageId(msg.getMessageId())
                    .setParseMode("HTML");
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    private void execute(InlineKeyboardMessage msg) throws RuntimeException {
        log.info("Sending message...");
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.create(chatId);

        if (msg.getButtons() != null) {
            Iterator<CallbackButton> buttons = msg.getButtons().iterator();
            while (buttons.hasNext()) {
                for (int i = 0; i < msg.getNumOfColumns(); i++) {
                    if (buttons.hasNext()) {
                        CallbackButton button = buttons.next();
                        builder.button(button.getText(), button.getCallbackText());
                    } else break;
                }
                builder.endRow().row();
            }
            builder.endRow();
        }

        if (msg.isRemoveButton())
            builder.row().button("Удалить").endRow();

        builder.row();
        if (msg.isBackButton())
            builder.button("Назад");
        if (msg.isAddButton())
            builder.button("Добавить");
        builder.endRow();

        builder.setText(msg.getText());

        if (msg.getMessageId() == 0) {
            SendMessage answer = builder.buildNew();
            try {
                bot.execute(answer);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            EditMessageText answer = builder.setMessageId(msg.getMessageId()).buildEdited();
            try {
                bot.execute(answer);
            } catch (TelegramApiRequestException e) {
                log.error(e.getMessage());
                throw new RuntimeException();
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }
}