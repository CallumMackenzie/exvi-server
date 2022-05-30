package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.GetFriendWorkouts;
import com.camackenzie.exvi.core.api.RemoteWorkoutResponse;
import com.camackenzie.exvi.core.model.RemoteWorkout;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class GetFriendWorkoutsAction implements LambdaAction<GetFriendWorkouts, RemoteWorkoutResponse> {
    @Override
    public RemoteWorkoutResponse enact(@NotNull RequestBodyHandler context, @NotNull GetFriendWorkouts req) {
        // Retrieve resources
        var database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, req.getUsername(), req.getAccessKey());
        var friend = UserDataEntry.ensureUserHasData(database, req.getFriend());

        // Ensure users are friends
        if (!user.isFriendsWith(friend)) throw new ApiException(400, "User is not a friend");

        // Get user's public workouts
        var remoteWorkouts = Arrays.stream(friend.getWorkouts())
                // Remove private workouts
                .map(it -> it.getPublic() ? it : null)
                .filter(Objects::nonNull)
                // Map workouts to remote ones
                .map(it -> new RemoteWorkout(new EncodedStringCache(it.getName()),
                        it.getId(),
                        req.getFriend()))
                .toArray(RemoteWorkout[]::new);
        // Return result
        return new RemoteWorkoutResponse(remoteWorkouts);
    }
}
