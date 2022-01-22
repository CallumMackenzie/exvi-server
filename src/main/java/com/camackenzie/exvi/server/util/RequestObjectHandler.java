/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * @author callum
 */
public abstract class RequestObjectHandler<IN, OUT> extends RequestStreamHandlerWrapper {

    private final Gson gson = new Gson();

    @Override
    public void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx)
            throws IOException {
        this.getLogger().log("handleRequestWrapped");
        APIRequest<IN> request = gson.fromJson(bf, APIRequest.class);
        APIResult<OUT> response = this.handleObjectRequest(request, ctx);
        pw.write(gson.toJson(response));
    }

    public abstract APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context);

    public final Gson getGson() {
        return this.gson;
    }

}
