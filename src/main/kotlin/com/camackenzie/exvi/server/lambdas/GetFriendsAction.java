package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.GetFriendedUsersRequest;
import com.camackenzie.exvi.core.api.GetFriendedUsersResponse;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

public class GetFriendsAction implements LambdaAction<GetFriendedUsersRequest, GetFriendedUsersResponse> {
    @Override
    public GetFriendedUsersResponse enact(@NotNull RequestBodyHandler context,
                                          @NotNull GetFriendedUsersRequest in) {
        var database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, in.getUsername(), in.getAccessKey());
        return new GetFriendedUsersResponse(user.getFriends());
    }
}
