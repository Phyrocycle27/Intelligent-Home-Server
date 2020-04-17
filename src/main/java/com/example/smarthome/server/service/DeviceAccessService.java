package com.example.smarthome.server.service;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.entity.Token;
import com.example.smarthome.server.exceptions.ChannelNotFoundException;
import com.example.smarthome.server.exceptions.UserAlreadyExistsException;
import com.example.smarthome.server.exceptions.UserNotFoundException;
import com.example.smarthome.server.netty.handler.SessionHandler;
import com.example.smarthome.server.repository.TelegramUsersRepository;
import com.example.smarthome.server.repository.TokensRepository;
import com.example.smarthome.server.telegram.objects.UserRole;
import io.netty.channel.Channel;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceAccessService {

    private static final Logger log = LoggerFactory.getLogger(DeviceAccessService.class);
    @Getter
    private static final DeviceAccessService instance = new DeviceAccessService();
    @Setter
    private TokensRepository tokensRepo;
    @Setter
    private TelegramUsersRepository usersRepo;

    public String createToken(long userId) {
        String tokenStr = SecureTokenGenerator.nextToken();

        Token token = new Token(0, tokenStr, null);
        TelegramUser user = new TelegramUser(userId, UserRole.CREATOR.getName(), LocalDateTime.now(), token);

        token.setUsers(new HashSet<TelegramUser>() {{
            add(user);
        }});

        tokensRepo.save(token);

        return tokenStr;
    }

    public void addUser(long userId, long newUserId, UserRole userRole) throws UserAlreadyExistsException {

        if (isUserExists(newUserId)) throw new UserAlreadyExistsException(newUserId);

        Token token = usersRepo.getOne(userId).getToken();
        TelegramUser user = new TelegramUser(newUserId, userRole.getName(), LocalDateTime.now(), token);

        usersRepo.save(user);
    }

    public void changeUserRole(long userId, UserRole role) throws UserNotFoundException {
        TelegramUser user = usersRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        user.setRole(role.getName());
        usersRepo.save(user);
    }

    public List<TelegramUser> getUsers(long userId) throws UserNotFoundException {
        Token token = usersRepo.findById(userId).orElseThrow(() -> new UserNotFoundException(userId)).getToken();
        return usersRepo.findByToken(token);
    }

    public void deleteUser(long userId) throws UserNotFoundException {
        TelegramUser deleted = getUser(userId);

        if (UserRole.getByName(deleted.getRole()) == UserRole.CREATOR) {
            // назначаем следующего пользователя создателем
            List<TelegramUser> users = usersRepo.findByTokenOrderByAdditionDateAsc(deleted.getToken());
            if (!users.isEmpty()) {
                for (int i = 0; i < users.size(); i++) {
                    TelegramUser user = users.get(i);
                    if (UserRole.getByName(user.getRole()) == UserRole.ADMIN) {
                        user.setRole(UserRole.CREATOR.getName());
                        usersRepo.save(user);
                        break;
                    } else if (i + 1 == users.size()) {
                        user = users.get(0);
                        user.setRole(UserRole.CREATOR.getName());
                        usersRepo.save(user);
                        break;
                    }
                }
            } else {
                tokensRepo.deleteById(deleted.getToken().getId());
            }
        }
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
            log.warn(e.getMessage());
        }
        return flag;
    }

    public boolean isUserExists(long userId) {
        return usersRepo.existsById(userId);
    }

    public boolean isTokenExists(String token) {
        return !tokensRepo.findByToken(token).isEmpty();
    }

    public static class SecureTokenGenerator {

        private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        public static final int SECURE_TOKEN_LENGTH = 32;

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
