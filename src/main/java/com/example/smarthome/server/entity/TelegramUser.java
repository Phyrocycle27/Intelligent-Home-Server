package com.example.smarthome.server.entity;

import lombok.*;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;

@Data
@Entity
@Table(name = "telegram_users")
@EqualsAndHashCode(of = {"user_id"})
@AllArgsConstructor
@NoArgsConstructor
@Proxy(lazy = false)
public class TelegramUser {

    @Id
    @Column(unique = true, updatable = false, nullable = false)
    private long user_id;

    @Column(length = 5, nullable = false)
    private String role;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.ALL})
    @JoinColumn(name = "token_id", nullable = false)
    private Token token;
}
