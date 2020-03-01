package com.example.smarthome.server.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = {"outputId"})
public class Output {

    private Integer outputId;

    private String name;

    private Integer gpio;

    private Boolean reverse;

    private LocalDateTime creationDate;

    private String type;
}