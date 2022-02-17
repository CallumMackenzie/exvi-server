/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author callum
 */
public abstract class RequestObjectHandler<IN extends SelfSerializable, OUT extends SelfSerializable>
        extends RequestStreamHandlerWrapper {

    private final Class<IN> inClass;
    private final Gson gson = new Gson();
    private String rawRequest;

    public RequestObjectHandler(Class<IN> inClass) {
        this.inClass = inClass;
    }

    public final String getRawRequest() {
        return rawRequest;
    }

    @Override
    public void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx)
            throws IOException {
        String request = bf.lines().collect(Collectors.joining(""));
        rawRequest = request;
        JsonObject requestRaw = JsonParser.parseString(request).getAsJsonObject();
        JsonElement jsonElem = gson.toJsonTree(requestRaw.getAsJsonObject("body"));

        ctx.getLogger().log("REQUEST: " + request);

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

        HashMap<String, String> headers = gson.fromJson(requestRaw.get("headers"), HashMap.class);
        APIRequest<IN> req = new APIRequest(requestRaw.get("endpoint").getAsString(),
                requestBody,
                headers);

        APIResult<String> strResponse;
        try {
            APIResult<OUT> response = this.handleObjectRequest(req, ctx);
            strResponse = new APIResult<>(response.getStatusCode(),
                    this.gson.toJson(response.getBody()),
                    response.getHeaders());
        } catch (RequestException e) {
            strResponse = new APIResult<String>(e.getCode(),
                    e.getMessage(),
                    APIRequest.jsonHeaders());
        }

        String finalResponse = gson.toJson(strResponse);
        ctx.getLogger().log("Response: " + finalResponse);
        pw.write(finalResponse);
    }

    public abstract APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context);

    public final Gson getGson() {
        return this.gson;
    }

}
