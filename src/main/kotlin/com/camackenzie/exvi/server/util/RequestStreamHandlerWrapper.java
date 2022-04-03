/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

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
            resourceManager = AWSResourceManager.get(ctx);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8)));

            this.handleRequestWrapped(reader, writer, resourceManager);

            if (writer.checkError()) {
                this.getLogger().log("WRITER ERROR OCCURED");
            }
            reader.close();
            writer.close();
        } catch (Throwable t) {
            getLogger().log("Uncaught fatal response exception: " + t);
        }
    }

    @NotNull
    public final LambdaLogger getLogger() {
        return resourceManager.getLogger();
    }

    public abstract void handleRequestWrapped(@NotNull BufferedReader bf,
                                              @NotNull PrintWriter pw,
                                              @NotNull AWSResourceManager resourceManager) throws IOException;

}
