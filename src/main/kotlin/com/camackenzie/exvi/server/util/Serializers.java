package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualBodyStats;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import kotlinx.serialization.KSerializer;

import java.util.List;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ArraySerializer;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ListSerializer;

public class Serializers {

    public static final KSerializer<ActualWorkout> workout = ActualWorkout.Companion.serializer();
    public static final KSerializer<ActualActiveWorkout> activeWorkout = ActualActiveWorkout.Companion.serializer();

    public static final KSerializer<ActualWorkout[]> workoutArray =
            ArraySerializer(getKotlinClass(ActualWorkout.class), workout);

    public static final KSerializer<ActualActiveWorkout[]> activeWorkoutArray =
            ArraySerializer(getKotlinClass(ActualActiveWorkout.class), activeWorkout);

    public static final KSerializer<List<ActualWorkout>> workoutList = ListSerializer(workout);
    public static final KSerializer<List<ActualActiveWorkout>> activeWorkoutList = ListSerializer(activeWorkout);

    public static final KSerializer<ActualBodyStats> bodyStats = ActualBodyStats.Companion.serializer();

    public static final KSerializer<String> string = ExviSerializer.Builtin.getString();
    public static final KSerializer<String[]> stringArray = ArraySerializer(getKotlinClass(String.class), string);
    public static final KSerializer<Long> longType = ExviSerializer.Builtin.getLong();

}
