package com.example.smarthome.server.repository;

import com.example.smarthome.server.entity.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TokensRepository extends JpaRepository<Token, Integer> {

    List<Token> findByToken(String token);
}
