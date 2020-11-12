package com.example.smarthome.server.telegram;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Telegram implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Telegram.class.getName());
    private final String PROXY_HOST;
    private final Integer PROXY_PORT;

    public Telegram(String proxyHost, Integer proxyPort) {
        this.PROXY_HOST = proxyHost;
        this.PROXY_PORT = proxyPort;
        new Thread(this, "Telegram Thread").start();
    }

    public Telegram() {
        PROXY_HOST = null;
        PROXY_PORT = 0;
        new Thread(this, "Telegram Thread").start();
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Telegram thread is running");

        try {
            TelegramBotsApi telegram = new TelegramBotsApi(DefaultBotSession.class);
            telegram.registerBot(new Bot());
            LOGGER.log(Level.INFO, "Successful connection to Telegram API server!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
