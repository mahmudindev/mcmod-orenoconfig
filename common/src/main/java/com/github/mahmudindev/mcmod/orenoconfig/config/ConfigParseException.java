package com.github.mahmudindev.mcmod.orenoconfig.config;

public class ConfigParseException extends RuntimeException {
    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
