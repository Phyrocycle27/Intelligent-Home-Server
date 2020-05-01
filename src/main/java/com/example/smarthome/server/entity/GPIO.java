package com.example.smarthome.server.entity;

import com.example.smarthome.server.entity.signal.SignalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(of = {"id"})
@AllArgsConstructor
@NoArgsConstructor
public class GPIO {

    private Integer id;

    private Integer gpio;

    private SignalType type;

    private GPIOMode mode;
}