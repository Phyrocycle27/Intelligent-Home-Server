package com.example.smarthome.server.entity;

import com.example.smarthome.server.entity.signal.SignalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GPIO {

    private Integer gpioPin;

    private SignalType signalType;

    private GPIOMode pinMode;
}