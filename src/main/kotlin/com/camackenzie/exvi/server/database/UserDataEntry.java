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
import kotlin.Unit;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.descriptors.SerialDescriptor;
import kotlinx.serialization.encoding.Decoder;
import kotlinx.serialization.encoding.Encoder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static kotlinx.serialization.descriptors.SerialDescriptorsKt.buildClassSerialDescriptor;

/**
 * @author callum
 */
@SuppressWarnings("unused")
public class UserDataEntry extends DatabaseEntry<UserDataEntry> {

    @NotNull
    public final String username;
    private ActualWorkout[] workouts;
    private ActualActiveWorkout[] activeWorkouts;
    private ActualBodyStats bodyStats;

    @NotNull
    private transient final DocumentDatabase database;

    private UserDataEntry(@NotNull DocumentDatabase database,
                          @NotNull String username,
                          ActualWorkout[] workouts,
                          ActualActiveWorkout[] activeWorkouts,
                          ActualBodyStats bodyStats) {
        this.username = username;
        this.bodyStats = bodyStats;
        this.workouts = workouts;
        this.activeWorkouts = activeWorkouts;
        this.database = database;
    }

    private static final SerialDescriptor descriptor = buildClassSerialDescriptor(
            "com.camackenzie.exvi.server.database.UserDataEntry",
            new SerialDescriptor[0],
            bt -> {
                bt.element("username", Serializers.stringDescriptor(), List.of(), false);
                bt.element("workouts", Serializers.workoutArray.getDescriptor(), List.of(), false);
                bt.element("activeWorkouts", Serializers.activeWorkoutArray.getDescriptor(), List.of(), false);
                bt.element("bodyStats", Serializers.bodyStats.getDescriptor(), List.of(), false);
                return Unit.INSTANCE;
            }
    );

    /////////////////////////
    // Database helper methods
    /////////////////////////

    @NotNull
    private static UserDataEntry defaultData(@NotNull DocumentDatabase database,
                                             @NotNull String username) {
        return new UserDataEntry(database, username, new ActualWorkout[0],
                new ActualActiveWorkout[0],
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

    private void updateDatabaseRaw(@NotNull String key, @NotNull Function<UpdateItemSpec, UpdateItemSpec> spec) {
        UpdateItemSpec update = spec.apply(new UpdateItemSpec()
                .withPrimaryKey("username", username)
                .withReturnValues(ReturnValue.UPDATED_NEW));
        database.getTable("exvi-user-data").updateItem(update);
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
    // Body stats methods
    /////////////////////////

    public String getBodyStatsJSON() {
        return getUserJSON("bodyStats");
    }

    public ActualBodyStats getBodyStats() {
        return this.bodyStats = ExviSerializer.fromJson(Serializers.bodyStats, getBodyStatsJSON());
    }

    public void setBodyStats(@NotNull ActualBodyStats bs) {
        this.bodyStats = bs;
        updateDatabaseMapRaw("bodyStats", Serializers.toMap(Serializers.bodyStats, bs));
    }

    /////////////////////////
    // Workout methods
    /////////////////////////

    private static Map<String, ?> workoutToMap(ActualWorkout w) {
        return Serializers.toMap(Serializers.workout, w);
    }

    private static List<Map<String, ?>> workoutToMapList(List<ActualWorkout> list) {
        return Serializers.toMapList(Serializers.workout, list);
    }

    public String getWorkoutsJSON() {
        return getUserJSON("workouts");
    }

    public ActualWorkout[] getWorkouts() {
        return workouts = ExviSerializer.fromJson(Serializers.workoutArray, getWorkoutsJSON());
    }

    public void setWorkouts(@NotNull List<ActualWorkout> workoutList) {
        updateDatabaseListRaw("workouts", workoutToMapList(workoutList));
    }

    /**
     * Does not check if the given workouts are already present
     * just appends them to the list in the database
     *
     * @param workoutList the workouts to add
     */
    private void addWorkoutsRaw(@NotNull List<ActualWorkout> workoutList) {
        appendToDatabaseList("workouts", workoutToMapList(workoutList));
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
                    updateDatabaseMapRaw("workouts[" + userIndex + "]", workoutToMap(addedWk));
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

    private static Map<String, ?> activeWorkoutToMap(ActualActiveWorkout w) {
        return Serializers.toMap(Serializers.activeWorkout, w);
    }

    private static List<Map<String, ?>> activeWorkoutToMapList(List<ActualActiveWorkout> list) {
        return Serializers.toMapList(Serializers.activeWorkout, list);
    }

    public String getActiveWorkoutsJSON() {
        return getUserJSON("activeWorkouts");
    }

    public ActualActiveWorkout[] getActiveWorkouts() {
        return activeWorkouts = ExviSerializer.fromJson(Serializers.activeWorkoutArray, getActiveWorkoutsJSON());
    }

    public void setActiveWorkouts(@NotNull List<ActualActiveWorkout> workoutList) {
        updateDatabaseListRaw("activeWorkouts", activeWorkoutToMapList(workoutList));
    }

    private void addActiveWorkoutsRaw(@NotNull List<ActualActiveWorkout> workoutList) {
        appendToDatabaseList("activeWorkouts", activeWorkoutToMapList(workoutList));
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
                    updateDatabaseMapRaw("activeWorkouts[" + userIndex + "]", activeWorkoutToMap(userWk));
                    return Unit.INSTANCE;
                }, (addedWorkout, index) -> {
                    toAppend.add(addedWorkout);
                    return Unit.INSTANCE;
                });
        if (!toAppend.isEmpty()) addActiveWorkoutsRaw(toAppend);
    }

    /////////////////////////
    // Serializer
    /////////////////////////

    private static final KSerializer<UserDataEntry> serializer = new KSerializer<>() {

        @NotNull
        @Override
        public SerialDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public UserDataEntry deserialize(@NotNull Decoder decoder) {
            // TODO ADD DESERIALIZATION LOGIC
            return null;
        }

        @Override
        public void serialize(@NotNull Encoder encoder, UserDataEntry userDataEntry) {
            // TODO ADD SERIALIZATION LOGIC
        }
    };

}
