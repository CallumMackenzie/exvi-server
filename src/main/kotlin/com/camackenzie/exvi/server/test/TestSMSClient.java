package com.camackenzie.exvi.server.test;

import com.camackenzie.exvi.server.util.SMSClient;
import org.jetbrains.annotations.NotNull;

import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;

public class TestSMSClient implements SMSClient {
    @Override
    public void sendText(@NotNull String recipient, @NotNull String message) {
        getExviLogger().i("Sending sms message to " + recipient + ": " + message, null, null);
    }
}
