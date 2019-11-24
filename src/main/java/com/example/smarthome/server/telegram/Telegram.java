package com.example.smarthome.server.telegram;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Telegram implements Runnable {

    private static Logger LOGGER = Logger.getLogger(Telegram.class.getName());
    private final String PROXY_HOST = null;
    private final Integer PROXY_PORT = 0;

    public Telegram(String proxyHost, Integer proxyPort) {
        /*this.PROXY_HOST = proxyHost;
        this.PROXY_PORT = proxyPort;*/
        new Thread(this, "Telegram Thread").start();
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Telegram thread is running");

        ApiContextInitializer.init();

        TelegramBotsApi telegram = new TelegramBotsApi();
        Bot bot;

        if (PROXY_HOST != null && PROXY_PORT != 0) {
            HttpHost httpHost = new HttpHost(PROXY_HOST, PROXY_PORT);

            DefaultBotOptions options = ApiContext.getInstance(DefaultBotOptions.class);
            RequestConfig requestConfig = RequestConfig.custom().setProxy(httpHost).setAuthenticationEnabled(false).build();
            options.setRequestConfig(requestConfig);

            bot = new Bot(options);
        } else bot = new Bot();

        try {
            telegram.registerBot(bot);
            LOGGER.log(Level.INFO, "Successful connection to Telegram API server!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
