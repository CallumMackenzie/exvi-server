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

import java.util.HashMap;

/**
 * @author callum
 */
public abstract class RequestBodyHandler<IN extends SelfSerializable, OUT extends SelfSerializable>
        extends RequestObjectHandler<IN, OUT> {

    public RequestBodyHandler(Class<IN> inClass) {
        super(inClass);
    }

    @Override
    public APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context) {
        OUT out = this.handleBodyRequest(in.getBody(), context);
        return new APIResult<OUT>(200,
                out,
                APIRequest.jsonHeaders());
    }

    public abstract OUT handleBodyRequest(IN in, Context context);

}
