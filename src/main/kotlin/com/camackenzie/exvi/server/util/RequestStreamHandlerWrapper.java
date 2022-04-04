/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import io.github.aakira.napier.Antilog;
import io.github.aakira.napier.LogLevel;
import io.github.aakira.napier.Napier;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public abstract class RequestStreamHandlerWrapper implements RequestStreamHandler {

    private AWSResourceManager resourceManager;

    @Override
    public final void handleRequest(@NotNull InputStream is,
                                    @NotNull OutputStream os,
                                    @NotNull Context ctx) throws IOException {
        try {
            // Set up resource manager
            resourceManager = AWSResourceManager.get(ctx);
            
            // Get lambda logger
            var lambdaLogger = ctx.getLogger();

            // Set up logging
            getExviLogger().base(new Antilog() {
                Map<LogLevel, String> logLevelStringMap = new HashMap<>() {{
                    put(LogLevel.VERBOSE, "[VERBOSE]");
                    put(LogLevel.DEBUG, "[DEBUG]");
                    put(LogLevel.INFO, "[INFO]");
                    put(LogLevel.WARNING, "[WARN]");
                    put(LogLevel.ERROR, "[ERROR]");
                    put(LogLevel.ASSERT, "[ASSERT]");
                }};

                @Override
                protected void performLog(@NotNull LogLevel logLevel, @Nullable String tag,
                                          @Nullable Throwable throwable, @Nullable String message) {
                    StringBuilder log = new StringBuilder()
                            .append(logLevelStringMap.get(logLevel)).append(" ");
                    if (tag != null) {
                        log.append(tag).append(": ");
                    }
                    if (message != null) {
                        log.append(message);
                    }
                    if (throwable != null) {
                        log.append("\n\t").append(Arrays.stream(throwable.getStackTrace()).map(it -> it.toString())
                                .collect(Collectors.joining("\n\t\t")));
                    }
                    lambdaLogger.log(log.toString());
                }
            });

            // Set up readers/writers
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8)));

            // Handle request
            this.handleRequestWrapped(reader, writer, resourceManager);

            if (writer.checkError()) {
                getLogger().e("Writer error", null, "ROOT_HANDLER");
            }
            reader.close();
            writer.close();
        } catch (Throwable t) {
            getLogger().e("Uncaught fatal response", t, "ROOT_HANDLER");
        }
    }

    @NotNull
    public final Napier getLogger() {
        return getExviLogger();
    }

    @NotNull
    public final AWSResourceManager getResourceManager() {
        return resourceManager;
    }

    public abstract void handleRequestWrapped(@NotNull BufferedReader bf,
                                              @NotNull PrintWriter pw,
                                              @NotNull AWSResourceManager resourceManager) throws IOException;

}
