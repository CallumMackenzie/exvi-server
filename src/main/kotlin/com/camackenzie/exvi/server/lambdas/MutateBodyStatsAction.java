package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

// Action to get/set user body stats
public class MutateBodyStatsAction<IN extends GenericDataRequest & ValidatedUserRequest> implements LambdaAction<IN, GenericDataResult> {
    @Override
    public GenericDataResult enact(@NotNull RequestBodyHandler context, @NotNull IN in) {
        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, in.getUsername(), in.getAccessKey());

        // Determine request behaviour
        if (in instanceof GetBodyStatsRequest) {
            return new GetBodyStatsResponse(user.getBodyStats());
        } else if (in instanceof SetBodyStatsRequest) {
            var request = (SetBodyStatsRequest) in;
            user.setBodyStats(request.getBodyStats());
            return new NoneResult();
        }

        throw new ApiException(400, "Not a mutate body stats request");
    }
}
