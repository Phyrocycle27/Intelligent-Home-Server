package com.example.smarthome.server.entity.signal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(of = {"digitalState"}, callSuper = true)
public class DigitalState extends Signal {

    @Getter
    private Boolean digitalState;

    public DigitalState(Integer outputId, Boolean digitalState) {
        super(outputId);
        this.digitalState = digitalState;
    }
}