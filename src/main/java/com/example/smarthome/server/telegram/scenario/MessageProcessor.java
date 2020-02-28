package com.example.smarthome.server.telegram.scenario;

import com.example.smarthome.server.telegram.UserInstance;
import com.example.smarthome.server.telegram.objects.IncomingMessage;

@FunctionalInterface
public interface MessageProcessor {

    Object process(UserInstance user, IncomingMessage msg);
}
