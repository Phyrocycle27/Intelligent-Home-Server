package com.example.smarthome.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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
}
