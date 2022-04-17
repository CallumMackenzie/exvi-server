/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.util.SelfSerializable;
import kotlinx.serialization.KSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public abstract class RequestBodyHandler<IN extends SelfSerializable, OUT extends SelfSerializable>
        extends RequestObjectHandler<IN, OUT> {

    public RequestBodyHandler(@NotNull KSerializer<IN> inSerializer,
                              @NotNull KSerializer<OUT> outSerializer) {
        super(inSerializer, outSerializer);
    }

    @Override
    @NotNull
    public APIResult<OUT> handleObjectRequest(@NotNull APIRequest<IN> in, @NotNull AWSResourceManager resourceManager) {
        OUT out = this.handleBodyRequest(in.getBody());
        return new APIResult<>(200,
                out,
                APIRequest.jsonHeaders());
    }

    @NotNull
    public abstract OUT handleBodyRequest(@NotNull IN in);

}
