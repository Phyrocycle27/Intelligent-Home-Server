package com.example.smarthome.server;

import com.example.smarthome.server.netty.Server;
import com.example.smarthome.server.repository.CitiesRepository;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import com.example.smarthome.server.repository.WeatherUsersRepository;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.service.WeatherService;
import com.example.smarthome.server.telegram.Telegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Main {

    private static final String PROXY_HOST = "127.0.0.1";
    private static final Integer PROXY_PORT = 9050;
    private static final int SERVER_PORT = 3141;
    public static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        log.debug("Application started");

        ApplicationContext ctx = SpringApplication.run(Main.class, args);

        // ***** INITIALIZE THE DeviceAccessService CLASS ***********/
        DeviceAccessService.getInstance().setTokensRepo(ctx.getBean(TokensRepository.class));
        DeviceAccessService.getInstance().setUsersRepo(ctx.getBean(TelegramUsersRepository.class));
        WeatherService.getInstance().setCitiesRepo(ctx.getBean(CitiesRepository.class));
        WeatherService.getInstance().setUsersRepo(ctx.getBean(WeatherUsersRepository.class));
        // *********** NETTY THREAD START ***************
        new Server(SERVER_PORT); // starting the netty server
        // *********** TELEGRAM THREAD START ************
        new Telegram(PROXY_HOST, PROXY_PORT);
    }
}