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
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
    private final Class<IN> inClass;
    @NotNull
    private final Eson eson;
    private String rawRequest, rawBody;

    public RequestObjectHandler(@NotNull Class<IN> inClass) {
        this.inClass = inClass;
        this.eson = new Eson();
    }

    public final String getRawRequest() {
        return rawRequest;
    }

    public final String getRawRequestBody() {
        return rawBody;
    }

    public final <Z> Z getRequestBodyAs(@NotNull Class<Z> cls) {
        return this.getGson().fromJson(this.getRawRequestBody(), cls);
    }

    @Override
    public void handleRequestWrapped(@NotNull BufferedReader bf,
                                     @NotNull PrintWriter pw,
                                     @NotNull Context ctx) throws IOException {
        Gson gson = eson.getGson();
        // Get raw request as string
        var encoded = gson.fromJson(bf, EncodedStringCache.class);
        getLogger().log("Encoded request " + encoded.toJson());
        rawRequest = encoded.get();
        // Log raw request
        ctx.getLogger().log("Raw request: " + rawRequest);
        // Parse request to json
        JsonObject requestObject = JsonParser.parseString(rawRequest).getAsJsonObject();
        JsonElement requestBodyObject = requestObject.get("body");

        APIResult<String> strResponse;
        try {
            IN requestBody = null;
            if (requestBodyObject.isJsonPrimitive()) {
                if (requestBodyObject.getAsJsonPrimitive().isString()) {
                    rawBody = requestBodyObject.getAsString();
                    requestBody = gson.fromJson(rawBody, this.inClass);
                }
            } else if (requestBodyObject.isJsonObject()) {
                rawBody = gson.toJson(requestBodyObject);
                requestBody = gson.fromJson(requestBodyObject, this.inClass);
            }
            if (requestBody == null) {
                throw new ApiException(400, "Cannot parse request body");
            }
            HashMap<String, String> headers = gson.fromJson(requestObject.get("headers"), HashMap.class);
            APIRequest<IN> req = new APIRequest("",
                    requestBody,
                    headers);
            APIResult<OUT> response = this.handleObjectRequest(req, ctx);
            strResponse = new APIResult<>(response.getStatusCode(),
                    gson.toJson(response.getBody()),
                    response.getHeaders());
        } catch (ApiException e) {
            strResponse = new APIResult(e.getCode(), e.getMessage(), APIRequest.jsonHeaders());
        } catch (Throwable e) {
            strResponse = new APIResult<>(500, "Internal server error.", APIRequest.jsonHeaders());
        }

        String finalResponse = gson.toJson(strResponse);
        ctx.getLogger().log("Response: " + finalResponse);
        pw.write(new EncodedStringCache(finalResponse).toJson());
    }

    @NotNull
    public abstract APIResult<OUT> handleObjectRequest(@NotNull APIRequest<IN> in, @NotNull Context context);

    @NotNull
    public final Gson getGson() {
        return this.eson.getGson();
    }

}
