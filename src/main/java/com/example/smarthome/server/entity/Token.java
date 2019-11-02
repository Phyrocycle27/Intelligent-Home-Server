package com.example.smarthome.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "tokens")
@AllArgsConstructor
@NoArgsConstructor
public class Token {

    @Id
    @Column(name = "id", unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "token", nullable = false, unique = true, updatable = false, length = 32)
    private String token;

    @OneToMany(mappedBy = "token", cascade = {CascadeType.ALL})
    private Set<TelegramUser> users = new HashSet<>();
}
