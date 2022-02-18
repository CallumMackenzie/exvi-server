package com.camackenzie.exvi.server.util;

// T class should be called RequestException, but for some reason IntelliJ doesn't like that name...
public class ApiException extends RuntimeException {

    private final int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}