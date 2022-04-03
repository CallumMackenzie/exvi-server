package com.camackenzie.exvi.server.test;

import com.camackenzie.exvi.server.util.EmailClient;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;

public class TestEmailClient implements EmailClient {
    @Override
    public void sendEmail(@NotNull String sender, @NotNull String recipient, @NotNull String subject, @NotNull String htmlBody, @NotNull String plainTextBody) {
        getExviLogger().i("Sending email from " + sender + " to " + recipient + ": \n" +
                "\tSubject: " + subject
                + "\n\tHTML:\n\t\t" + htmlBody.lines().collect(Collectors.joining("\n\t\t"))
                + "\n\tPlainText:\n\t\t" + plainTextBody.lines().collect(Collectors.joining("\n\t\t")), null, null);
    }
}
