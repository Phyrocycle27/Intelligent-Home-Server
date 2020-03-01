package com.example.smarthome.server.telegram.objects;

public enum UserRole {
    CREATOR("creator", 2),
    ADMIN("admin", 1),
    USER("user", 0);

    private String name;
    private int code;

    UserRole(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public int getCode() {
        return code;
    }
}
