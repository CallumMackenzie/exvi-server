/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import java.util.HashMap;

/**
 *
 * @author callum
 */
public abstract class RequestBodyHandler<IN, OUT> extends RequestObjectHandler<IN, OUT> {

    @Override
    public APIResult<OUT> handleObjectRequest(APIRequest<IN> in, Context context) {
        this.getLogger().log("handleObjectRequest");
        return new APIResult<OUT>(200,
                this.handleBodyRequest(in.getBody(), context),
                new HashMap<>()).withJsonHeader();
    }

    public abstract OUT handleBodyRequest(IN in, Context context);

}
