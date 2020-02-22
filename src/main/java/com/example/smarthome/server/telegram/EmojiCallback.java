package com.example.smarthome.server.telegram;

import com.example.smarthome.server.telegram.objects.callback.AnswerCallback;

import static com.example.smarthome.server.telegram.MessageExecutor.execute;

public class EmojiCallback {

    private static final Bot bot = Bot.getInstance();

    public static void success(String callbackId) {
        execute(bot, new AnswerCallback(callbackId, "\u2705"));
    }

    public static void next(String callbackId) {
        execute(bot, new AnswerCallback(callbackId, "\u27a1"));
    }

    public static void back(String callbackId) {
        execute(bot, new AnswerCallback(callbackId, "\u2B05"));
    }
}
