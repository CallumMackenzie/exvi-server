package com.camackenzie.exvi.server.util;

import org.jetbrains.annotations.NotNull;

public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(int code, @NotNull String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}
