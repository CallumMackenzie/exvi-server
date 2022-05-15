package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.ActiveWorkoutPutRequest;
import com.camackenzie.exvi.core.api.GenericDataRequest;
import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.core.api.WorkoutPutRequest;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

// Action to add workouts to a user's account
public class WorkoutPutRequestAction implements LambdaAction<GenericDataRequest, NoneResult> {
    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull GenericDataRequest in) {
        // Retrieve resources
        DocumentDatabase database = context.getResourceManager().getDatabase();

        // Determine request behaviour
        if (in instanceof ActiveWorkoutPutRequest) {
            var request = (ActiveWorkoutPutRequest) in;
            var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.addActiveWorkouts(request.getWorkouts());
        } else if (in instanceof WorkoutPutRequest) {
            var request = (WorkoutPutRequest) in;
            var user = UserDataEntry.ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.addWorkouts(request.getWorkouts());
        }

        return new NoneResult();
    }
}
