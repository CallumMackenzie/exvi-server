/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataRequestAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    public UserDataRequestAction() {
        super(GenericDataRequest.class);
    }

    @Override
    @NotNull
    public GenericDataResult handleBodyRequest(@NotNull GenericDataRequest in) {
        // Preconditions
        if (in.getRequester().get().isBlank()) {
            throw new ApiException(400, "No requester provided");
        }

        // Retrieve resources
        DocumentDatabase database = getResourceManager().getDatabase();
        this.getLogger().i("Requester: " + in.getRequester().get(), null, "DATA_LAMBDA");

        // Determine behaviour based on request
        switch (in.getRequester().get()) {
            case WorkoutListRequest.uid: {
                var request = this.getRequestBodyAs(WorkoutListRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                switch (request.getType()) {
                    case ListAllTemplates:
                        return new WorkoutListResult(user.getWorkouts());
                    case ListAllActive:
                        return new ActiveWorkoutListResult(user.getActiveWorkouts());
                }
            }
            case WorkoutPutRequest.uid: {
                var request = this.getRequestBodyAs(WorkoutPutRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                user.addWorkouts(request.getWorkouts());
                return NoneResult.INSTANCE;
            }
            case ActiveWorkoutPutRequest.uid: {
                var request = this.getRequestBodyAs(ActiveWorkoutPutRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                user.addActiveWorkouts(request.getWorkouts());
                return NoneResult.INSTANCE;
            }
            case DeleteWorkoutsRequest.uid: {
                var request = this.getRequestBodyAs(DeleteWorkoutsRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                switch (request.getWorkoutType()) {
                    case Workout:
                        user.removeWorkouts(request.getWorkoutIds());
                        break;
                    case ActiveWorkout:
                        user.removeActiveWorkouts(request.getWorkoutIds());
                        break;
                }
                return NoneResult.INSTANCE;
            }
            case GetBodyStatsRequest.uid: {
                var request = this.getRequestBodyAs(GetBodyStatsRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                return new GetBodyStatsResponse(user.getBodyStats());
            }
            case SetBodyStatsRequest.uid: {
                var request = this.getRequestBodyAs(SetBodyStatsRequest.class);
                var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                user.setBodyStats(request.getBodyStats());
                return NoneResult.INSTANCE;
            }
            case CompatibleVersionRequest.uid: {
                var request = this.getRequestBodyAs(CompatibleVersionRequest.class);
                return new BooleanResult(true);
            }
            default:
                throw new ApiException(400, "Could not recognize requester");
        }
    }

    @NotNull
    private UserDataEntry ensureUserValidity(@NotNull DocumentDatabase database,
                                             @NotNull EncodedStringCache username,
                                             @NotNull EncodedStringCache accessKey) {
        UserLoginEntry.ensureAccessKeyValid(database, username, accessKey);
        return UserDataEntry.ensureUserHasData(database, username);
    }
}