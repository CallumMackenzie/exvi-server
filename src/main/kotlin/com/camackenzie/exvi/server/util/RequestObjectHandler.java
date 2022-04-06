/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kotlinx.serialization.DeserializationStrategy;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author callum
 */
@SuppressWarnings("unused")
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

    public final <Z> Z getRequestBodyAs(@NotNull DeserializationStrategy<Z> deserializer) {
        return ExviSerializer.INSTANCE.fromJson(deserializer, rawBody);
    }

    @Override
    public void handleRequestWrapped(@NotNull BufferedReader bf,
                                     @NotNull PrintWriter pw,
                                     @NotNull AWSResourceManager resourceManager) {
        Gson gson = eson.getGson();
        APIResult<String> strResponse;
        try {
            // Get raw request as string
            rawRequest = bf.lines().collect(Collectors.joining(""));
            // Parse request to json
            JsonObject requestObject = JsonParser.parseString(rawRequest).getAsJsonObject();

            JsonElement requestBodyObject = requestObject.get("body");

            // Dynamically parse request to input type
            IN requestBody = null;
            if (requestBodyObject.isJsonPrimitive()) {
                if (requestBodyObject.getAsJsonPrimitive().isString()) {
                    rawBody = EncodedStringCache.fromEncoded(requestBodyObject.getAsString()).get();
                    requestBody = gson.fromJson(rawBody, this.inClass);
                }
            } else if (requestBodyObject.isJsonObject()) {
                rawBody = requestBodyObject.getAsJsonObject().toString();
                requestBody = gson.fromJson(rawBody, this.inClass);
            }

            // Ensure a valid request body has been parsed
            if (requestBody == null) {
                throw new ApiException(400, "Cannot parse request body");
            }
            getLogger().i("Request body is valid", null, "OBJECT_HANDLER");
            getLogger().v("Body: " + getRawRequestBody(), null, "OBJECT_HANDLER");

            // Pass the headers to a new api request object with the proper body format
            HashMap headers = gson.fromJson(requestObject.get("headers"), HashMap.class);
            if (headers == null) headers = new HashMap(); // Kotlin expects the headers variable to be not null
            APIRequest<IN> req = new APIRequest<>("",
                    requestBody,
                    headers);
            // Call inheriting class for response
            APIResult<OUT> response = this.handleObjectRequest(req, resourceManager);

            // Convert response to JSON
            strResponse = new APIResult<>(response.getStatusCode(),
                    gson.toJson(response.getBody()),
                    response.getHeaders());
            getLogger().i("Response formed", null, "OBJECT_HANDLER");
        } catch (ApiException e) {
            getLogger().w("Returning API exception", e, "OBJECT_HANDLER");
            strResponse = new APIResult<>(e.getCode(), e.getMessage(), APIRequest.jsonHeaders());
        } catch (Throwable e) {
            getLogger().e("Fatal uncaught exception", e, "OBJECT_HANDLER");
            strResponse = new APIResult<>(500, "Internal server error", APIRequest.jsonHeaders());
        }

        // Encode & write response
        getLogger().v("Response (code " + strResponse.getStatusCode() + "): " + strResponse.getBody(),
                null, "OBJECT_HANDLER");
        strResponse.setBody(CryptographyUtils.encodeString(strResponse.getBody()));
        pw.write(gson.toJson(strResponse));
    }

    @NotNull
    public abstract APIResult<OUT> handleObjectRequest(@NotNull APIRequest<IN> in, @NotNull AWSResourceManager resourceManager);

}
