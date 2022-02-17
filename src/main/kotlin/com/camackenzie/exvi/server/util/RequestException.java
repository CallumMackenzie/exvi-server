package com.camackenzie.exvi.server.util;

public class RequestException extends RuntimeException {

    private final int code;

    public RequestException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

}