package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.ActiveWorkoutListResult;
import com.camackenzie.exvi.core.api.GenericDataResult;
import com.camackenzie.exvi.core.api.WorkoutListRequest;
import com.camackenzie.exvi.core.api.WorkoutListResult;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class WorkoutListRequestAction implements LambdaAction<WorkoutListRequest, GenericDataResult> {
    @Override
    public GenericDataResult enact(@NotNull RequestBodyHandler context, @NotNull WorkoutListRequest request) {
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
        switch (request.getType()) {
            case ListAllTemplates:
                return new WorkoutListResult(user.getWorkouts());
            case ListAllActive:
                return new ActiveWorkoutListResult(user.getActiveWorkouts());
        }
        throw new ApiException(400, "Improperly formed request");
    }
}
