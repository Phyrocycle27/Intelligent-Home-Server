package com.example.smarthome.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "telegram_users")
@EqualsAndHashCode(of = {"userId"})
@AllArgsConstructor
@NoArgsConstructor
@Proxy(lazy = false)
public class TelegramUser {

    @Id
    @Column(unique = true, updatable = false, nullable = false, name = "user_id")
    private long userId;

    @Column(length = 5, nullable = false)
    private String role;

    @Column(updatable = false, nullable = false, name = "addition_date")
    private LocalDateTime additionDate;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;
}
