/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.amazonaws.services.lambda.runtime.Context;
import com.camackenzie.exvi.core.api.GenericDataRequest;
import com.camackenzie.exvi.core.api.GenericDataResult;
import com.camackenzie.exvi.core.api.WorkoutListRequest;
import com.camackenzie.exvi.core.api.WorkoutListResult;
import com.camackenzie.exvi.core.api.WorkoutPutRequest;
import com.camackenzie.exvi.core.model.Workout;
import com.camackenzie.exvi.core.util.None;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import com.camackenzie.exvi.server.util.RequestException;

/**
 * @author callum
 */
public class UserDataRequestAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    public UserDataRequestAction() {
        super(GenericDataRequest.class);
    }

    @Override
    public GenericDataResult handleBodyRequest(GenericDataRequest in, Context context) {
        AWSDynamoDB database = new AWSDynamoDB();

        try {
            UserLoginEntry.ensureAccessKeyValid(database, in.getUsername().get(), in.getAccessKey().get());
            UserDataEntry.ensureUserHasData(database, in.getUsername().get());

            String requester = in.getRequester().get();
            this.getLogger().log("Requester: " + requester);

            if (requester.equals(WorkoutListRequest.UID())) {
                WorkoutListResult res = this.getWorkoutList(database, in);
                return new GenericDataResult<>(res);
            } else if (requester.equals(WorkoutPutRequest.UID())) {
                this.putWorkouts(database, in);
                return new GenericDataResult(200, "Success", None.INSTANCE);
            }
        } catch (Exception e) {
            this.getLogger().log("Request error: " + e);
        }
        throw new RequestException(400, "Invalid request");
    }

    private void putWorkouts(AWSDynamoDB database,
                             GenericDataRequest<WorkoutPutRequest> in) {
        UserDataEntry.addUserWorkouts(database,
                in.getUsername().get(),
                in.getBody().getWorkouts());
    }

    private WorkoutListResult getWorkoutList(AWSDynamoDB database,
                                             GenericDataRequest<WorkoutListRequest> in) {
        WorkoutListRequest req = in.getBody();
        Workout[] workouts
                = UserDataEntry.userWorkouts(database, in.getUsername().get());

        if (workouts == null) {
            throw new RuntimeException("User does not have data.");
        }
        this.getLogger().log("Returning " + workouts.length + " workouts");

        switch (req.getType()) {
            case LIST_ALL:
                return new WorkoutListResult(workouts);
            default:
                break;
        }
        throw new RuntimeException("WorkoutListRequest type was not recognised");
    }

}
