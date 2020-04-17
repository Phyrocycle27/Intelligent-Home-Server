package com.example.smarthome.server.entity;

import lombok.*;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "telegram_users")
@EqualsAndHashCode(of = {"userId"})
@ToString(of = {"userId", "role", "additionDate", "token"})
@AllArgsConstructor
@NoArgsConstructor
@Proxy(lazy = false)
public class TelegramUser {

    @Id
    @Column(unique = true, updatable = false, nullable = false, name = "user_id")
    private long userId;

    @Column(length = 10, nullable = false)
    private String role;

    @Column(updatable = false, nullable = false, name = "addition_date")
    private LocalDateTime additionDate;

    @JoinColumn(name = "token_id", nullable = false)
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Token token;
}
