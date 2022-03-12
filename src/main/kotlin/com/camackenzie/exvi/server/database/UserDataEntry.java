/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.camackenzie.exvi.server.database;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.UpdateItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author callum
 */
public class UserDataEntry extends DatabaseEntry<UserDataEntry> {

    private static final Gson gson = new Gson();

    @NotNull
    public final String username;
    private Workout[] workouts;
    private ActiveWorkout[] activeWorkouts;
    private BodyStats bodyStats;

    @NotNull
    private transient AWSDynamoDB database;

    private UserDataEntry(@NotNull AWSDynamoDB database,
                          @NotNull String username,
                          Workout[] workouts,
                          ActiveWorkout[] activeWorkouts,
                          BodyStats bodyStats) {
        this.username = username;
        this.bodyStats = bodyStats;
        this.workouts = workouts;
        this.activeWorkouts = activeWorkouts;
        this.database = database;
    }

    @NotNull
    private static UserDataEntry defaultData(@NotNull AWSDynamoDB database, @NotNull String username) {
        return new UserDataEntry(database, username, new Workout[0], new ActiveWorkout[0], BodyStats.average());
    }

    @NotNull
    private static UserDataEntry registeredUser(@NotNull AWSDynamoDB database, @NotNull String username) {
        return new UserDataEntry(database, username, null, null, null);
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database, @NotNull String user) throws ApiException {
        if (database.getObjectFromTable("exvi-user-login", "username",
                user, UserLoginEntry.class) == null) {
            throw new ApiException(400, "User does not have an account");
        }
        Item item = database.cacheTable("exvi-user-data").getItem("username", user);
        if (item == null) {
            UserDataEntry entry = UserDataEntry.defaultData(database, user);
            database.putObjectInTable("exvi-user-data", entry);
            return entry;
        } else {
            return UserDataEntry.registeredUser(database, user);
        }
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database, @NotNull EncodedStringCache user) {
        return ensureUserHasData(database, user.get());
    }

    @NotNull
    private static <T extends SelfSerializable> List<Map> toMapList(@NotNull List<T> l) {
        List<Map> ret = new ArrayList<>();
        for (var li : l) {
            ret.add(toMap(li));
        }
        return ret;
    }

    @NotNull
    private static <T> Map toMap(@NotNull T in) {
        return gson.fromJson(gson.toJson(in), Map.class);
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

    public String getWorkoutsJSON() {
        return getUserJSON("workouts", "workouts");
    }

    public String getActiveWorkoutsJSON() {
        return getUserJSON("activeWorkouts", "activeWorkouts");
    }

    public String getBodyStatsJSON() {
        return getUserJSON("bodyStats", "bodyStats");
    }

    public Workout[] getWorkouts() {
        return workouts = gson.fromJson(getWorkoutsJSON(), Workout[].class);
    }

    public ActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts = gson.fromJson(getActiveWorkoutsJSON(), ActiveWorkout[].class);
    }

    public BodyStats getBodyStats() {
        return this.bodyStats = gson.fromJson(getBodyStatsJSON(), BodyStats.class);
    }

    private UpdateItemOutcome updateDataEntryRaw(@NotNull String key, Object value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        return database.cacheTable("exvi-user-data").updateItem(update);
    }

    private UpdateItemOutcome appendToDataEntryList(@NotNull String key, Object value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = list_append(:a, " + key + ")")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        return database.cacheTable("exvi-user-data").updateItem(update);
    }

    private <T> List<T> arrayToList(@NotNull T[] arr) {
        return new ArrayList<>() {{
            for (var i : arr) add(i);
        }};
    }

    private void updateActiveUserWorkouts(@NotNull List<ActiveWorkout> workoutList) {
        updateDataEntryRaw("activeWorkouts", toMapList(workoutList));
    }

    public void updateUserWorkouts(@NotNull List<Workout> workoutList) {
        updateDataEntryRaw("workouts", toMapList(workoutList));
    }

    public void removeUserWorkouts(@NotNull Identifiable[] ids) {
        ArrayList<Workout> newWorkouts = new ArrayList<>();
        Identifiable.checkIntersects(arrayToList(ids),
                arrayToList(getWorkouts()), (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((Workout) b);
                    return Unit.INSTANCE;
                });
        updateUserWorkouts(newWorkouts);
    }

    public void removeUserWorkouts(@NotNull EncodedStringCache[] ids) {
        var identifiables = new Identifiable[ids.length];
        for (int i = 0; i < identifiables.length; ++i) {
            identifiables[i] = new RawIdentifiable(ids[i]);
        }
        removeUserWorkouts(identifiables);
    }

    public void removeActiveUserWorkouts(@NotNull Identifiable[] ids) {
        ArrayList<ActiveWorkout> newWorkouts = new ArrayList<>();
        Identifiable.checkIntersects(arrayToList(ids),
                arrayToList(getActiveWorkouts()), (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((ActiveWorkout) b);
                    return Unit.INSTANCE;
                });
        updateActiveUserWorkouts(newWorkouts);
    }

    public void removeActiveUserWorkouts(@NotNull EncodedStringCache[] ids) {
        var idnts = new Identifiable[ids.length];
        for (int i = 0; i < idnts.length; ++i) {
            idnts[i] = new RawIdentifiable(ids[i]);
        }
        removeActiveUserWorkouts(idnts);
    }

    public void addActiveUserWorkouts(@NotNull ActiveWorkout[] workouts) {
        List<Map> toAppend = new ArrayList<>();
        Identifiable.checkIntersects(arrayToList(workouts), arrayToList(getActiveWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDataEntryRaw("activeWorkouts[" + userIndex + "]", toMap(userWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add(toMap(addedWorkout));
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) appendToDataEntryList("activeWorkouts", toAppend);
    }

    public void addUserWorkouts(@NotNull Workout[] workouts) {
        List<Map> toAppend = new ArrayList<>();
        Identifiable.checkIntersects(arrayToList(workouts), arrayToList(getWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDataEntryRaw("workouts[" + userIndex + "]", toMap(userWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add(toMap(addedWorkout));
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) appendToDataEntryList("workouts", toAppend);
    }

    public void setBodyStats(BodyStats bs) {
        this.bodyStats = bs;
        updateDataEntryRaw("bodyStats", toMap(bs));
    }

}
