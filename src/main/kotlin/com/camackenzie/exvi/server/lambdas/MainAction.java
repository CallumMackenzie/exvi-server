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
// High level entry point for requests
public class MainAction extends RequestBodyHandler<GenericDataRequest, GenericDataResult> {

    public MainAction() {
        // Set up polymorphic request serialization
        super(GenericDataRequest.Companion.serializer(), GenericDataResult.Companion.serializer());
    }

    // Map request classes to their respective actions
    private final Map<Class, LambdaAction> actionMap = new HashMap<>() {{
        put(WorkoutPutRequest.class, new WorkoutPutRequestAction());
        put(ActiveWorkoutPutRequest.class, new WorkoutPutRequestAction());

        put(GetBodyStatsRequest.class, new MutateBodyStatsAction());
        put(SetBodyStatsRequest.class, new MutateBodyStatsAction());

        put(WorkoutListRequest.class, new WorkoutListRequestAction());
        put(DeleteWorkoutsRequest.class, new DeleteWorkoutsAction());
        put(CompatibleVersionRequest.class, new VerifyVersionAction());
        put(VerificationRequest.class, new VerificationAction());
        put(AccountCreationRequest.class, new SignUpAction());
        put(LoginRequest.class, new LoginAction());
        put(RetrieveSaltRequest.class, new RetrieveSaltAction());
    }};

    @Override
    @NotNull
    @SuppressWarnings("unchecked")
    // Respond to incoming requests
    protected GenericDataResult handleBodyRequest(@NotNull GenericDataRequest in) {
        // Get action for request type
        final LambdaAction action = actionMap.get(in.getClass());
        // Assert action is not null
        if (action == null) throw new ApiException(400, "Could not recognize requester");
        // Return action output
        return action.enact(this, in); // Giving up some safety for type flexibility here
    }

}