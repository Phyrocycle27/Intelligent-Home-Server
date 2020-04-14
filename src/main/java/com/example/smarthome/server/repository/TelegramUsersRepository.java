package com.example.smarthome.server.repository;

import com.example.smarthome.server.entity.TelegramUser;
import com.example.smarthome.server.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelegramUsersRepository extends JpaRepository<TelegramUser, Long> {

    List<TelegramUser> findByToken(Token token);

    List<TelegramUser> findByTokenOrderByAdditionDateAsc(Token token);
}
