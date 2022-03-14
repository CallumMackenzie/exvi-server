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
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.camackenzie.exvi.core.model.ActiveWorkout;
import com.camackenzie.exvi.core.model.BodyStats;
import com.camackenzie.exvi.core.model.Workout;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.core.util.RawIdentifiable;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.ApiException;
import com.google.gson.Gson;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataEntry extends DatabaseEntry<UserDataEntry> {

    @NotNull
    private static final Gson gson = new Gson();

    @NotNull
    public final String username;
    private Workout[] workouts;
    private ActiveWorkout[] activeWorkouts;
    private BodyStats bodyStats;

    @NotNull
    private transient final AWSDynamoDB database;
    @NotNull
    private transient final LambdaLogger logger;

    private UserDataEntry(@NotNull AWSDynamoDB database,
                          @NotNull LambdaLogger logger,
                          @NotNull String username,
                          Workout[] workouts,
                          ActiveWorkout[] activeWorkouts,
                          BodyStats bodyStats) {
        this.username = username;
        this.bodyStats = bodyStats;
        this.workouts = workouts;
        this.activeWorkouts = activeWorkouts;
        this.database = database;
        this.logger = logger;
    }

    /////////////////////////
    // Database helper methods
    /////////////////////////

    @NotNull
    private static UserDataEntry defaultData(@NotNull AWSDynamoDB database,
                                             @NotNull LambdaLogger logger,
                                             @NotNull String username) {
        return new UserDataEntry(database, logger, username, new Workout[0], new ActiveWorkout[0], BodyStats.average());
    }

    @NotNull
    private static UserDataEntry registeredUser(@NotNull AWSDynamoDB database,
                                                @NotNull LambdaLogger logger,
                                                @NotNull String username) {
        return new UserDataEntry(database, logger, username, null, null, null);
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database,
                                                  @NotNull LambdaLogger logger,
                                                  @NotNull String user) throws ApiException {
        if (database.getObjectFromTable("exvi-user-login", "username",
                user, UserLoginEntry.class) == null) {
            throw new ApiException(400, "User does not have an account");
        }
        Item item = database.cacheTable("exvi-user-data").getItem("username", user);
        if (item == null) {
            UserDataEntry entry = UserDataEntry.defaultData(database, logger, user);
            database.putObjectInTable("exvi-user-data", entry);
            return entry;
        } else {
            return UserDataEntry.registeredUser(database, logger, user);
        }
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database,
                                                  @NotNull LambdaLogger logger,
                                                  @NotNull EncodedStringCache user) {
        return ensureUserHasData(database, logger, user.get());
    }

    private String getUserJSON(@NotNull String attr) {
        return getUserJSON(attr, attr);
    }

    private String getUserJSON(@NotNull String projectionExpr, @NotNull String attr) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", username)
                .withProjectionExpression(projectionExpr)
                .withConsistentRead(true);
        Item item = database.cacheTable("exvi-user-data")
                .getItem(get);
        return item.getJSON(attr);
    }

    private <T> void updateDataEntryRaw(@NotNull String key, T value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        database.cacheTable("exvi-user-data").updateItem(update);
    }

    private <T> void appendToDataEntryList(@NotNull String key, List<T> value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = list_append(:a, " + key + ")")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        var result = database.cacheTable("exvi-user-data").updateItem(update);
    }

    /////////////////////////
    // General helper methods
    /////////////////////////

    @NotNull
    private static <T> Map<?, ?> toMap(@NotNull T in) {
        return gson.fromJson(gson.toJson(in), Map.class);
    }

    @NotNull
    @SuppressWarnings("unused")
    private <T> List<T> arrayToList(@NotNull T[] arr) {
        return new ArrayList<>() {{
            addAll(List.of(arr));
        }};
    }

    @NotNull
    private static <T extends SelfSerializable> List<Map<?, ?>> toMapList(@NotNull List<T> l) {
        List<Map<?, ?>> ret = new ArrayList<>();
        for (var li : l) {
            ret.add(toMap(li));
        }
        return ret;
    }

    /////////////////////////
    // Body stats methods
    /////////////////////////

    public String getBodyStatsJSON() {
        return getUserJSON("bodyStats");
    }

    public BodyStats getBodyStats() {
        return this.bodyStats = gson.fromJson(getBodyStatsJSON(), BodyStats.class);
    }

    public void setBodyStats(@NotNull BodyStats bs) {
        this.bodyStats = bs;
        updateDataEntryRaw("bodyStats", toMap(bs));
    }

    /////////////////////////
    // Workout methods
    /////////////////////////

    public String getWorkoutsJSON() {
        return getUserJSON("workouts");
    }

    public Workout[] getWorkouts() {
        return workouts = gson.fromJson(getWorkoutsJSON(), Workout[].class);
    }

    public void setWorkouts(@NotNull List<Workout> workoutList) {
        updateDataEntryRaw("workouts", toMapList(workoutList));
    }

    /**
     * Does not check if the given workouts are already present
     * just appends them to the list in the database
     *
     * @param workoutList the workouts to add
     */
    private void addWorkoutsRaw(@NotNull List<Workout> workoutList) {
        appendToDataEntryList("workouts", toMapList(workoutList));
    }

    public void removeWorkouts(@NotNull Identifiable[] ids) {
        if (ids.length == 0) return;
        ArrayList<Workout> newWorkouts = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(ids), arrayToList(getWorkouts()),
                (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((Workout) b);
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

    public void addWorkouts(@NotNull Workout[] workoutsToAdd) {
        if (workoutsToAdd.length == 0) return;
        List<Workout> toAdd = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(workoutsToAdd), arrayToList(getWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDataEntryRaw("workouts[" + userIndex + "]", toMap(addedWk));
                    return Unit.INSTANCE;
                }, (addedWk, index) -> {
                    toAdd.add((Workout) addedWk);
                    return Unit.INSTANCE;
                });
        if (!toAdd.isEmpty()) addWorkoutsRaw(toAdd);
    }

    /////////////////////////
    // Active workout methods
    /////////////////////////

    public String getActiveWorkoutsJSON() {
        return getUserJSON("activeWorkouts");
    }

    public ActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts = gson.fromJson(getActiveWorkoutsJSON(), ActiveWorkout[].class);
    }

    public void setActiveWorkouts(@NotNull List<ActiveWorkout> workoutList) {
        updateDataEntryRaw("activeWorkouts", toMapList(workoutList));
    }

    private void addActiveWorkoutsRaw(@NotNull List<ActiveWorkout> workoutList) {
        appendToDataEntryList("activeWorkouts", toMapList(workoutList));
    }

    public void removeActiveWorkouts(@NotNull Identifiable[] ids) {
        if (ids.length == 0) return;
        ArrayList<ActiveWorkout> newWorkouts = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(ids),
                arrayToList(getActiveWorkouts()), (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((ActiveWorkout) b);
                    return Unit.INSTANCE;
                });
        setActiveWorkouts(newWorkouts);
    }

    public void removeActiveWorkouts(@NotNull EncodedStringCache[] ids) {
        var idnts = new Identifiable[ids.length];
        for (int i = 0; i < idnts.length; ++i) {
            idnts[i] = new RawIdentifiable(ids[i]);
        }
        removeActiveWorkouts(idnts);
    }

    public void addActiveWorkouts(@NotNull ActiveWorkout[] workouts) {
        if (activeWorkouts.length == 0) return;
        List<ActiveWorkout> toAppend = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(workouts), arrayToList(getActiveWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDataEntryRaw("activeWorkouts[" + userIndex + "]", toMap(userWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add((ActiveWorkout) addedWorkout);
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) addActiveWorkoutsRaw(toAppend);
    }

}
