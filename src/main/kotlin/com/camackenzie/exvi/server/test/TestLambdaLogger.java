package com.camackenzie.exvi.server.test;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;

public class TestLambdaLogger implements LambdaLogger {
    @Override
    public void log(String s) {
        getExviLogger().i(s, null, null);
    }

    @Override
    public void log(byte[] bytes) {
        getExviLogger().i(bytes.toString(), null, null);
    }
}
