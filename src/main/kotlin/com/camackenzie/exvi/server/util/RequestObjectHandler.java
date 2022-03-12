/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
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
        APIResult<String> strResponse;
        try {
            // Get raw request as string
            String encodedRequest = bf.lines().collect(Collectors.joining(""));
            ctx.getLogger().log("Encoded request: " + encodedRequest);
            EncodedStringCache encoded = EncodedStringCache.fromEncoded(encodedRequest);
            rawRequest = encoded.get();
            // Log raw request
            ctx.getLogger().log("Raw request: " + rawRequest);
            // Parse request to json
            JsonObject requestObject = JsonParser.parseString(rawRequest).getAsJsonObject();
            JsonElement requestBodyObject = requestObject.get("body");

            // Parse request from either json or json string format
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
            // Ensure a valid request body has been parsed
            if (requestBody == null) {
                throw new ApiException(400, "Cannot parse request body");
            }
            // Pass the headers to a new api request object with the proper body format
            HashMap<String, String> headers = gson.fromJson(requestObject.get("headers"), HashMap.class);
            APIRequest<IN> req = new APIRequest("",
                    requestBody,
                    headers);
            // Call inheriting class for response
            APIResult<OUT> response = this.handleObjectRequest(req, ctx);
            // Get JSON response
            strResponse = new APIResult<>(response.getStatusCode(),
                    gson.toJson(response.getBody()),
                    response.getHeaders());
        } catch (ApiException e) {
            strResponse = new APIResult(e.getCode(), e.getMessage(), APIRequest.jsonHeaders());
        } catch (Throwable e) {
            getLogger().log("Uncaught Exception: " + e + " : Stack Trace: " + Arrays.asList(e.getStackTrace()));
            strResponse = new APIResult<>(500, "Internal server error.", APIRequest.jsonHeaders());
        }

        // Encode & return
        String finalResponse = gson.toJson(strResponse);
        ctx.getLogger().log("Response: " + finalResponse);
        pw.write(new EncodedStringCache(finalResponse).getEncoded());
    }

    @NotNull
    public abstract APIResult<OUT> handleObjectRequest(@NotNull APIRequest<IN> in, @NotNull Context context);

    @NotNull
    public final Gson getGson() {
        return this.eson.getGson();
    }

}
