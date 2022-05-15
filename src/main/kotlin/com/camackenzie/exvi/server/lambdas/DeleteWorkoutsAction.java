package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.DeleteWorkoutsRequest;
import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class DeleteWorkoutsAction implements LambdaAction<DeleteWorkoutsRequest, NoneResult> {
    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull DeleteWorkoutsRequest request) {
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
        switch (request.getWorkoutType()) {
            case Workout:
                user.removeWorkouts(request.getWorkoutIds());
                break;
            case ActiveWorkout:
                user.removeActiveWorkouts(request.getWorkoutIds());
                break;
        }
        return new NoneResult();
    }
}
