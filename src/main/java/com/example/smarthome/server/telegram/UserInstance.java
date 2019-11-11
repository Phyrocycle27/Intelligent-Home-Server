package com.example.smarthome.server.telegram;

import com.example.smarthome.server.entity.Output;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.service.DeviceAccessService;
import io.netty.channel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
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
    private static final String channelNotFound = "Ваша Raspberry PI не подключена к серверу\n" +
            "Введите, пожалуйста, свой токен в соответствующем разделе в приложении чтобы Ваше устройство могло " +
            "подключиться к серверу";
    private static final String homeControl = "Выберите Устройства, чтобы управлять устройствами или добавить новое, или " +
            "выберите Датчики чтобы посмотреть показания или добавить новый датчик";
    // ********************* TOKEN **********************
    private static final String tokenAlreadyHaveGot = "Вам уже выдан токен. Введите его, пожалуйста, в соответствующем разделе " +
            "в приложении чтобы Ваше устройство могло подключиться к серверу";
    private static final String tokenSuccessGen = "Ваш токен успешно сгенерирован!\nОн требуется для подключения Вашего " +
            "устройства к серверу.\nПожалуйста, скопируйте и вставьте Ваш токен в соответствующий раздел в приложении, " +
            "чтобы Ваше устройство могло подключиться к серверу\n\n\u2B07\u2B07\u2B07 ВАШ ТОКЕН \u2B07\u2B07\u2B07";
    private static final String tokenNotFound = "Похоже, что у Вас нет токена...\nЧтобы управлять домом через этого телеграм " +
            "бота Вам нужен уникальный токен, который Ваша Raspberry PI будет использовать для подключения у серверу";
    private static final List<String> tokenGenBtn;
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
        outputsMap = new HashMap<>();
    }

    List<SendMessage> getMessage(String incoming) {
        List<SendMessage> messages = new ArrayList<>();

        if (incoming.equals("меню") || incoming.equals("/start")) {
            messages.add(getKeyboard(menuMsg, menuButtons, userId, false, false));
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
                        } else {
                            messages.add(getKeyboard(tokenNotFound, tokenGenBtn, userId, true,
                                    false));
                            subLevel = 1;
                            level++;
                        }
                        break;
                    case "информация":
                        messages.add(getKeyboard(infoMsg, infoButtons, userId, true, false));
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

                                    for (Output output : getOutputs()) {
                                        outputsBtns.add(output.getName());
                                        outputsMap.put(output.getName(), output.getOutputId());
                                    }

                                    messages.add(getKeyboard("Нажмите на существующее устройство или " +
                                                    "добавьте новое",
                                            outputsBtns, userId, true, true));

                                    level++;
                                    subLevel = 1;
                                } catch (ChannelNotFoundException e) {
                                    LOGGER.log(Level.WARNING, e.getMessage());
                                    messages.add(createMsg(userId).setText(channelNotFound));
                                }
                                break;
                            case "датчики":
                                // Запрашиваем с raspberry pi все подключенные датчики и выводим их как кнопки
                                List<String> inputsBtns = new ArrayList<>();

                                messages.add(getKeyboard("Нажмите на существующий датчик или добавьте новый",
                                        inputsBtns, userId, true, true));
                                level++;
                                subLevel = 2;
                                break;
                            case "сгенерировать токен":
                                if (!service.isExists(userId)) {

                                    messages.add(getKeyboard(tokenSuccessGen, null, userId,
                                            true, false));

                                    // Сообщение с токеном
                                    messages.add(createMsg(userId)
                                            .setText(service.createToken(userId)));
                                } else
                                    messages.add(createMsg(userId)
                                            .setText(tokenAlreadyHaveGot));

                                break;
                            case "назад":
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false,
                                        false));
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
                                messages.add(getKeyboard(menuMsg, menuButtons, userId, false,
                                        false));
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
                        } else if (outputsMap.containsKey(incoming)) {
                            // код для показа информации о кнопке
                            messages.add(createMsg(userId).setText("Вот мы и получили устройство с малинки, а тут - " +
                                    "должны вывести о нём подробную информацию"));
                        } else {
                            messages.add(createMsg(userId).setText(String.format(defaultSection, incoming)));
                        }
                        break;
                    case 2:
                        if (incoming.equals("добавить")) {
                            // код для добавления кнопки
                        } else if (incoming.equals("назад")) {
                            messages.add(getHomeControl());
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

    private List<Output> getOutputs() throws ChannelNotFoundException {
        List<Output> outputs = new ArrayList<>();

        // ********************************
        JSONObject jsonRequest = new JSONObject()
                .put("type", "request")
                .put("body", new JSONObject()
                        .put("method", "GET")
                        .put("uri", "http://localhost:8080/outputs"));
        // *******************************

        JSONArray outputList = getDataFromClient(jsonRequest)
                .getJSONObject("body")
                .getJSONObject("_embedded")
                .getJSONArray("outputList");

        for (int i = 0; i < outputList.length(); i++) {
            JSONObject outputJson = outputList.getJSONObject(i);
            Output output = new Output();

            output.setName(outputJson.getString("name"));
            output.setOutputId(outputJson.getInt("outputId"));

            outputs.add(output);
        }

        return outputs;
    }

    private JSONObject getDataFromClient(JSONObject request) throws ChannelNotFoundException {
        JSONObject obj = new JSONObject();

        Channel ch = service.getChannel(userId);
        ChannelFuture f = ch.writeAndFlush(request.toString()).addListener((ChannelFutureListener) channelFuture -> {

            if (ch.pipeline().names().contains("msgTmpReader"))
                ch.pipeline().remove("msgTmpReader");

            ch.pipeline().addBefore("sessionHandler", "msgTmpReader", new ChannelInboundHandlerAdapter() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    JSONObject tmp = new JSONObject(msg.toString());

                    if (tmp.getString("type").equals("data"))
                        obj.put("body", tmp.getJSONObject("body"));
                }

                @Override
                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                    synchronized (obj) {
                        obj.notify();
                    }

                    ch.pipeline().remove(this);
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
        if (service.isChannelExist(userId)) {
            msg = getKeyboard(homeControl, homeControlBtns, userId, true, false);
            level = 2;
            subLevel = 1;
        } else {
            msg = getKeyboard(channelNotFound, menuButtons, userId, false, false);
            level = 1;
            subLevel = 0;
        }

        return msg;
    }

    private SendMessage createMsg(long chatId) {
        return new SendMessage().setChatId(chatId);
    }

    private SendMessage getKeyboard(String messageText, List<String> buttonsText, long userId, boolean containPrevBtn,
                                    boolean containAddBtn) {

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

        KeyboardRow row = new KeyboardRow();

        if (containPrevBtn)
            row.add("Назад");

        if (containAddBtn)
            row.add("Добавить");

        keyboard.add(row);

        markup.setKeyboard(keyboard);
        msg.setReplyMarkup(markup);

        return msg;
    }
}