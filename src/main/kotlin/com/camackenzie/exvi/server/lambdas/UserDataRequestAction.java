/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataRequestAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    public UserDataRequestAction() {
        super(GenericDataRequest.Companion.serializer(), GenericDataResult.Companion.serializer());
    }

    private final Map<Class, LambdaAction> actionMap = new HashMap<>() {{
        put(WorkoutListRequest.class, new WorkoutListRequestAction());
        put(WorkoutPutRequest.class, new WorkoutPutRequestAction());
        put(ActiveWorkoutPutRequest.class, new ActiveWorkoutPutRequestAction());
        put(DeleteWorkoutsRequest.class, new DeleteWorkoutsAction());
        put(GetBodyStatsRequest.class, new MutateBodyStatsAction());
        put(SetBodyStatsRequest.class, new MutateBodyStatsAction());
        put(CompatibleVersionRequest.class, new VerifyVersionAction());
        put(VerificationRequest.class, new VerificationAction());
        put(AccountCreationRequest.class, new SignUpAction());
        put(LoginRequest.class, new LoginAction());
        put(RetrieveSaltRequest.class, new RetrieveSaltAction());
    }};

    @Override
    @NotNull
    protected GenericDataResult handleBodyRequest(@NotNull GenericDataRequest in) {
        final LambdaAction action = actionMap.get(in.getClass());
        if (action == null) throw new ApiException(400, "Could not recognize requester");
        return action.enact(this, in);
    }

}