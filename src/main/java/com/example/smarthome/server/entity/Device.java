package com.example.smarthome.server.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Device {

    private Integer id;

    private String name;

    private Boolean reverse;

    private LocalDateTime creationDate;

    private GPIO gpio;
}
