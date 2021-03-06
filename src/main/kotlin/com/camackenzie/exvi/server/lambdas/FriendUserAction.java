package com.camackenzie.exvi.server.lambdas;

import com.camackenzie.exvi.core.api.BooleanResult;
import com.camackenzie.exvi.core.api.FriendUserRequest;
import com.camackenzie.exvi.core.api.NoneResult;
import com.camackenzie.exvi.core.model.FriendedUser;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.LambdaAction;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class FriendUserAction implements LambdaAction<FriendUserRequest, NoneResult> {
    private static final String LOG_TAG = "FRIEND_USER_ACTION";

    @Override
    public NoneResult enact(@NotNull RequestBodyHandler context, @NotNull FriendUserRequest in) {
        // Preconditions
        if (in.getUsers().length == 0) throw new ApiException(400, "No users provided to friend");
        if (in.getUsername().get().isEmpty()) throw new ApiException(400, "Requester username is blank");
        if (in.getAccessKey().get().isEmpty()) throw new ApiException(400, "Requester access key is blank");

        // Retrieve resources
        var database = context.getResourceManager().getDatabase();
        var user = UserDataEntry.ensureUserValidity(database, in.getUsername(), in.getAccessKey());

        // Create lists of friend objects to add or remove
        var senderToFriend = Arrays.stream(in.getUsers())
                .map(it -> {
                    try {
                        UserLoginEntry.ensureUserExists(database, it.get());
                        return new FriendedUser(it, false, false);
                    } catch (ApiException e) {
                        context.getLogger().e("Could not register user for friending", e, LOG_TAG);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(FriendedUser[]::new);
        var receiverToFriend = new FriendedUser[]{
                new FriendedUser(in.getUsername(), false, true)
        };

        // Determine whether to friend or unfriend users
        if (in.getFriend()) { // Friend users
            // Add requests to requesting user
            user.addFriends(senderToFriend);
            // Add requests to receiving users
            for (var receiverId : in.getUsers()) {
                var receiver = UserDataEntry.ensureUserHasData(database, receiverId);
                receiver.addFriends(receiverToFriend);
            }
        } else { // Unfriend users
            // Remove requests for requesting user
            user.removeFriends(senderToFriend);
            // Remove requests of receiving users
            for (var receiverId : in.getUsers()) {
                var receiver = UserDataEntry.ensureUserHasData(database, receiverId);
                receiver.removeFriends(receiverToFriend);
            }
        }

        return new NoneResult();
    }
}
