package com.example.smarthome.server.service;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.entity.Token;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.netty.handler.ServerHandler;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.logging.Logger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceAccessService {

    private static final Logger LOGGER;
    private static DeviceAccessService instance;

    static {
        LOGGER = Logger.getLogger(DeviceAccessService.class.getName());
    }

    @Setter
    private TokensRepository tokensRepo;
    @Setter
    private TelegramUsersRepository usersRepo;

    public static synchronized DeviceAccessService getInstance() {
        if (instance == null) {
            instance = new DeviceAccessService();
        }
        return instance;
    }

    public String createToken(long userId) throws UserAlreadyExistsException {
        if (isExists(userId)) throw new UserAlreadyExistsException(userId);

        String tokenStr = SecureTokenGenerator.nextToken();

        Token token = new Token(0, tokenStr, null);
        TelegramUser user = new TelegramUser(userId, "admin", token);

        token.setUsers(new HashSet<TelegramUser>(){{
            add(user);
        }});

        tokensRepo.save(token);
        usersRepo.save(user);

        return tokenStr;
    }

    public Channel getChannel(long userId) throws ChannelNotFoundException {
        Channel ch = ServerHandler.getChannel(usersRepo.getOne(userId).getToken().getToken());

        if (ch == null) throw new ChannelNotFoundException(userId);
        return ch;
    }

    public void addUser(long newUserId, String newUserRole, long userId, String userRole)
            throws UserAlreadyExistsException, UserNotFoundException {

        if (isExists(newUserId)) throw new UserAlreadyExistsException(newUserId);
        if (!isExists(userId)) throw new UserNotFoundException(userId);

        Token token = usersRepo.getOne(userId).getToken();
        TelegramUser user = new TelegramUser(newUserId, newUserRole, token);

        usersRepo.save(user);
    }

    // For Netty authHandler
    public boolean isExists(String token) {
        return !tokensRepo.findByToken(token).isEmpty();
    }

    public boolean isExists(long userId) {
        return usersRepo.existsById(userId);
    }

    private static class SecureTokenGenerator {
        private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        private static final int SECURE_TOKEN_LENGTH = 32;

        private static final SecureRandom random = new SecureRandom();

        private static final char[] symbols = CHARACTERS.toCharArray();

        private static final char[] buf = new char[SECURE_TOKEN_LENGTH];

        /**
         * Generate the next secure random token in the series.
         */
        static String nextToken() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf);
        }
    }
}
