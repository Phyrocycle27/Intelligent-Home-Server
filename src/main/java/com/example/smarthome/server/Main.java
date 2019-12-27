package com.example.smarthome.server;

import com.example.smarthome.server.netty.Server;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import com.example.smarthome.server.service.DeviceAccessService;
import com.example.smarthome.server.telegram.Telegram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import java.io.IOException;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Main {

    private static final String PROXY_HOST = "127.0.0.1";
    private static final Integer PROXY_PORT = 9050;
    private static final int SERVER_PORT = 3141;
    public static final Logger log;

    static {
        log = LoggerFactory.getLogger(Main.class);
    }

    public static void main(String[] args) throws IOException {
        log.debug("Application started");

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