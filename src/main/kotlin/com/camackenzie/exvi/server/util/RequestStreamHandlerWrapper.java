/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.camackenzie.exvi.core.model.TimeUnit;
import com.camackenzie.exvi.core.model.UnitValue;
import io.github.aakira.napier.Antilog;
import io.github.aakira.napier.LogLevel;
import io.github.aakira.napier.Napier;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;
import static com.camackenzie.exvi.core.model.TimeUnitKt.formatToElapsedTime;

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
            // Record start of lambda
            UnitValue<TimeUnit> execStart = TimeUnit.now();

            // Set up resource manager
            resourceManager = AWSResourceManager.get(ctx);

            // Get lambda logger
            var lambdaLogger = ctx.getLogger();

            // Set up logging for AWS
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
                    // Get elapsed time since start of lambda
                    UnitValue<TimeUnit> timeDiff = TimeUnit.now().minus(execStart);

                    // Build final message
                    StringBuilder msg = new StringBuilder()
                            .append(logLevelStringMap.get(logLevel)).append(" ")
                            .append(formatToElapsedTime(timeDiff)).append(": ");
                    if (tag != null) msg.append(tag).append(": ");
                    if (message != null) msg.append(message);
                    if (throwable != null) msg.append("\n\t").append(throwable.getMessage()).append("\n\t")
                            .append(Arrays.stream(throwable.getStackTrace()).map(StackTraceElement::toString)
                                    .collect(Collectors.joining("\n\t\t")));

                    // Log the completed log
                    lambdaLogger.log(msg.toString());
                }
            });

            // Set up readers/writers
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8)));

            // Handle request
            this.handleRequestWrapped(reader, writer, resourceManager);

            // Check for writer error
            if (writer.checkError()) {
                getLogger().e("Writer error", null, "ROOT_HANDLER");
            }
            // Release reader & writer resources
            reader.close();
            writer.close();
        } catch (Throwable t) {
            getLogger().e("Uncaught fatal response", t, "ROOT_HANDLER");
        } finally {
            getLogger().takeLogarithm();
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

    protected abstract void handleRequestWrapped(@NotNull BufferedReader bf,
                                              @NotNull PrintWriter pw,
                                              @NotNull AWSResourceManager resourceManager) throws IOException;

}
