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
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.RequestBodyHandler;

/**
 *
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
            UserLoginEntry.ensureAccessKeyValid(database, in.getUsername(), in.getAccessKey());
            UserDataEntry.ensureUserHasData(database, in.getUsername());

            Class inClass = in.getRequestClass();
            if (inClass.equals(WorkoutListRequest.class)) {
                return new GenericDataResult(this.getWorkoutList(database, in));
            } else if (inClass.equals(WorkoutPutRequest.class)) {
                this.putWorkouts(database, in);
                return new GenericDataResult(200, "Success", Void.class);
            }
        } catch (Exception e) {
            this.getLogger().log("Request error: " + e);
        }
        return new GenericDataResult(400, "Invalid request", Void.class);
    }

    private void putWorkouts(AWSDynamoDB database,
            GenericDataRequest<WorkoutPutRequest> in) {
        UserDataEntry.addUserWorkouts(database,
                in.getUsername(),
                in.getBody().getWorkouts());
    }

    private WorkoutListResult getWorkoutList(AWSDynamoDB database,
            GenericDataRequest<WorkoutListRequest> in) {

        WorkoutListRequest req = in.getBody();
        Workout[] workouts
                = UserDataEntry.userWorkouts(database, in.getUsername());
        if (workouts == null) {
            throw new RuntimeException("User does not have data.");
        }
        switch (req.getType()) {
            case LIST_ALL:
                return new WorkoutListResult(workouts);
            default:
                break;
        }
        throw new RuntimeException("WorkoutListRequest type was not recognised");
    }

}
