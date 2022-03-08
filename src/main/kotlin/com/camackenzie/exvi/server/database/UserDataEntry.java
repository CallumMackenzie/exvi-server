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
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.model.Workout;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.camackenzie.exvi.server.util.ApiException;
import com.google.gson.Gson;

import java.lang.reflect.Array;
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

    public UserDataEntry(String username) {
        this.username = username;
        this.bodyStats = BodyStats.Companion.average();
        this.workouts = new Workout[0];
        this.activeWorkouts = new ActiveWorkout[0];
    }

    public String getUsername() {
        return this.username;
    }

    public BodyStats getBodyStats() {
        return this.bodyStats;
    }

    public Workout[] getWorkouts() {
        return workouts;
    }

    public ActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts;
    }

    public void setBodyStats(BodyStats bodyStats) {
        this.bodyStats = bodyStats;
    }

    public static String getUserWorkoutsJSON(AWSDynamoDB database,
                                             String user) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", user)
                .withProjectionExpression("workouts")
                .withConsistentRead(true);
        Item item = database.cacheTable("exvi-user-data")
                .getItem(get);
        return item.getJSON("workouts");
    }

    public static Workout[] userWorkouts(AWSDynamoDB database,
                                         String user) {
        return gson.fromJson(getUserWorkoutsJSON(database,
                user), Workout[].class);
    }

    public static String getActiveUserWorkoutsJSON(AWSDynamoDB database,
                                                   String user) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", user)
                .withProjectionExpression("activeWorkouts")
                .withConsistentRead(true);
        Item item = database.cacheTable("exvi-user-data")
                .getItem(get);
        return item.getJSON("activeWorkouts");
    }

    public static ActiveWorkout[] activeUserWorkouts(AWSDynamoDB database,
                                                     String user) {
        return gson.fromJson(getActiveUserWorkoutsJSON(database,
                user), ActiveWorkout[].class);
    }

    public static void ensureUserHasData(AWSDynamoDB database,
                                         String user) {
        if (database.getObjectFromTable("exvi-user-login", "username",
                user, UserLoginEntry.class) == null) {
            throw new ApiException(400, "User does not have an account");
        }
        Item item = database.cacheTable("exvi-user-data")
                .getItem("username", user);
        if (item == null) {
            database.putObjectInTable("exvi-user-data", new UserDataEntry(user));
        }
    }

    public static void ensureUserHasData(AWSDynamoDB database,
                                         EncodedStringCache user) {
        ensureUserHasData(database, user.get());
    }

    private static void updateUserWorkoutsRaw(AWSDynamoDB database,
                                              String user,
                                              List<Map> workoutList) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", user)
                .withUpdateExpression("set workouts = :a")
                .withValueMap(new ValueMap().withList(":a", workoutList))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        database.cacheTable("exvi-user-data")
                .updateItem(update);
    }

    public static void updateUserWorkouts(AWSDynamoDB database,
                                          String user,
                                          Workout[] workouts) {
        updateUserWorkoutsRaw(database, user, toMapList(workouts));
    }

    public static void updateUserWorkouts(AWSDynamoDB database,
                                          String user,
                                          List<Workout> workouts) {
        updateUserWorkoutsRaw(database, user, toMapList(workouts));
    }

    public static void removeUserWorkouts(AWSDynamoDB database,
                                          String user,
                                          String[] ids) {
        ArrayList<Workout> newWorkouts = new ArrayList<>();
        for (var workout : userWorkouts(database, user)) {
            boolean remove = false;
            for (var id : ids) {
                if (workout.getId().get().equals(id)) {
                    remove = true;
                    break;
                }
            }
            if (!remove) {
                newWorkouts.add(workout);
            }
        }
        updateUserWorkouts(database, user, newWorkouts);
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

    public static void addActiveUserWorkouts(AWSDynamoDB database,
                                             String user,
                                             ActiveWorkout[] workouts) {

    }

    public static void addUserWorkouts(AWSDynamoDB database,
                                       String user,
                                       Workout[] workouts) {
        List<Map> toAppend = new ArrayList<>();
        Workout[] userWorkouts = userWorkouts(database, user);
        boolean[] workoutIdsMatched = new boolean[userWorkouts.length];

        forEachIdentifiable(workouts,
                (wk, index) -> {
                    UpdateItemSpec spec = new UpdateItemSpec()
                            .withPrimaryKey("username", user)
                            .withUpdateExpression("set workouts[" + index + "] = :updated")
                            .withValueMap(new ValueMap()
                                    .withMap(":updated", toMap(wk)))
                            .withReturnValues(ReturnValue.UPDATED_NEW);
                    database.cacheTable("exvi-user-data")
                            .updateItem(spec);
                },
                wk -> {
                    toAppend.add(toMap(wk));
                });

        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", user)
                .withUpdateExpression("set workouts = list_append(:a, workouts)")
                .withValueMap(new ValueMap().withList(":a", toAppend))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        database.cacheTable("exvi-user-data")
                .updateItem(update);
    }

}
