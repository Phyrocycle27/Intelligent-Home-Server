package com.example.smarthome.server.entity.signal;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(of = {"pwmSignal"}, callSuper = true)
public class PwmSignal extends Signal {

    @Getter
    private Integer pwmSignal;

    public PwmSignal(Integer outputId, Integer pwmSignal) {
        super(outputId);
        this.pwmSignal = pwmSignal;
    }
}
