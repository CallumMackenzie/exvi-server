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
import com.camackenzie.exvi.core.model.*;
import com.camackenzie.exvi.core.util.EncodedStringCache;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.core.util.RawIdentifiable;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.camackenzie.exvi.server.util.ApiException;
import com.camackenzie.exvi.server.util.DocumentDatabase;
import com.google.gson.Gson;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataEntry extends DatabaseEntry<UserDataEntry> {

    @NotNull
    private static final Gson gson = new Gson();

    @NotNull
    public final String username;
    private ActualWorkout[] workouts;
    private ActiveWorkoutArray activeWorkouts;
    private ActualBodyStats bodyStats;

    @NotNull
    private transient final DocumentDatabase database;

    private UserDataEntry(@NotNull DocumentDatabase database,
                          @NotNull String username,
                          ActualWorkout[] workouts,
                          ActiveWorkoutArray activeWorkouts,
                          ActualBodyStats bodyStats) {
        this.username = username;
        this.bodyStats = bodyStats;
        this.workouts = workouts;
        this.activeWorkouts = activeWorkouts;
        this.database = database;
    }

    /////////////////////////
    // Database helper methods
    /////////////////////////

    @NotNull
    private static UserDataEntry defaultData(@NotNull DocumentDatabase database,
                                             @NotNull String username) {
        return new UserDataEntry(database, username, new ActualWorkout[0], new ActiveWorkoutArray(new ActiveWorkout[0]),
                ActualBodyStats.average());
    }

    @NotNull
    private static UserDataEntry registeredUser(@NotNull DocumentDatabase database,
                                                @NotNull String username) {
        return new UserDataEntry(database, username, null, null, null);
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull DocumentDatabase database,
                                                  @NotNull String user) throws ApiException {
        if (database.getObjectFromTable("exvi-user-login", "username",
                user, UserLoginEntry.class) == null) {
            throw new ApiException(400, "User does not have an account");
        }
        Item item = database.getTable("exvi-user-data").getItem("username", user);
        if (item == null) {
            UserDataEntry entry = UserDataEntry.defaultData(database, user);
            database.putObjectInTable("exvi-user-data", entry);
            return entry;
        } else {
            return UserDataEntry.registeredUser(database, user);
        }
    }

    @NotNull
    public static UserDataEntry ensureUserHasData(@NotNull DocumentDatabase database,
                                                  @NotNull EncodedStringCache user) {
        return ensureUserHasData(database, user.get());
    }

    private String getUserJSON(@NotNull String attr) {
        return getUserJSON(attr, attr);
    }

    private String getUserJSON(@NotNull String projectionExpr, @NotNull String attr) {
        GetItemSpec get = new GetItemSpec()
                .withPrimaryKey("username", username)
                .withProjectionExpression(projectionExpr)
                .withConsistentRead(true);
        Item item = database.getTable("exvi-user-data")
                .getItem(get);
        return item.getJSON(attr);
    }

    private UpdateItemOutcome updateDatabaseRaw(@NotNull String key, @NotNull Function<UpdateItemSpec, UpdateItemSpec> spec) {
        UpdateItemSpec update = spec.apply(new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withReturnValues(ReturnValue.UPDATED_NEW));
        return database.getTable("exvi-user-data").updateItem(update);
    }

    private void updateDatabaseMapRaw(@NotNull String key, Map<String, ?> value) {
        updateDatabaseRaw(key, item -> item
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withMap(":a", value)));
    }

    private void updateDatabaseListRaw(@NotNull String key, List<?> value) {
        updateDatabaseRaw(key, item -> item
                .withUpdateExpression("set " + key + " = :a")
                .withValueMap(new ValueMap().withList(":a", value)));
    }

    private void appendToDatabaseList(@NotNull String key, List<?> value) {
        updateDatabaseRaw(key, spec -> spec
                .withUpdateExpression("set " + key + " = list_append(:a, " + key + ")")
                .withValueMap(new ValueMap().withList(":a", value)));
    }

    /////////////////////////
    // General helper methods
    /////////////////////////

    @NotNull
    private static <T> Map<String, ?> toMap(@NotNull T in) {
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

    public ActualBodyStats getBodyStats() {
        return this.bodyStats = ExviSerializer.INSTANCE.fromJson(ActualBodyStats.Companion.serializer(), getBodyStatsJSON());
    }

    public void setBodyStats(@NotNull ActualBodyStats bs) {
        this.bodyStats = bs;
        updateDatabaseMapRaw("bodyStats", toMap(bs));
    }

    /////////////////////////
    // Workout methods
    /////////////////////////

    public String getWorkoutsJSON() {
        return getUserJSON("workouts");
    }

    public ActualWorkout[] getWorkouts() {
        return workouts = gson.fromJson(getWorkoutsJSON(), ActualWorkout[].class);
    }

    public void setWorkouts(@NotNull List<Workout> workoutList) {
        updateDatabaseListRaw("workouts", toMapList(workoutList));
    }

    /**
     * Does not check if the given workouts are already present
     * just appends them to the list in the database
     *
     * @param workoutList the workouts to add
     */
    private void addWorkoutsRaw(@NotNull List<Workout> workoutList) {
        appendToDatabaseList("workouts", toMapList(workoutList));
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
                    updateDatabaseMapRaw("workouts[" + userIndex + "]", toMap(addedWk));
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

    public String getActiveWorkoutsJSON() {
        return getUserJSON("activeWorkouts");
    }

    public ActualActiveWorkout[] getActiveWorkouts() {
        activeWorkouts = ExviSerializer.INSTANCE.fromJson(ActiveWorkoutArray.Companion.serializer(),
                getActiveWorkoutsJSON());
        return (ActualActiveWorkout[]) activeWorkouts.getArray();
    }

    public void setActiveWorkouts(@NotNull List<ActualActiveWorkout> workoutList) {
        updateDatabaseListRaw("activeWorkouts", toMapList(workoutList));
    }

    private void addActiveWorkoutsRaw(@NotNull List<ActualActiveWorkout> workoutList) {
        appendToDatabaseList("activeWorkouts", toMapList(workoutList));
    }

    public void removeActiveWorkouts(@NotNull Identifiable[] ids) {
        if (ids.length == 0) return;
        ArrayList<ActualActiveWorkout> newWorkouts = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(ids),
                arrayToList(getActiveWorkouts()), (a, ai, b, bi) -> Unit.INSTANCE,
                (a, ai) -> Unit.INSTANCE,
                (b, bi) -> {
                    newWorkouts.add((ActualActiveWorkout) b);
                    return Unit.INSTANCE;
                });
        setActiveWorkouts(newWorkouts);
    }

    public void removeActiveWorkouts(@NotNull EncodedStringCache[] ids) {
        Identifiable[] iids = Arrays.stream(ids)
                .map(RawIdentifiable::new)
                .toArray(Identifiable[]::new);
        removeActiveWorkouts(iids);
    }

    public void addActiveWorkouts(@NotNull ActualActiveWorkout[] workouts) {
        if (activeWorkouts.getArray().length == 0) return;
        List<ActualActiveWorkout> toAppend = new ArrayList<>();
        Identifiable.intersectIndexed(arrayToList(workouts), arrayToList(getActiveWorkouts()),
                (addedWk, addedIndex, userWk, userIndex) -> {
                    updateDatabaseMapRaw("activeWorkouts[" + userIndex + "]", toMap(userWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add(addedWorkout);
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) addActiveWorkoutsRaw(toAppend);
    }

}
