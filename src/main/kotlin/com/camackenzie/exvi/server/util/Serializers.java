package com.camackenzie.exvi.server.util;

import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualBodyStats;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.SelfSerializable;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerializationStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static kotlin.jvm.JvmClassMappingKt.getKotlinClass;
import static kotlinx.serialization.builtins.BuiltinSerializersKt.ArraySerializer;
import static kotlinx.serialization.json.JsonElementKt.getJsonObject;

public class Serializers {

    public static final KSerializer<ActualWorkout> workout = ActualWorkout.Companion.serializer();
    public static final KSerializer<ActualActiveWorkout> activeWorkout = ActualActiveWorkout.Companion.serializer();

    public static final KSerializer<ActualWorkout[]> workoutArray =
            ArraySerializer(getKotlinClass(ActualWorkout.class), workout);

    public static final KSerializer<ActualActiveWorkout[]> activeWorkoutArray =
            ArraySerializer(getKotlinClass(ActualActiveWorkout.class), activeWorkout);

    public static final KSerializer<ActualBodyStats> bodyStats = ActualBodyStats.Companion.serializer();

    public static final KSerializer<String> string = ExviSerializer.Builtin.getString();
    public static final KSerializer<String[]> stringArrat = ArraySerializer(getKotlinClass(String.class), string);
    public static final KSerializer<Long> longType = ExviSerializer.Builtin.getLong();


    @NotNull
    public static <T> Map<String, ?> toMap(@NotNull SerializationStrategy<T> inSerializer, @NotNull T in) {
        return getJsonObject(ExviSerializer.toJsonElement(inSerializer, in));
    }

    @NotNull
    public static <T extends SelfSerializable> List<Map<String, ?>> toMapList(@NotNull SerializationStrategy<T> inSerializer, @NotNull List<T> l) {
        return new ArrayList<>() {{
            for (var li : l) {
                add(toMap(inSerializer, li));
            }
        }};
    }

}
