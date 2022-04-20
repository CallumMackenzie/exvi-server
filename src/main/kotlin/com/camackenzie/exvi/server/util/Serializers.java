package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.SelfSerializable;
import kotlinx.serialization.KSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ArraySerializer;
import static kotlinx.serialization.json.JsonElementKt.getJsonObject;

public class Serializers {

    public static final KSerializer<ActualWorkout[]> workoutArray =
            ArraySerializer(getKotlinClass(ActualWorkout.class), ActualWorkout.Companion.serializer());

    public static final KSerializer<ActualActiveWorkout[]> activeWorkoutArray =
            ArraySerializer(getKotlinClass(ActualActiveWorkout.class), ActualActiveWorkout.Companion.serializer());


    @NotNull
    public static <T> Map<String, ?> toMap(@NotNull KSerializer<T> inSerializer, @NotNull T in) {
        return getJsonObject(ExviSerializer.toJsonElement(inSerializer, in));
    }

    @NotNull
    public static <T extends SelfSerializable> List<Map<String, ?>> toMapList(@NotNull KSerializer<T> inSerializer, @NotNull List<T> l) {
        return new ArrayList<>() {{
            for (var li : l) {
                add(toMap(inSerializer, li));
            }
        }};
    }

}
