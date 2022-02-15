/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 *
 * @author callum
 */
public abstract class RequestStreamHandlerWrapper implements RequestStreamHandler {

    private LambdaLogger logger;

    @Override
    public final void handleRequest(InputStream is, OutputStream os, Context ctx) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, Charset.forName("UTF-8"))));
        this.logger = ctx.getLogger();

        this.handleRequestWrapped(reader, writer, ctx);

        if (writer.checkError()) {
            this.getLogger().log("WRITER ERROR OCCURED");
        }
        reader.close();
        writer.close();
    }

    public final LambdaLogger getLogger() {
        return this.logger;
    }

    public abstract void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx) throws IOException;

}
