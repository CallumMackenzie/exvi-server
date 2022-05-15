/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.api.APIResultKt;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.CryptographyUtils;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.SelfSerializable;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.stream.Collectors;

import static kotlinx.serialization.json.JsonElementKt.getJsonObject;
import static kotlinx.serialization.json.JsonElementKt.getJsonPrimitive;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public abstract class RequestObjectHandler<IN extends SelfSerializable, OUT extends SelfSerializable>
        extends RequestStreamHandlerWrapper {

    @NotNull
    private final KSerializer<IN> inSerializer;

    @NotNull
    private final KSerializer<OUT> outSerializer;

    public RequestObjectHandler(@NotNull KSerializer<IN> inSerializer,
                                @NotNull KSerializer<OUT> outSerializer) {
        this.inSerializer = inSerializer;
        this.outSerializer = outSerializer;
    }

    @Override
    protected void handleRequestWrapped(@NotNull BufferedReader bf,
                                        @NotNull PrintWriter pw,
                                        @NotNull AWSResourceManager resourceManager) {
        APIResult<String> strResponse;
        try {
            // Get raw request as string
            final var rawRequest = bf.lines().collect(Collectors.joining(""));
            // Parse request to json
            JsonObject requestObject = getJsonObject(ExviSerializer.getSerializer().parseToJsonElement(rawRequest));

            JsonElement requestBodyObject = requestObject.get("body");

            // Dynamically parse request to input type
            IN requestBody = null;
            if (requestBodyObject != null) {
                if (requestBodyObject instanceof JsonPrimitive) {
                    final var primitive = getJsonPrimitive(requestBodyObject);
                    if (primitive.isString()) {
                        requestBody = ExviSerializer
                                .fromJson(this.inSerializer, EncodedStringCache.fromEncoded(primitive.getContent()).get());
                    }
                } else if (requestBodyObject instanceof JsonObject) {
                    requestBody = ExviSerializer.fromJsonElement(this.inSerializer, requestBodyObject);
                }
            }

            // Ensure a valid request body has been parsed
            if (requestBody == null) {
                throw new ApiException(400, "Cannot parse request body");
            }
//            getLogger().i("Request body is valid", null, "OBJECT_HANDLER");
//            getLogger().v("Body: " + rawRequest, null, "OBJECT_HANDLER");

            // Pass the headers to a new api request object with the proper body format
            // Get request headers if they are present
            final var headersJsonElement = requestObject.get("headers");
            final HashMap headers;
            if (headersJsonElement == null) headers = new HashMap();
            else headers = new HashMap(getJsonObject(headersJsonElement));
            // Construct as an APIRequest object
            APIRequest<IN> req = new APIRequest<>("",
                    requestBody,
                    headers);
            // Call inheriting class for response
            APIResult<OUT> response = this.handleObjectRequest(req, resourceManager);

            // Convert response to JSON
            strResponse = new APIResult<>(response.getStatusCode(),
                    ExviSerializer.toJson(this.outSerializer, response.getBody()),
                    response.getHeaders());
//            getLogger().i("Response formed", null, "OBJECT_HANDLER");
        } catch (ApiException e) {
            getLogger().w("Returning API exception", e, "OBJECT_HANDLER");
            strResponse = new APIResult<>(e.getCode(), e.getMessage(), APIRequest.jsonHeaders());
        } catch (Throwable e) {
            getLogger().e("Fatal uncaught exception", e, "OBJECT_HANDLER");
            strResponse = new APIResult<>(500, "Internal server error", APIRequest.jsonHeaders());
        }

        // Encode & write response
//        getLogger().v("Response (code " + strResponse.getStatusCode() + "): " + strResponse.getBody(), null, "OBJECT_HANDLER");
        strResponse.setBody(CryptographyUtils.encodeString(strResponse.getBody()));
        pw.write(APIResultKt.toJson(strResponse));
    }

    @NotNull
    protected abstract APIResult<OUT> handleObjectRequest(@NotNull APIRequest<IN> in, @NotNull AWSResourceManager resourceManager);

}
