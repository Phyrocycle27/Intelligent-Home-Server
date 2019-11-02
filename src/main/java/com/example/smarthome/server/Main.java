package com.example.smarthome.server;

import com.example.smarthome.server.netty.Server;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Telegram;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
public class Main {

    private static final String PROXY_HOST = "151.80.199.89";
    private static final Integer PROXY_PORT = 3128;
    private static final int SERVER_PORT = 3141;
    private static Logger LOGGER;

    static {
        LOGGER = Logger.getLogger(Main.class.getName());
    }

    public static void main(String[] args) throws IOException {
        LOGGER.log(Level.INFO, "Application started");

        ApplicationContext ctx = SpringApplication.run(Main.class, args);

        // ***** INITIALIZE THE DeviceAccessService CLASS ***********/
        DeviceAccessService service = DeviceAccessService.getInstance();

        service.setTokensRepo(ctx.getBean(TokensRepository.class));
        service.setUsersRepo(ctx.getBean(TelegramUsersRepository.class));
        // *********** NETTY THREAD START **********
        new Server(SERVER_PORT); // starting the netty server

        // *********** TELEGRAM THREAD START ********
        new Telegram(PROXY_HOST, PROXY_PORT);
    }
}
