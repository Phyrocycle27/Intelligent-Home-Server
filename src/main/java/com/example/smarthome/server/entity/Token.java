package com.example.smarthome.server.entity;

import com.example.smarthome.server.service.DeviceAccessService;
import lombok.*;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.Set;

@Data
@Entity
@Table(name = "tokens")
@ToString(exclude = "users")
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
@Proxy(lazy = false)
public class Token {

    @Id
    @Column(name = "id", unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "token", nullable = false, unique = true, updatable = false,
            length = DeviceAccessService.SecureTokenGenerator.SECURE_TOKEN_LENGTH)
    private String token;

    @OneToMany(mappedBy = "token", fetch = FetchType.EAGER)
    private Set<TelegramUser> users;
}
