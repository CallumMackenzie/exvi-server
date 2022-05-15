package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.ActiveWorkoutPutRequest;
import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class ActiveWorkoutPutRequestAction implements LambdaAction<ActiveWorkoutPutRequest, NoneResult> {
    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull ActiveWorkoutPutRequest request) {
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
        user.addActiveWorkouts(request.getWorkouts());
        return new NoneResult();
    }
}
