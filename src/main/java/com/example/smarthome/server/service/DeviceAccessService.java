package com.example.smarthome.server.service;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.entity.Token;
import com.example.smarthome.server.entity.UserRole;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.netty.handler.SessionHandler;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceAccessService {

    private static final Logger LOGGER;
    private static DeviceAccessService instance;
    @Setter
    private TokensRepository tokensRepo;
    @Setter
    private TelegramUsersRepository usersRepo;

    static {
        LOGGER = Logger.getLogger(DeviceAccessService.class.getName());
    }

    public static synchronized DeviceAccessService getInstance() {
        if (instance == null) {
            instance = new DeviceAccessService();
        }
        return instance;
    }

    public String createToken(long userId) {
        String tokenStr = SecureTokenGenerator.nextToken();

        Token token = new Token(0, tokenStr, null);
        TelegramUser user = new TelegramUser(userId, UserRole.CREATOR.name(), LocalDateTime.now(), token);

        token.setUsers(new HashSet<TelegramUser>() {{
            add(user);
        }});

        tokensRepo.save(token);
        usersRepo.save(user);

        return tokenStr;
    }

    public void addUser(long userId, long newUserId, UserRole userRole) throws UserAlreadyExistsException {

        if (isExists(newUserId)) throw new UserAlreadyExistsException(newUserId);

        Token token = usersRepo.getOne(userId).getToken();
        TelegramUser user = new TelegramUser(newUserId, userRole.name(), LocalDateTime.now(), token);

        usersRepo.save(user);
    }

    public List<TelegramUser> getUsers(long userId) {
        Token token = usersRepo.getOne(userId).getToken();
        return usersRepo.findByToken(token);
    }

    public void deleteUser(long userId) {
        usersRepo.deleteById(userId);
    }

    public TelegramUser getUser(long userId) throws UserNotFoundException {
        return usersRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    public Channel getChannel(long userId) throws ChannelNotFoundException {
        Channel ch = null;
        try {
            ch = SessionHandler.getChannel(usersRepo.findById(userId).orElseThrow(
                    () -> new UserNotFoundException(userId)).getToken().getToken());
        } catch (UserNotFoundException e) {
            e.printStackTrace();
        }

        if (ch == null) throw new ChannelNotFoundException(userId);
        return ch;
    }

    public boolean isChannelExist(long userId) {
        boolean flag = false;
        try {
            flag = getChannel(userId) != null;
        } catch (ChannelNotFoundException e) {
            LOGGER.log(Level.ALL, e.getMessage());
        }
        return flag;
    }

    public boolean isExists(long userId) {
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
