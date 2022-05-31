package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.BooleanResult;
import com.camackenzie.exvi.core.api.CompatibleVersionRequest;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

// Action to verify client version compatibility
public class VerifyVersionAction implements LambdaAction<CompatibleVersionRequest, BooleanResult> {

    private static final int LATEST_COMPATIBLE_VERSION = 8;

    @Override
    public BooleanResult enact(@NotNull RequestBodyHandler context, @NotNull CompatibleVersionRequest request) {
        return new BooleanResult(request.getVersion() >= LATEST_COMPATIBLE_VERSION);
    }
}
