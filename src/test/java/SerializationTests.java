import com.camackenzie.exvi.core.model.ActiveWorkout;
import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.server.util.Serializers;
import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SerializationTests {
    @Test
    public void testSerializeWorkoutArray() {
        var og = new ActualWorkout[]{
                new ActualWorkout(),
                new ActualWorkout(),
                new ActualWorkout()
        };
        Gson gson = new Gson();
        var gsonSerialized = gson.toJson(og);
        var exviSerialized = ExviSerializer.toJson(Serializers.workoutArray, og);
        var deserializedGson = ExviSerializer.fromJson(Serializers.workoutArray, gsonSerialized);
        var deserializedExvi = ExviSerializer.fromJson(Serializers.workoutArray, exviSerialized);

        assertEquals(deserializedGson.length, og.length, deserializedExvi.length);
        assertEquals(deserializedGson[0].getName(), og[0].getName(), deserializedExvi[0].getName());
        assertEquals(deserializedGson[2].getId().get(), og[2].getId().get(),
                deserializedExvi[2].getId().get());
    }

    @Test
    public void testSerializeActiveWorkoutArray() {
        var base = new ActualWorkout();
        var og = new ActualActiveWorkout[]{
                ActiveWorkout.invoke(base),
                ActiveWorkout.invoke(base)
        };
        Gson gson = new Gson();
        var gsonSerialized = gson.toJson(og);
        var exviSerialized = ExviSerializer.toJson(Serializers.activeWorkoutArray, og);
        var deserializedGson = ExviSerializer.fromJson(Serializers.activeWorkoutArray, gsonSerialized);
        var deserializedExvi = ExviSerializer.fromJson(Serializers.activeWorkoutArray, exviSerialized);

        assertEquals(deserializedGson.length, og.length, deserializedExvi.length);
        assertEquals(deserializedGson[0].getName(), og[0].getName(), deserializedExvi[0].getName());
        assertEquals(deserializedGson[1].getActiveWorkoutId().get(), og[1].getActiveWorkoutId().get(),
                deserializedExvi[1].getActiveWorkoutId().get());
    }
}
