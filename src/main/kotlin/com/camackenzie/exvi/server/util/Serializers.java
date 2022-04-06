package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualWorkout;
import kotlinx.serialization.KSerializer;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ArraySerializer;

public class Serializers {

    public static final KSerializer<ActualWorkout[]> workoutArray =
            ArraySerializer(getKotlinClass(ActualWorkout.class), ActualWorkout.Companion.serializer());

    public static final KSerializer<ActualActiveWorkout[]> activeWorkoutArray =
            ArraySerializer(getKotlinClass(ActualActiveWorkout.class), ActualActiveWorkout.Companion.serializer());

}
