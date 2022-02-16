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
import com.google.gson.*;

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
        // Get raw request as string
        rawRequest = bf.lines().collect(Collectors.joining(""));
        // Log raw request
        ctx.getLogger().log("Raw request: " + rawRequest);
        // Parse request to json
        JsonObject requestObject = JsonParser.parseString(rawRequest).getAsJsonObject();
        JsonElement requestBodyObject = requestObject.get("body");

        IN requestBody = null;
        if (requestBodyObject.isJsonPrimitive()) {
            if (requestBodyObject.getAsJsonPrimitive().isString()) {
                requestBody = this.gson.fromJson(requestBodyObject.getAsString(), this.inClass);
            }
        } else if (requestBodyObject.isJsonObject()) {
            requestBody = this.gson.fromJson(requestBodyObject, this.inClass);
        }
        if (requestBody == null) {
            pw.write(this.gson.toJson(new APIResult(400, "Cannot parse request body.",
                    new HashMap<>())));
            return;
        }

        HashMap<String, String> headers = gson.fromJson(requestObject.get("headers"), HashMap.class);
        APIRequest<IN> req = new APIRequest(requestObject.get("endpoint").getAsString(),
                requestBody,
                headers);
        APIResult<OUT> response = this.handleObjectRequest(req, ctx);
        APIResult<String> strResponse = new APIResult<>(response.getStatusCode(),
                this.gson.toJson(response.getBody()),
                response.getHeaders());

        String finalResponse = gson.toJson(strResponse);
        ctx.getLogger().log("Response: " + finalResponse);
        pw.write(finalResponse);
    }

    public abstract APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context);

    public final Gson getGson() {
        return this.gson;
    }

}
