package com.example.smarthome.server.repository;

import com.example.smarthome.server.entity.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramUsersRepository extends JpaRepository<TelegramUser, Long> {

}
