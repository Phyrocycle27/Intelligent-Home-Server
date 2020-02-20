package com.example.smarthome.server.telegram;

import com.example.smarthome.server.connection.JsonRequester;
import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.objects.*;
import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;
import com.example.smarthome.server.telegram.objects.callback.CallbackButton;
import com.example.smarthome.server.telegram.objects.inlinemsg.InlineKeyboardMessage;
import com.example.smarthome.server.telegram.scenario.AnswerCreator;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UserInstance {
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
    private static final String errorDownloadingWeather = "Ошибка при получении погоды";
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
    private static final String devicesNotFound = "Устройства не обнаружены. Вы можете добавить их прямо сейчас";
    private static final String removeConfirmationDevice = "Вы действительно хотите удалить это устройство?";
    private static final String removeConfirmationUser = "Вы действительно хотите удалить этого пользователя?";
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
    private static final List<CallbackButton> homeControlButtons = new ArrayList<CallbackButton>() {{
        add(new CallbackButton("Устройства", "devices"));
        add(new CallbackButton("Датчики", "sensors"));
        add(new CallbackButton("Пользователи", "users"));
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
    private static final String buttonInvalid = "Кнопка недействительна";
    // ******************************** STATIC FINAL VARIABLES **********************************************
    private static final Logger log;
    private static final Weather weatherService;
    private static final DeviceAccessService service;
    private static Bot bot;

    /* ************************************ TEMP VARIABLES ************************************************** */
    private DeviceCreator creator;

    public long getChatId() {
        return chatId;
    }

    private long chatId;
    private int lastMessageId;

    // Scenery

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
    /**
     * Главное меню
     */
    private Consumer<IncomingMessage> menuLvl;
    /**
     * Дополнительная информация. Погода и текущее время в Химках
     */
    private Consumer<IncomingMessage> infoLvl;
    /**
     * Выбор перехода: датчики или устройства
     *
     * @apiNote На этот уровень пользователя будет также перебрасывать тогда, когда оборвётся соединение с его
     * Raspberry PI
     */
    private Consumer<IncomingMessage> homeControlLvl;
    /**
     * Уровень, загружающий список созданных устройств. Тут пользователь может либо добавть новое, либо управлять
     * существующим
     */
    private Consumer<IncomingMessage> devicesLvl;
    /**
     * Уровень управления устройствами
     */
    private Consumer<IncomingMessage> deviceControlLvl;
    /**
     * Создание устройств
     */
    private Consumer<IncomingMessage> deviceCreationLvl;
    /**
     * Подтверждение удаления устройства
     */
    private Consumer<IncomingMessage> confirmRemove;

    private Consumer<IncomingMessage> usersLvl;
    private Consumer<IncomingMessage> userLvl;
    private Consumer<IncomingMessage> userAdditionLvl;

    static {
        log = LoggerFactory.getLogger(Bot.class);
        weatherService = Weather.getInstance();
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
            if (msg.getText().equals("/start")) {
                if (lastMessageId != 0) {
                    goToMain(0);
                    lastMessageId = 0;
                } else goToMain(0);
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
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, tokenNotFound, tokenGenButton)
                                .setMessageId(msg.getId())
                                .hasBackButton(true));
                        currentLvl = homeControlLvl;
                    }
                    break;
                case "information":
                    MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, infoMsg, infoButtons)
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2)
                            .hasBackButton(true));
                    currentLvl = infoLvl;
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
        };

        infoLvl = msg -> {
            log.info("Info level");
            switch (msg.getText()) {
                case "weather":
                    String weather = weatherService.getWeather();
                    String answer;

                    if (weather == null) answer = "";
                    else answer = weather;

                    try {
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, answer, infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "time":
                    try {
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, String.format("Химкинское время %s",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))), infoButtons)
                                .setMessageId(msg.getId())
                                .setNumOfColumns(2)
                                .hasBackButton(true));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), "\u2705"));
                    }
                    break;
                case "back":
                    goToMain(msg.getId());
                    break;
                default:
                    if (!msg.getCallbackId().isEmpty())
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
            }
        };

        homeControlLvl = msg -> {
            log.info("HomeControl level");
            if (msg.getType() == MessageType.CALLBACK)
                switch (msg.getText()) {
                    case "devices":
                        goToDevicesLevel(null, msg);
                        break;
                    case "users":
                        goToUsersLevel(msg.getId(), null);
                        break;
                    case "token_gen":
                        MessageExecutor.execute(bot, new Message(chatId, tokenSuccessGen).setMessageId(msg.getId()));
                        MessageExecutor.execute(bot, new Message(chatId, service.createToken(chatId)));
                        goToMain(0);
                        break;
                    case "back":
                        goToMain(msg.getId());
                        break;
                    default:
                        MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), buttonInvalid));
                }
        };

        usersLvl = msg -> {
            log.info("Users level");
            if (msg.getType() == MessageType.CALLBACK)
                switch (msg.getText()) {
                    case "add":
                        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, "Отправьте контакт " +
                                "пользователя, которого хотите добавить", null)
                                .hasBackButton(true).setMessageId(msg.getId()));
                        currentLvl = userAdditionLvl;
                        lastMessageId = msg.getId();
                        break;
                    case "back":
                        goToHomeControlLevel(msg);
                        break;
                    default:
                        goToUserLevel(msg.getId(), Long.parseLong(msg.getText()));
                }
        };

        userLvl = msg -> {
            log.info("User level");
            if (msg.getType() == MessageType.CALLBACK) {
                if (msg.getText().equals("back")) {
                    goToUsersLevel(msg.getId(), null);
                    return;
                }

                String[] arr = msg.getText().split("[_]");
                String cmd = arr[0];
                long userId = Long.parseLong(arr[1]);

                if (cmd.equals("remove")) {
                    MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, removeConfirmationUser,
                            new ArrayList<CallbackButton>() {{
                                add(new CallbackButton("Подтвердить", "confirmRemove_user_" + userId));
                                add(new CallbackButton("Отмена", "cancel_user_" + userId));
                            }})
                            .setMessageId(msg.getId())
                            .setNumOfColumns(2));
                    currentLvl = confirmRemove;
                }
            }
        };

        userAdditionLvl = msg -> {
            log.info("User addition level");
            if (msg.getType() == MessageType.CALLBACK && msg.getText().equals("back")) {
                goToUsersLevel(lastMessageId, null);
            } else if (msg.getType() == MessageType.CONTACT) {
                try {
                    service.addUser(chatId, Long.parseLong(msg.getText()), "user");
                    goToUsersLevel(0, "Пользователь добавлен");
                } catch (UserAlreadyExistsException e) {
                    goToUsersLevel(0, "Данный пользователь уже имеет доступ");
                } finally {
                    MessageExecutor.delete(bot, chatId, lastMessageId);
                    lastMessageId = 0;
                }
            }
        };

        devicesLvl = msg -> {
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
                case "user":
                    long userId = Long.parseLong(arr[2]);
                    if (cmd.equals("confirmRemove")) {
                        service.deleteUser(userId);
                        goToUsersLevel(msg.getId(), "Пользователь удалён");
                    } else if (cmd.equals("cancel")) {
                        goToUserLevel(msg.getId(), userId);
                    }
                    break;
            }
        };

        deviceCreationLvl = msg -> {
            log.info("Device creation level");
            if (msg.getText().equals("back")) {
                creator.goToPrev(msg);
            } else creator.currCreationLvl.accept(msg);
        };
    }

    void sendAnswer(IncomingMessage msg) {
        if (!defaultLvl.test(msg)) {
            if (currentLvl != null) {
                currentLvl.accept(msg);
            } else MessageExecutor.delete(bot, chatId, msg.getId());
        }
    }

    // LEVELS
    private void goToMain(int messageId) {
        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                .setMessageId(messageId)
                .setNumOfColumns(2));
        currentLvl = menuLvl;
    }

    private void goToHomeControlLevel(IncomingMessage msg) {
        if (service.isChannelExist(chatId)) {
            MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, homeControl, homeControlButtons)
                    .setNumOfColumns(2)
                    .setMessageId(msg.getId())
                    .hasBackButton(true));
            currentLvl = homeControlLvl;
        } else {
            MessageExecutor.execute(bot, new AnswerCallback(msg.getCallbackId(), channelNotFound).hasAlert(true));
            if (currentLvl != menuLvl) {
                MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, menuMsg, menuButtons)
                        .setMessageId(msg.getId())
                        .setNumOfColumns(2));
                currentLvl = menuLvl;
            }
        }
    }

    public void goToUsersLevel(int messageId, String s) {
        String text = s == null ? "Список пользователей, имеющих доступ к вашему дому" : s;
        List<CallbackButton> users = new ArrayList<>();

        for (TelegramUser user : service.getUsers(chatId)) {
            users.add(new CallbackButton(bot.getUserName(user.getUserId()), String.valueOf(user.getUserId())));
        }

        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, text, users)
                .hasAddButton(true)
                .hasBackButton(true)
                .setMessageId(messageId));
        currentLvl = usersLvl;
    }

    public void goToUserLevel(int messageId, long userId) {
        TelegramUser user = service.getUser(userId);
        MessageExecutor.execute(bot, new InlineKeyboardMessage(chatId, String.format(
                "<i>%s</i>\nУровень доступа: %s\nДата добавления: %s",
                bot.getUserName(userId), user.getRole(), user.getAdditionDate()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))),
                new ArrayList<CallbackButton>() {{
                    add(new CallbackButton("Удалить", "remove_" + userId));
                }})
                .setMessageId(messageId)
                .hasBackButton(true));
        currentLvl = userLvl;
    }

    private void goToDevicesLevel(String text, IncomingMessage msg) {
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
    }

    private void goToDeviceLevel(IncomingMessage msg, int deviceId) throws ChannelNotFoundException {
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
    }
}