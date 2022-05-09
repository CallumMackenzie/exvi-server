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
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataRequestAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    private static final int LATEST_COMPATIBLE_VERSION = 3;

    public UserDataRequestAction() {
        super(GenericDataRequest.Companion.serializer(), GenericDataResult.Companion.serializer());
    }

    @Override
    @NotNull
    protected GenericDataResult handleBodyRequest(@NotNull GenericDataRequest in) {

        // TODO: Refactor database retrieval out

        // Determine behaviour based on request
        if (in instanceof WorkoutListRequest) {
            var request = (WorkoutListRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            switch (request.getType()) {
                case ListAllTemplates:
                    return new WorkoutListResult(user.getWorkouts());
                case ListAllActive:
                    return new ActiveWorkoutListResult(user.getActiveWorkouts());
            }
        } else if (in instanceof WorkoutPutRequest) {
            var request = (WorkoutPutRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.addWorkouts(request.getWorkouts());
            return new NoneResult();
        } else if (in instanceof ActiveWorkoutPutRequest) {
            var request = (ActiveWorkoutPutRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.addActiveWorkouts(request.getWorkouts());
            return new NoneResult();
        } else if (in instanceof DeleteWorkoutsRequest) {
            var request = (DeleteWorkoutsRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            switch (request.getWorkoutType()) {
                case Workout:
                    user.removeWorkouts(request.getWorkoutIds());
                    break;
                case ActiveWorkout:
                    user.removeActiveWorkouts(request.getWorkoutIds());
                    break;
            }
            return new NoneResult();
        } else if (in instanceof GetBodyStatsRequest) {
            var request = (GetBodyStatsRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            return new GetBodyStatsResponse(user.getBodyStats());
        } else if (in instanceof SetBodyStatsRequest) {
            var request = (SetBodyStatsRequest) in;
            DocumentDatabase database = getResourceManager().getDatabase();
            var user = ensureUserValidity(database, request.getUsername(), request.getAccessKey());
            user.setBodyStats(request.getBodyStats());
            return new NoneResult();
        } else if (in instanceof CompatibleVersionRequest)
            return new BooleanResult(((CompatibleVersionRequest) in).getVersion() >= LATEST_COMPATIBLE_VERSION);
        else if (in instanceof VerificationRequest)
            return VerificationAction.enact((VerificationRequest) in, this);
        else if (in instanceof AccountCreationRequest)
            return SignUpAction.enact((AccountCreationRequest) in, this);
        else if (in instanceof LoginRequest)
            return LoginAction.enact((LoginRequest) in, this);
        else if (in instanceof RetrieveSaltRequest)
            return RetrieveSaltAction.enact((RetrieveSaltRequest) in, this);

        throw new ApiException(400, "Could not recognize requester");
    }

    @NotNull
    private UserDataEntry ensureUserValidity(@NotNull DocumentDatabase database,
                                             @NotNull EncodedStringCache username,
                                             @NotNull EncodedStringCache accessKey) {
        UserLoginEntry.ensureAccessKeyValid(database, username, accessKey);
        return UserDataEntry.ensureUserHasData(database, username);
    }
}