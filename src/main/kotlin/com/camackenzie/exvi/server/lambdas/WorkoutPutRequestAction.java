package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

// Action to add workouts to a user's account
public class WorkoutPutRequestAction<IN extends GenericDataRequest & ValidatedUserRequest> implements LambdaAction<IN, NoneResult> {
    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull IN in) {
        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, in.getUsername(), in.getAccessKey());

        // Determine request behaviour
        if (in instanceof ActiveWorkoutPutRequest) {
            var request = (ActiveWorkoutPutRequest) in;
            user.addActiveWorkouts(request.getWorkouts());
        } else if (in instanceof WorkoutPutRequest) {
            var request = (WorkoutPutRequest) in;
            user.addWorkouts(request.getWorkouts());
        }

        return new NoneResult();
    }
}
