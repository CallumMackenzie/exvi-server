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
import com.camackenzie.exvi.server.util.AWSDynamoDB;
import com.google.gson.Gson;

/**
 *
 * @author callum
 */
public class UserDataEntry extends DatabaseEntry {

    private static final Gson gson = new Gson();

    private final String username;
    private final Workout[] workouts;
    private final ActiveWorkout[] activeWorkouts;
    private BodyStats bodyStats;

    public UserDataEntry(String username) {
        this.username = username;
        this.bodyStats = BodyStats.average();
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

    public static Workout[] userWorkouts(AWSDynamoDB database,
            String user) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", user)
                .withAttributesToGet("workouts");
        Item item = database.cacheTable("exvi-user-data")
                .getItem(get);

        return gson.fromJson(gson.toJson(item.getList("workouts")), Workout[].class);
    }

    public static void ensureUserHasData(AWSDynamoDB database,
            String user) {
        if (database.getObjectFromTable("exvi-user-login", "username",
                user, UserLoginEntry.class) == null) {
            throw new RuntimeException("User does not have an account");
        }
        if (database.getObjectFromTable("exvi-user-data", "username", user,
                UserLoginEntry.class) == null) {
            database.putObjectInTable("exvi-user-data", new UserDataEntry(user));
        }
    }

    public static void updateUserWorkouts(AWSDynamoDB database,
            String user,
            Workout[] workouts) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", user)
                .withUpdateExpression("set workouts = :a")
                .withValueMap(new ValueMap().withList(":a", workouts))
                .withReturnValues(ReturnValue.UPDATED_NEW);
        database.cacheTable("exvi-user-data")
                .updateItem(update);
    }

    public static void addUserWorkouts(AWSDynamoDB database,
            String user,
            Workout[] workouts) {
        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("username", user)
                .withUpdateExpression("list_append(:a, workout)")
                .withValueMap(new ValueMap().withList(":a", workouts));
        database.cacheTable("exvi-user-data")
                .updateItem(update);
    }

}
