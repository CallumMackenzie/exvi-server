import com.camackenzie.exvi.core.model.ActiveWorkout;
import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.database.VerificationDatabaseEntry;
import com.camackenzie.exvi.server.test.TestContext;
import com.camackenzie.exvi.server.util.AWSResourceManager;
import com.camackenzie.exvi.server.util.Serializers;
import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testSerializeUserDataEntry() {
        var rm = AWSResourceManager.get(new TestContext());
        var base = UserDataEntry.defaultData(rm.getDatabase(), "TEST");
        var ser = ExviSerializer.toJson(UserDataEntry.serializer, base);
        System.out.println(ser);
        assertTrue(ser.length() > 10);
        assertTrue(ser.contains("TEST"));
    }

    @Test
    public void testSerializeUserLoginEntry() {
        var base = new UserLoginEntry("TESTER", "asdsa", "ad", "21313", "asdsa");
        var ser = ExviSerializer.toJson(UserLoginEntry.serializer, base);
        System.out.println(ser);
        assertTrue(ser.length() > 10);
        assertTrue(ser.contains("TESTER"));
    }

    @Test
    public void testSerializeUserVerificationEntry() {
        var base = new VerificationDatabaseEntry("s", "AS0idsajd8sad7sad", "2", "dassadsa");
        var ser = ExviSerializer.toJson(VerificationDatabaseEntry.serializer, base);
        System.out.println(ser);
        assertTrue(ser.length() > 10);
        assertTrue(ser.contains("AS0idsajd8sad7sad"));
    }
}
