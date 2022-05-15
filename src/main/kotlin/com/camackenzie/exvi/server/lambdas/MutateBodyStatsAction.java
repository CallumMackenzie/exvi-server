package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class MutateBodyStatsAction implements LambdaAction<GenericDataRequest, GenericDataResult> {
    @Override
    public GenericDataResult enact(@NotNull RequestBodyHandler context, @NotNull GenericDataRequest in) {
        DocumentDatabase database = context.getResourceManager().getDatabase();

        if (in instanceof GetBodyStatsRequest) {
            var request = (GetBodyStatsRequest) in;
            var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            return new GetBodyStatsResponse(user.getBodyStats());
        } else if (in instanceof SetBodyStatsRequest) {
            var request = (SetBodyStatsRequest) in;
            var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.setBodyStats(request.getBodyStats());
            return new NoneResult();
        }

        throw new ApiException(400, "Not a mutate body stats request");
    }
}
