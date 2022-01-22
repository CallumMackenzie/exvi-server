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
import com.google.gson.JsonElement;
import com.google.gson.internal.LinkedTreeMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 *
 * @author callum
 */
public abstract class RequestObjectHandler<IN, OUT> extends RequestStreamHandlerWrapper {

    private final Class<IN> inClass;
    private final Gson gson = new Gson();

    public RequestObjectHandler(Class<IN> inClass) {
        this.inClass = inClass;
    }

    @Override
    public void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx)
            throws IOException {
        APIRequest<LinkedTreeMap> requestRaw = gson.fromJson(bf, APIRequest.class);
        JsonElement jsonElem = gson.toJsonTree(requestRaw.getBody());

        IN requestBody = null;
        if (jsonElem.isJsonPrimitive()) {
            if (jsonElem.getAsJsonPrimitive().isString()) {
                requestBody = this.gson.fromJson(jsonElem.getAsString(), this.inClass);
            }
        } else if (jsonElem.isJsonObject()) {
            requestBody = this.gson.fromJson(jsonElem, this.inClass);
        }
        if (requestBody == null) {
            pw.write(this.gson.toJson(new APIResult(400, "Cannot parse request body.",
                    new HashMap<>())));
            return;
        }

        APIRequest<IN> req = new APIRequest(requestRaw.getEndpoint(),
                requestBody,
                requestRaw.getHeaders());

        APIResult<OUT> response = this.handleObjectRequest(req, ctx);
        APIResult<String> strResponse = new APIResult<>(response.getStatusCode(),
                this.gson.toJson(response.getBody()),
                response.getHeaders());
        pw.write(this.gson.toJson(strResponse));
    }

    public abstract APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context);

    public final Gson getGson() {
        return this.gson;
    }

}
