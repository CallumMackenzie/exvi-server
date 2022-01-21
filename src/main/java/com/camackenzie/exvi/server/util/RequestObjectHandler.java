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
public abstract class RequestObjectHandler<REQUEST, RESULT> extends RequestStreamHandlerWrapper {

    private final Gson gson = new Gson();

    @Override
    public void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx)
            throws IOException {
        APIRequest<REQUEST> request = gson.fromJson(bf, APIRequest.class);
        APIResult<RESULT> response = this.handleObjectRequest(request, ctx);
        pw.write(gson.toJson(response));
    }

    public abstract APIResult<RESULT> handleObjectRequest(APIRequest<REQUEST> in, Context context);

    public final Gson getGson() {
        return this.gson;
    }

}
