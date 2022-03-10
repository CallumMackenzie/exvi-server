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
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.ApiException;
import com.google.gson.Gson;
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

    private final String username;
    private final Workout[] workouts;
    private final ActiveWorkout[] activeWorkouts;
    private BodyStats bodyStats;

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
    }

    private static UserDataEntry defaultData(@NotNull AWSDynamoDB database, @NotNull String username) {
        return new UserDataEntry(database, username, new Workout[0], new ActiveWorkout[0], BodyStats.average());
    }

    private static UserDataEntry registeredUser(@NotNull AWSDynamoDB database, @NotNull String username) {
        return new UserDataEntry(database, username, null, null, null);
    }

    public String getUserWorkoutsJSON() {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", username)
                .withProjectionExpression("workouts")
                .withConsistentRead(true);
        Item item = database.cacheTable("exvi-user-data")
                .getItem(get);
        return item.getJSON("workouts");
    }

    public Workout[] userWorkouts() {
        return gson.fromJson(getUserWorkoutsJSON(), Workout[].class);
    }

    public String getActiveUserWorkoutsJSON() {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", username)
                .withProjectionExpression("activeWorkouts")
                .withConsistentRead(true);
        Item item = database.cacheTable("exvi-user-data").getItem(get);
        return item.getJSON("activeWorkouts");
    }

    public ActiveWorkout[] activeWorkouts() {
        return gson.fromJson(getActiveUserWorkoutsJSON(), ActiveWorkout[].class);
    }

    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database, @NotNull String user) {
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

    public static UserDataEntry ensureUserHasData(@NotNull AWSDynamoDB database, @NotNull EncodedStringCache user) {
        return ensureUserHasData(database, user.get());
    }

    private static <T extends SelfSerializable> List<Map> toMapList(List<T> l) {
        List<Map> ret = new ArrayList<>();
        for (var li : l) {
            ret.add(toMap(li));
        }
        return ret;
    }

    private static <T extends SelfSerializable> List<Map> toMapList(T[] l) {
        List<Map> ret = new ArrayList<>();
        for (var li : l) {
            ret.add(toMap(li));
        }
        return ret;
    }

    private static <T> Map toMap(T in) {
        return gson.fromJson(gson.toJson(in), Map.class);
    }

    private static <T extends Identifiable> void forEachIdentifiable(
            T[] identifiables,
            BiConsumer<T, Integer> onMatch,
            Consumer<T> onNotMatched
    ) {
        boolean[] workoutIdsMatched = new boolean[identifiables.length];
        for (var toPut : identifiables) {
            int matched = -1;
            for (int i = 0; i < identifiables.length; ++i) {
                if (!workoutIdsMatched[i]) {
                    if (toPut.getIdentifier().get().equals(identifiables[i].getIdentifier().get())) {
                        matched = i;
                        workoutIdsMatched[i] = true;
                        break;
                    }
                }
            }
            if (matched == -1) {
                onNotMatched.accept(toPut);
            } else {
                onMatch.accept(toPut, matched);
            }
        }
    }

    private UpdateItemOutcome updateDataEntryRaw(String key, Object value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        return database.cacheTable("exvi-user-data").updateItem(update);
    }

    private UpdateItemOutcome appendToDataEntryList(String key, Object value) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withUpdateExpression("set " + key + " = list_append(:a, " + key + ")")
                .withValueMap(new ValueMap().withList(":a", value))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        return database.cacheTable("exvi-user-data").updateItem(update);
    }

    private void updateActiveUserWorkoutsRaw(List<Map> workoutList) {
        updateDataEntryRaw("activeWorkouts", workoutList);
    }

    private void updateUserWorkoutsRaw(List<Map> workoutList) {
        updateDataEntryRaw("workouts", workoutList);
    }

    public void updateUserWorkouts(List<Workout> workouts) {
        updateUserWorkoutsRaw(toMapList(workouts));
    }

    public void removeUserWorkouts(String[] ids) {
        ArrayList<Workout> newWorkouts = new ArrayList<>();
//        for (var workout : userWorkouts()) {
//            boolean remove = false;
//            for (var id : ids) {
//                if (workout.getId().get().equals(id)) {
//                    remove = true;
//                    break;
//                }
//            }
//            if (!remove) {
//                newWorkouts.add(workout);
//            }
//        }

        updateUserWorkouts(newWorkouts);
    }

    public void addActiveUserWorkouts(ActiveWorkout[] workouts) {
        List<Map> toAppend = new ArrayList<>();
        forEachIdentifiable(workouts,
                (wk, index) -> updateDataEntryRaw("activeWorkouts[" + index + "]", toMap(wk)),
                wk -> toAppend.add(toMap(wk)));
        if (!toAppend.isEmpty()) {
            appendToDataEntryList("activeWorkouts", toAppend);
        }
    }

    public void addUserWorkouts(Workout[] workouts) {
        List<Map> toAppend = new ArrayList<>();
        forEachIdentifiable(workouts,
                (wk, index) -> updateDataEntryRaw("workouts[" + index + "]", toMap(wk)),
                wk -> toAppend.add(toMap(wk)));
        if (!toAppend.isEmpty()) {
            appendToDataEntryList("workouts", toAppend);
        }
    }

    public String getUsername() {
        return this.username;
    }

    public BodyStats getBodyStats() {
        return this.bodyStats;
    }

    public void setBodyStats(BodyStats bodyStats) {
        this.bodyStats = bodyStats;
    }

    public Workout[] getWorkouts() {
        return workouts;
    }

    public ActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts;
    }

}
