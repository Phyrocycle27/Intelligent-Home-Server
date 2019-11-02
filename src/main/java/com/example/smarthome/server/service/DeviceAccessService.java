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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceAccessService {

    private static DeviceAccessService instance;
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
        if(isUserRegistered(userId)) throw new UserAlreadyExistsException(userId);

        String tokenStr = SecureTokenGenerator.nextToken();

        Token token = new Token(0, tokenStr, null);
        TelegramUser user = new TelegramUser(userId, "admin", token);

        tokensRepo.save(token);
        usersRepo.save(user);

        return tokenStr;
    }

    public Channel getChannel(long userId) throws ChannelNotFoundException, UserNotFoundException {
        if (usersRepo.existsById(userId)) {

            Channel ch = ServerHandler.getChannel(usersRepo.getOne(userId).getToken().getToken());

            if (ch == null) throw new ChannelNotFoundException(userId);
            return ch;
        } else throw new UserNotFoundException(userId);
    }

    public void addUser(long newUserId, String newUserRole, long userId, String userRole) {
        // Нужно сделать
    }

    private boolean isUserRegistered(long userId) {
        return usersRepo.existsById(userId);
    }

    public boolean isExists(String token) {
        return !tokensRepo.findByToken(token).isEmpty();
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
