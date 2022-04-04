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
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public abstract class RequestBodyHandler<IN extends SelfSerializable, OUT extends SelfSerializable>
        extends RequestObjectHandler<IN, OUT> {

    public RequestBodyHandler(@NotNull Class<IN> inClass) {
        super(inClass);
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
