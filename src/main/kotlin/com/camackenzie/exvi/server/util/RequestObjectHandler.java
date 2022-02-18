/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.api.GenericDataRequest;
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
    private final Eson eson;
    private String rawRequest, rawBody;

    public RequestObjectHandler(Class<IN> inClass) {
        this.inClass = inClass;
        this.eson = new Eson();
    }

    public final String getRawRequest() {
        return rawRequest;
    }

    public final String getRawRequestBody() {
        return rawBody;
    }

    public final <Z> Z getRequestBodyAs(Class<Z> cls) {
        return this.getGson().fromJson(this.getRawRequestBody(), cls);
    }

    @Override
    public void handleRequestWrapped(BufferedReader bf, PrintWriter pw, Context ctx)
            throws IOException {
        Gson gson = eson.getGson();
        // Get raw request as string
        rawRequest = bf.lines().collect(Collectors.joining(""));
        // Log raw request
        ctx.getLogger().log("Raw request: " + rawRequest);
        // Parse request to json
        JsonObject requestObject = JsonParser.parseString(rawRequest).getAsJsonObject();
        JsonElement requestBodyObject = requestObject.get("body");
        rawBody = gson.toJson(requestBodyObject);

        IN requestBody = null;
        if (requestBodyObject.isJsonPrimitive()) {
            if (requestBodyObject.getAsJsonPrimitive().isString()) {
                requestBody = gson.fromJson(requestBodyObject.getAsString(), this.inClass);
            }
        } else if (requestBodyObject.isJsonObject()) {
            requestBody = gson.fromJson(requestBodyObject, this.inClass);
        }
        if (requestBody == null) {
            pw.write(gson.toJson(new APIResult(400, "Cannot parse request body.",
                    new HashMap<>())));
            return;
        }

        HashMap<String, String> headers = gson.fromJson(requestObject.get("headers"), HashMap.class);
        APIRequest<IN> req = new APIRequest("",
                requestBody,
                headers);

        APIResult<String> strResponse;
        try {
            APIResult<OUT> response = this.handleObjectRequest(req, ctx);
            strResponse = new APIResult<>(response.getStatusCode(),
                    gson.toJson(response.getBody()),
                    response.getHeaders());
        } catch (ApiException e) {
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
        return this.eson.getGson();
    }

}
