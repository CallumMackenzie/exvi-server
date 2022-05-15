package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.core.api.WorkoutPutRequest;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class WorkoutPutRequestAction implements LambdaAction<WorkoutPutRequest, NoneResult> {
    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull WorkoutPutRequest request) {
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
        user.addWorkouts(request.getWorkouts());
        return new NoneResult();
    }
}
