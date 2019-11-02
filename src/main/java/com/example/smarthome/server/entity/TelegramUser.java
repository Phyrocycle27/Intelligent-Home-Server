package com.example.smarthome.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Entity
@Table(name = "telegram_users")
@AllArgsConstructor
@NoArgsConstructor
public class TelegramUser {
    @Id
    @Column(unique = true, updatable = false, nullable = false)
    private long user_id;

    @Column(length = 5, nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;
}
