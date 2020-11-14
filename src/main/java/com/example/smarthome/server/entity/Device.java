package com.example.smarthome.server.entity;

import lombok.*;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Device {

    private Long id;

    private String name;

    private String description;

    private Boolean signalInversion;

    private Long areaId;

    private LocalDateTime creationTimestamp;

    private LocalDateTime updateTimestamp;

    private GPIO gpio;
}
