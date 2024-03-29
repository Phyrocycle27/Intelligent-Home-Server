package com.example.smarthome.server.entity.signal;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode(of = {"outputId"}, callSuper = false)
abstract class Signal{

    @Getter
    private Integer outputId;
}
