/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.camackenzie.exvi.core.model.*;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.core.util.RawIdentifiable;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.camackenzie.exvi.server.util.Serializers;
import com.camackenzie.exvi.server.util.SortedCache;
import kotlin.Unit;
import kotlin.jvm.Transient;
import kotlinx.serialization.SerializationStrategy;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.Encoder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static com.camackenzie.exvi.core.model.ExviSerializer.Builtin.element;
import static com.camackenzie.exvi.core.util.LoggingKt.getExviLogger;
import static kotlinx.serialization.descriptors.SerialDescriptorsKt.buildClassSerialDescriptor;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataEntry {

    private static final String LOG_TAG = "USER_DATA_ENTRY",
            DATA_TABLE_NAME = "exvi-user-data",
            USERNAME_JSON_KEY = "username", // Must be the same name as the database primary key
            WORKOUTS_JSON_KEY = "workouts",
            ACTIVE_WORKOUTS_JSON_KEY = "activeWorkouts",
            BODY_STATS_JSON_KEY = "bodyStats",
            FRIENDS_JSON_KEY = "friends";
    private static final SortedCache<UserDataEntry> userCache
            = new SortedCache<>(Comparator.comparing(a -> a.accesses), 5);

    // Ensure elements to be serialized are added to the serial descriptor and serializer

    @NotNull
    public final String username;
    private ActualWorkout[] workouts;
    private ActualActiveWorkout[] activeWorkouts;
    private ActualBodyStats bodyStats;
    private FriendedUser[] friends;

    @NotNull
    @Transient
    private transient final DocumentDatabase database;

    // Used to score importance of account for caching
    @Transient
    private transient int accesses;

    private static final SerialDescriptor descriptor = buildClassSerialDescriptor(
            "UserDataEntry",
            new SerialDescriptor[0],
            bt -> {
                element(bt, USERNAME_JSON_KEY, Serializers.string.getDescriptor());
                element(bt, WORKOUTS_JSON_KEY, Serializers.workoutArray.getDescriptor());
                element(bt, ACTIVE_WORKOUTS_JSON_KEY, Serializers.activeWorkoutArray.getDescriptor());
                element(bt, BODY_STATS_JSON_KEY, Serializers.bodyStats.getDescriptor());
                element(bt, FRIENDS_JSON_KEY, Serializers.friendedUserArray.getDescriptor());
                return Unit.INSTANCE;
            }
    );

    private UserDataEntry(@NotNull DocumentDatabase database,
                          @NotNull String username,
                          ActualWorkout[] workouts,
                          ActualActiveWorkout[] activeWorkouts,
                          ActualBodyStats bodyStats,
                          FriendedUser[] friendedUsers) {
        this.username = username;
        this.bodyStats = bodyStats;
        this.workouts = workouts;
        this.activeWorkouts = activeWorkouts;
        this.database = database;
        this.friends = friendedUsers;
    }

    /////////////////////////
    // Database helper methods
    /////////////////////////

    @NotNull
    public static UserDataEntry defaultData(@NotNull DocumentDatabase database,
                                            @NotNull String username) {
        return new UserDataEntry(database, username, new ActualWorkout[0],
                new ActualActiveWorkout[0],
                ActualBodyStats.average(),
                new FriendedUser[0]);
    }

    @NotNull
    private static UserDataEntry registeredUser(@NotNull DocumentDatabase database,
                                                @NotNull String username) {
        return new UserDataEntry(database, username, null, null, null, null);
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull DocumentDatabase database,
                                                  @NotNull String user) throws ApiException {
        UserLoginEntry.ensureUserExists(database, user);
        // Checked for cached user data
        UserDataEntry entry = userCache.matchFirst(it -> it.username.equalsIgnoreCase(user));
        if (entry == null) {
            // Check for database user data
            Item item = database.getTable(DATA_TABLE_NAME).getItem(USERNAME_JSON_KEY, user);
            // Recreate user data object if needed
            if (item == null) {
                entry = UserDataEntry.defaultData(database, user);
                database.putObjectInTable(DATA_TABLE_NAME, UserDataEntry.serializer, entry);
            } else // Use previous database data
                entry = UserDataEntry.registeredUser(database, user);
        } else getExviLogger().i("Cache hit", null, LOG_TAG);
        ++entry.accesses;
        userCache.cache(entry);
        return entry;
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull DocumentDatabase database,
                                                  @NotNull EncodedStringCache user) {
        return ensureUserHasData(database, user.get());
    }

    @NotNull
    public static UserDataEntry ensureUserValidity(@NotNull DocumentDatabase database,
                                                   @NotNull EncodedStringCache username,
                                                   @NotNull EncodedStringCache accessKey) {
        UserLoginEntry.ensureAccessKeyValid(database, username, accessKey);
        return UserDataEntry.ensureUserHasData(database, username);
    }

    private String getUserJSON(@NotNull String attr) {
        return getUserJSON(attr, attr);
    }

    private String getUserJSON(@NotNull String projectionExpr, @NotNull String attr) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey(USERNAME_JSON_KEY, username)
                .withProjectionExpression(projectionExpr)
                .withConsistentRead(true);
        Item item = database.getTable(DATA_TABLE_NAME)
                .getItem(get);
        // Ensure user item exists, if not, remove user from cache
        if (item == null) {
            if (userCache.removeFirst(user -> user.username.equalsIgnoreCase(username)) != null)
                getExviLogger().i("Removed user \"" + username + "\" from cache", null, LOG_TAG);
            else
                getExviLogger().w("Attempted to remove non-cached user " + username + " from cache", null, LOG_TAG);
            return null;
        }
        return item.getJSON(attr);
    }

    private void updateDatabaseRaw(@NotNull String key, @NotNull Function<UpdateItemSpec, UpdateItemSpec> spec) {
        UpdateItemSpec update = spec.apply(new UpdateItemSpec()
                .withPrimaryKey(USERNAME_JSON_KEY, username)
                .withReturnValues(ReturnValue.UPDATED_NEW));
        database.getTable(DATA_TABLE_NAME).updateItem(update);
    }

    private void updateDatabaseMapRaw(@NotNull String key, String json) {
        updateDatabaseRaw(key, item -> item
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withJSON(":a", json)));
    }

    private void updateDatabaseListRaw(@NotNull String key, String json) {
        updateDatabaseRaw(key, item -> item
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withJSON(":a", json)));
    }

    private void appendToDatabaseList(@NotNull String key, String json) {
        updateDatabaseRaw(key, spec -> spec
                .withUpdateExpression("set " + key + " = list_append(:a, " + key + ")")
                .withValueMap(new ValueMap().withJSON(":a", json)));
    }

    /////////////////////////
    // Body stats methods
    /////////////////////////

    public String getBodyStatsJSON() {
        return getUserJSON(BODY_STATS_JSON_KEY);
    }

    public ActualBodyStats getBodyStats() {
        return this.bodyStats = ExviSerializer.fromJson(Serializers.bodyStats, getBodyStatsJSON());
    }

    public void setBodyStats(@NotNull ActualBodyStats bs) {
        this.bodyStats = bs;
        updateDatabaseMapRaw(BODY_STATS_JSON_KEY, ExviSerializer.toJson(Serializers.bodyStats, bs));
    }

    /////////////////////////
    // Workout methods
    /////////////////////////

    public String getWorkoutsJSON() {
        return getUserJSON(WORKOUTS_JSON_KEY);
    }

    public ActualWorkout[] getWorkouts() {
        return workouts = ExviSerializer.fromJson(Serializers.workoutArray, getWorkoutsJSON());
    }

    public void setWorkouts(@NotNull List<ActualWorkout> workoutList) {
        updateDatabaseListRaw(WORKOUTS_JSON_KEY, ExviSerializer.toJson(Serializers.workoutList, workoutList));
    }

    /**
     * Does not check if the given workouts are already present
     * just appends them to the list in the database
     *
     * @param workoutList the workouts to add
     */
    private void addWorkoutsRaw(@NotNull List<ActualWorkout> workoutList) {
        appendToDatabaseList(WORKOUTS_JSON_KEY, ExviSerializer.toJson(Serializers.workoutList, workoutList));
    }

    public void removeWorkouts(@NotNull Identifiable[] ids) {
        if (ids.length == 0) return;
        ArrayList<ActualWorkout> newWorkouts = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(ids), List.of(getWorkouts()),
                (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((ActualWorkout) b);
                    return Unit.INSTANCE;
                });
        setWorkouts(newWorkouts);
    }

    public void removeWorkouts(@NotNull EncodedStringCache[] ids) {
        Identifiable[] iids = Arrays.stream(ids)
                .map(RawIdentifiable::new)
                .toArray(Identifiable[]::new);
        removeWorkouts(iids);
    }

    public void addWorkouts(@NotNull ActualWorkout[] workoutsToAdd) {
        if (workoutsToAdd.length == 0) return;
        List<ActualWorkout> toAdd = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(workoutsToAdd), List.of(getWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDatabaseMapRaw(WORKOUTS_JSON_KEY + "[" + userIndex + "]",
                            ExviSerializer.toJson(Serializers.workout, addedWk));
                    return Unit.INSTANCE;
                }, (addedWk, index) -> {
                    toAdd.add(addedWk);
                    return Unit.INSTANCE;
                });
        if (!toAdd.isEmpty()) addWorkoutsRaw(toAdd);
    }

    /////////////////////////
    // Active workout methods
    /////////////////////////

    @NotNull
    public String getActiveWorkoutsJSON() {
        var json = getUserJSON(ACTIVE_WORKOUTS_JSON_KEY);
        if (json == null) {
            json = ExviSerializer.toJson(Serializers.activeWorkoutArray, new ActualActiveWorkout[0]);
            updateDatabaseListRaw(ACTIVE_WORKOUTS_JSON_KEY, json);
        }
        return json;
    }

    public ActualActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts = ExviSerializer.fromJson(Serializers.activeWorkoutArray, getActiveWorkoutsJSON());
    }

    public void setActiveWorkouts(@NotNull List<ActualActiveWorkout> workoutList) {
        updateDatabaseListRaw(ACTIVE_WORKOUTS_JSON_KEY, ExviSerializer.toJson(Serializers.activeWorkoutList, workoutList));
    }

    private void addActiveWorkoutsRaw(@NotNull List<ActualActiveWorkout> workoutList) {
        appendToDatabaseList(ACTIVE_WORKOUTS_JSON_KEY, ExviSerializer.toJson(Serializers.activeWorkoutList, workoutList));
    }

    public void removeActiveWorkouts(@NotNull Identifiable[] ids) {
        if (ids.length == 0) return;
        ArrayList<ActualActiveWorkout> newWorkouts = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(ids), List.of(getActiveWorkouts()),
                (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((ActualActiveWorkout) b);
                    return Unit.INSTANCE;
                });
        setActiveWorkouts(newWorkouts);
    }

    public void removeActiveWorkouts(@NotNull EncodedStringCache[] ids) {
        Identifiable[] iIds = Arrays.stream(ids)
                .map(RawIdentifiable::new)
                .toArray(Identifiable[]::new);
        removeActiveWorkouts(iIds);
    }

    public void addActiveWorkouts(@NotNull ActualActiveWorkout[] workouts) {
        if (workouts.length == 0) return;
        List<ActualActiveWorkout> toAppend = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(workouts), List.of(getActiveWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDatabaseMapRaw(ACTIVE_WORKOUTS_JSON_KEY + "[" + userIndex + "]",
                            ExviSerializer.toJson(Serializers.activeWorkout, addedWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add(addedWorkout);
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) addActiveWorkoutsRaw(toAppend);
    }

    /////////////////////////
    // Friend methods
    /////////////////////////

    @NotNull
    public String getFriendsJSON() {
        var json = getUserJSON(FRIENDS_JSON_KEY);
        if (json == null) {
            json = ExviSerializer.toJson(Serializers.friendedUserArray, new FriendedUser[0]);
            updateDatabaseListRaw(FRIENDS_JSON_KEY, json);
        }
        return json;
    }

    public FriendedUser[] getFriends() {
        return friends = ExviSerializer.fromJson(Serializers.friendedUserArray, getFriendsJSON());
    }

    private void addFriendsRaw(@NotNull List<FriendedUser> toFriend) {
        appendToDatabaseList(FRIENDS_JSON_KEY, ExviSerializer.toJson(Serializers.friendedUserList, toFriend));
    }

    private void setFriends(@NotNull List<FriendedUser> toKeep) {
        updateDatabaseListRaw(FRIENDS_JSON_KEY, ExviSerializer.toJson(Serializers.friendedUserList, toKeep));
    }

    public void addFriends(@NotNull FriendedUser[] friends) {
        if (friends.length == 0) return;
        List<FriendedUser> toAppend = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(friends), List.of(getFriends()),
                (addedFriend, addedIdx, userFriend, userIdx) -> {
                    getExviLogger().v("Intersected friend: \nDATABASE:"
                            + ExviSerializer.toJson(Serializers.friendedUser, userFriend)
                            + "\nINCOMING: " + ExviSerializer.toJson(Serializers.friendedUser, addedFriend), null, LOG_TAG);

                    if (userFriend.getAcceptedRequest()) // Is accepted in database
                        throw new ApiException(400, "User is already a friend");
                    else if (userFriend.getIncomingRequest() || addedFriend.getIncomingRequest()) { // Is incoming in database or in request
                        var newFriend = new FriendedUser(userFriend.getUsername(), true, false);
                        updateDatabaseMapRaw(FRIENDS_JSON_KEY + "[" + userIdx + "]",
                                ExviSerializer.toJson(Serializers.friendedUser, newFriend));
                        return Unit.INSTANCE;
                    } else // Not incoming and not accepted
                        throw new ApiException(400, "Friend request already sent");
                }, (addedFriend, index) -> {
                    // Ensure user does not friend themselves
                    if (addedFriend.getUsername().get().equals(this.username))
                        throw new ApiException(400, "You can't friend yourself");
                    else toAppend.add(addedFriend);
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) addFriendsRaw(toAppend);
    }

    public void removeFriends(@NotNull FriendedUser[] friends) {
        if (friends.length == 0) return;
        List<FriendedUser> keepFriends = new ArrayList<>();
        Identifiable.intersectIndexed(List.of(getFriends()), List.of(friends),
                (userFriend, userIdx, removedFriend, removedIdx) -> Unit.INSTANCE,
                (userFriend, index) -> {
                    keepFriends.add(userFriend);
                    return Unit.INSTANCE;
                });
        setFriends(keepFriends);
    }

    public boolean isFriendsWith(UserDataEntry other) {
        var friendsFriends = other.getFriends();
        boolean localFriends = false, remoteFriends = false;
        for (var friend : this.getFriends())
            if (friend.getAcceptedRequest() && friend.getUsername().get().equals(other.username)) {
                localFriends = true;
                break;
            }
        if (!localFriends) return false;
        for (var friend : other.getFriends())
            if (friend.getAcceptedRequest() && friend.getUsername().get().equals(username))
                return true;
        return false;
    }

    /////////////////////////
    // Serializer
    /////////////////////////

    public static final SerializationStrategy<UserDataEntry> serializer = new SerializationStrategy<>() {

        @NotNull
        @Override
        public SerialDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public void serialize(@NotNull Encoder encoder, UserDataEntry userDataEntry) {
            var struct = encoder.beginStructure(descriptor);
            struct.encodeStringElement(descriptor, 0, userDataEntry.username);
            struct.encodeSerializableElement(descriptor, 1, Serializers.workoutArray, userDataEntry.workouts);
            struct.encodeSerializableElement(descriptor, 2, Serializers.activeWorkoutArray, userDataEntry.activeWorkouts);
            struct.encodeSerializableElement(descriptor, 3, Serializers.bodyStats, userDataEntry.bodyStats);
            struct.encodeSerializableElement(descriptor, 4, Serializers.friendedUserArray, userDataEntry.friends);
            struct.endStructure(descriptor);
        }
    };

}
