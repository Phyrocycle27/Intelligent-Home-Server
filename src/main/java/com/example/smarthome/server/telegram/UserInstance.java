package com.example.smarthome.server.telegram;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.*;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class UserInstance {
    /* ***************************** MAIN ******************************************************* */
    private static final String defaultMain = "Команды '%s' не существует. Пожалуйста, " +
            "наберите команду \"меню\" чтобы воспользоваться ботом";

    /* ********************************* DEFAULT IN SECTION ************************************* */
    private static final String defaultSection = "Команды '%s' нет в данном разделе. Пожалуйста, " +
            "выберите один из предложенных вариантов";

    /* ************************************* MENU *********************************************** */
    private static final String menuMsg = "Нажмите \"Управление домом\" чтобы перейти к управлению умным домом и " +
            "просмотру информации с датчиков или нажмите \"Информация\", чтобы узнать точное время или погоду";
    private static final List<String> menuButtons;

    /* ************************************* INFO ************************************************ */
    private static final String infoMsg = "Выберите \"Погода\" чтобы узнать погоду в совём городе " +
            "или нажмите \"Время\"чтобы узнать точное время в вашем городе";
    private static final List<String> infoButtons;

    /* ********************************** NOTHING ************************************************ */
    private static final String nothingToShow = "Тут пока ничего нет...";

    /* *********************************** HOME CONTROL *************************************** */
    private static final List<String> homeControlBtns;
    private static final String addBtn = "Добавить";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final List<String> tokenGenBtn;
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";

    /* ******************************** STATIC FINAL VARIABLES ********************************************** */
    private static final Logger LOGGER;
    private static final YandexWeather weatherService;
    private static final DeviceAccessService service;

    static {
        LOGGER = Logger.getLogger(Bot.class.getName());
        weatherService = new YandexWeather();
        service = DeviceAccessService.getInstance();

        menuButtons = new ArrayList<String>() {{
            add("Управление домом");
            add("Информация");
        }};

        infoButtons = new ArrayList<String>() {{
            add("Погода");
            add("Время");
        }};

        tokenGenBtn = new ArrayList<String>() {{
            add("Сгенерировать токен");
        }};

        homeControlBtns = new ArrayList<String>() {{
            add("Устройства");
            add("Датчики");
        }};
    }

    /* ************************************ TEMP VARIABLES ************************************************** */
    private Output creationOutput;
    private Map<String, Integer> outputsMap;

    private int level = 0;
    private int subLevel = 0;
    private long userId;

    UserInstance(long userId) {
        this.userId = userId;
    }

    List<SendMessage> getMessage(String incoming) {
        List<SendMessage> messages = new ArrayList<>();

        if (incoming.equals("меню") || incoming.equals("/start")) {
            messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
            level = 1;
        } else if (level == 0)
            messages.add(createMsg(userId).setText(String.format(defaultMain, incoming)));

        if (!messages.isEmpty()) return messages;

        switch (level) {
            case 1:
                switch (incoming) {
                    case "управление домом":
                        if (service.isExists(userId)) {
                            messages.add(getHomeControl());
                        } else
                            messages.add(getKeyboard(tokenNotFound, tokenGenBtn, userId, true));
                        subLevel = 1;
                        level++;
                        break;
                    case "информация":
                        messages.add(getKeyboard(infoMsg, infoButtons, userId, true));
                        subLevel = 2;
                        level++;
                        break;
                    default:
                        messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                }
                break;
            case 2:
                switch (subLevel) {
                    case 1:
                        switch (incoming) {
                            case "устройства":
                                try {
                                    List<String> outputsBtns = new ArrayList<>();

                                    for (Output output : getOutputs(null)) {
                                        outputsBtns.add(output.getName());
                                        outputsMap.put(output.getName(), output.getOutputId());
                                        outputsBtns.add(addBtn);
                                    }

                                    messages.add(getKeyboard("Нажмите на существующее устройство или " +
                                                    "добавьте новое",
                                            outputsBtns, userId, true));

                                    level++;
                                    subLevel = 1;
                                } catch (ChannelNotFoundException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage());
                                    messages.add(getKeyboard(channelNotFound, null, userId, true));
                                }
                                break;
                            case "датчики":
                                // Запрашиваем с raspberry pi все подключенные датчики и выводим их как кнопки
                                List<String> inputsBtns = new ArrayList<>();

                                inputsBtns.add(addBtn);

                                messages.add(getKeyboard("Нажмите на существующий датчик или добавьте новый",
                                        inputsBtns, userId, true));
                                level++;
                                subLevel = 2;
                                break;
                            case "сгенерировать токен":
                                try {
                                    messages.add(createMsg(userId)
                                            .setText("Ваш токен успешно сгенерирован!\n" +
                                                    "Он требуется для подключения Вашего устройства к серверу.\n" +
                                                    "Пожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в" +
                                                    "приложении, чтобы Ваше устройство могло подключиться к серверу\n\n" +
                                                    "\u2B07\u2B07\u2B07 ВАШ ТОКЕН \u2B07\u2B07\u2B07"));
                                    // Сообщение с токеном
                                    messages.add(createMsg(userId)
                                            .setText(service.createToken(userId)));

                                } catch (UserAlreadyExistsException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage());
                                    messages.clear();

                                    messages.add(createMsg(userId)
                                            .setText("Вам уже выдан токен. Введите его, пожалуйста, в соответствующем разделе " +
                                                    "в приложении чтобы Ваше устройство могло подключиться к серверу"));
                                }
                                break;
                            case "назад":
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
                                subLevel = 0;
                                level--;
                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                    case 2:
                        switch (incoming) {
                            case "погода":
                                String weather = weatherService.getWeather();
                                SendMessage msg = createMsg(userId);

                                if (weather == null) msg.setText("Ошибка при получении погоды");
                                else msg.setText(weather);

                                messages.add(msg);
                                break;
                            case "время":
                                messages.add(createMsg(userId)
                                        .setText(String.format("Химкинское время %s",
                                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))));
                                break;
                            case "назад":
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false));
                                subLevel = 0;
                                level--;
                                break;
                            default:
                                messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                }
                break;
            case 3:
                switch (subLevel) {
                    case 1:
                        if (incoming.equals("добавить")) {
                            // код для добавления кнопки
                        } else if (incoming.equals("назад")) {
                            messages.add(getHomeControl());
                            subLevel = 1;
                            level--;
                        } else if (outputsMap.containsKey(incoming)) {
                            // код для показа информации о кнопке
                        } else {
                            messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                    case 2:
                        if (incoming.equals("добавить")) {
                            // код для добавления кнопки
                        } else if (incoming.equals("назад")) {
                            messages.add(getHomeControl());
                            subLevel = 1;
                            level--;
                        } /*else if () {
                            // код для показа информации о кнопке
                        }*/ else {
                            messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                }
                break;
        }

        return messages;
    }

    private Output getOutputByName(String name) throws ChannelNotFoundException {
        for (Output output : getOutputs(null)) {
            if (output.getName().equals(name)) return output;
        }
        return null;
    }

    private List<Output> getOutputs(String type) throws ChannelNotFoundException {
        List<Output> outputList = new ArrayList<>();
        // метод, в котором мы получаем устройства с Raspberry PI
        // собираем запрос
        try {
            URI uri = new URI(String.format("http://localhost:8080/outputs?type=%s", type != null ? type : ""));

            HttpGet request = new HttpGet(uri);
            request.addHeader("content-type", "application/json");

            // Составляем HTTP запрос из JSON
            JSONObject jsonRequest = new JSONObject()
                    .put("type", "request")
                    .put("body", new JSONObject()
                            .put("method", "GET")
                            .put("uri", uri));

            JSONObject data = getDataFromClient(jsonRequest);

            LOGGER.log(Level.INFO, data.toString());


        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return outputList;
    }

    private JSONObject getDataFromClient(JSONObject request) throws ChannelNotFoundException {
        JSONObject obj = new JSONObject();
        Channel ch = service.getChannel(userId);


            ChannelFuture f = ch.writeAndFlush(request.toString()).addListener((ChannelFutureListener) channelFuture -> {
                LOGGER.log(Level.INFO, "Waiting message from Raspberry PI...");

                if (ch.pipeline().names().contains("msgTmpReader"))
                    ch.pipeline().remove("msgTmpReader");

                ch.pipeline().addBefore("sessionHandler", "msgTmpReader", new ChannelInboundHandlerAdapter() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        LOGGER.log(Level.INFO, String.format("This is answer from client: %s", msg));

                        obj.put("head", new JSONObject(msg.toString()));
                    }

                    @Override
                    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                        LOGGER.log(Level.INFO, "Handler deleting");
                        ch.pipeline().remove(this);

                        synchronized (obj) {
                            obj.notify();
                        }
                    }
                });
            });

        try {
            synchronized (obj) {
                obj.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return obj;
    }

    private SendMessage getHomeControl() {
        SendMessage msg;
        try {
            service.getChannel(userId);
            msg = getKeyboard("Выберите Устройства, чтобы управлять устройствами " +
                    "или добавить новое, или выберите Датчики чтобы посмотреть показания или добавить " +
                    "новый датчик", homeControlBtns, userId, true);
        } catch (ChannelNotFoundException e) {
            LOGGER.log(Level.WARNING, e.getMessage());
            msg = getKeyboard(channelNotFound, null, userId, true);
        }
        return msg;
    }

    private SendMessage createMsg(long chatId) {
        return new SendMessage().setChatId(chatId);
    }

    private SendMessage getKeyboard(String messageText, List<String> buttonsText, long userId, Boolean prevBtn) {
        SendMessage msg = createMsg(userId);

        if (messageText != null)
            msg.setText(messageText);

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup()
                .setResizeKeyboard(true)
                .setOneTimeKeyboard(false)
                .setSelective(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        if (buttonsText != null) {
            KeyboardRow row = new KeyboardRow();
            for (int i = 1; i <= buttonsText.size(); i++) {
                row.add(buttonsText.get(i - 1));

                if (i % 2 == 0 & i != buttonsText.size()) {
                    keyboard.add(row);
                    row = new KeyboardRow();
                }
            }
            keyboard.add(row);
        }

        if (prevBtn) {
            KeyboardRow row = new KeyboardRow();
            row.add("Назад");
            keyboard.add(row);
        }

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);

        return msg;
    }
}
