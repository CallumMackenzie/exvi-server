package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.api.GenericDataRequest;
import com.camackenzie.exvi.core.api.GenericDataResult;
import org.jetbrains.annotations.NotNull;

public interface LambdaAction<IN extends GenericDataRequest, OUT extends GenericDataResult> {
    OUT enact(@NotNull RequestBodyHandler context, @NotNull IN in);
}
