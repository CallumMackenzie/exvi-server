/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.core.model.Workout;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import com.camackenzie.exvi.server.util.ApiException;

import java.util.Arrays;

/**
 * @author callum
 */
public class UserDataRequestAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    public UserDataRequestAction() {
        super(GenericDataRequest.class);
    }

    @Override
    public GenericDataResult handleBodyRequest(GenericDataRequest in, Context context) {
        // Preconditions
        if (in.getRequester().get().isBlank()) {
            throw new ApiException(400, "No requester provided");
        }

        // Retrieve resources
        AWSDynamoDB database = new AWSDynamoDB();
        this.getLogger().log("Requester: " + in.getRequester().get());

        switch (in.getRequester().get()) {
            case WorkoutListRequest.uid: {
                WorkoutListRequest request = this.getRequestBodyAs(WorkoutListRequest.class);
                ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                return this.getWorkoutList(database, request);
            }
            case WorkoutPutRequest.uid: {
                WorkoutPutRequest request = this.getRequestBodyAs(WorkoutPutRequest.class);
                ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                putWorkouts(database, request);
                return NoneResult.INSTANCE;
            }
            case DeleteWorkoutsRequest.uid: {
                DeleteWorkoutsRequest request = this.getRequestBodyAs(DeleteWorkoutsRequest.class);
                ensureUserValidity(database, request.getUsername(), request.getAccessKey());
                deleteWorkouts(database, request);
                return NoneResult.INSTANCE;
            }
            default:
                throw new ApiException(400, "Could not recognize requester");
        }
    }

    private void ensureUserValidity(AWSDynamoDB database,
                                    EncodedStringCache username,
                                    EncodedStringCache accessKey) {
        UserLoginEntry.ensureAccessKeyValid(database, username, accessKey);
        UserDataEntry.ensureUserHasData(database, username);
    }

    private void deleteWorkouts(AWSDynamoDB database, DeleteWorkoutsRequest request) {
        UserDataEntry.removeUserWorkouts(database, request.getUsername().get(),
                Arrays.stream(request.getWorkoutIds()).map(i -> i.get()).toArray(sz -> new String[sz])
        );
    }

    private void putWorkouts(AWSDynamoDB database,
                             WorkoutPutRequest in) {
        UserDataEntry.addUserWorkouts(database,
                in.getUsername().get(),
                in.getWorkouts());
    }

    private WorkoutListResult getWorkoutList(AWSDynamoDB database,
                                             WorkoutListRequest in) {
        Workout[] workouts
                = UserDataEntry.userWorkouts(database, in.getUsername().get());

        if (workouts == null) {
            throw new RuntimeException("User does not have data.");
        }
        this.getLogger().log("Returning " + workouts.length + " workouts");

        switch (in.getType()) {
            case LIST_ALL:
                return new WorkoutListResult(workouts);
            default:
                break;
        }
        throw new RuntimeException("WorkoutListRequest type was not recognised");
    }

}
