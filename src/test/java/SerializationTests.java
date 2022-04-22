import com.camackenzie.exvi.core.model.ActiveWorkout;
import com.camackenzie.exvi.core.model.ActualActiveWorkout;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.server.database.UserDataEntry;
import com.camackenzie.exvi.server.database.UserLoginEntry;
import com.camackenzie.exvi.server.database.UserVerificationEntry;
import com.camackenzie.exvi.server.test.TestContext;
import com.camackenzie.exvi.server.util.AWSResourceManager;
import com.camackenzie.exvi.server.util.Serializers;
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
        var exviSerialized = ExviSerializer.toJson(Serializers.workoutArray, og);
        var deserializedExvi = ExviSerializer.fromJson(Serializers.workoutArray, exviSerialized);

        assertEquals(og.length, deserializedExvi.length);
        assertEquals(og[0].getName(), deserializedExvi[0].getName());
        assertEquals(og[2].getId().get(), deserializedExvi[2].getId().get());
    }

    @Test
    public void testSerializeActiveWorkoutArray() {
        var base = new ActualWorkout();
        var og = new ActualActiveWorkout[]{
                ActiveWorkout.invoke(base),
                ActiveWorkout.invoke(base)
        };
        var exviSerialized = ExviSerializer.toJson(Serializers.activeWorkoutArray, og);
        var deserializedExvi = ExviSerializer.fromJson(Serializers.activeWorkoutArray, exviSerialized);

        assertEquals(og.length, deserializedExvi.length);
        assertEquals(og[0].getName(), deserializedExvi[0].getName());
        assertEquals(og[1].getActiveWorkoutId().get(), deserializedExvi[1].getActiveWorkoutId().get());
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
        base.addAccessKey("DJSAIKDJK");
        var ser = ExviSerializer.toJson(UserLoginEntry.serializer, base);
        System.out.println(ser);
        assertTrue(ser.length() > 10);
        assertTrue(ser.contains("TESTER"));

        var des = ExviSerializer.fromJson(UserLoginEntry.serializer, ser);
        assertEquals(des.email, base.email);
        assertEquals(des.username, base.username);
        assertEquals(des.getAccessKeys()[0], base.getAccessKeys()[0]);
    }

    @Test
    public void testSerializeUserVerificationEntry() {
        var base = new UserVerificationEntry("s", "AS0idsajd8sad7sad", "2", "dassadsa");
        var ser = ExviSerializer.toJson(UserVerificationEntry.serializer, base);
        System.out.println(ser);
        assertTrue(ser.length() > 10);
        assertTrue(ser.contains("AS0idsajd8sad7sad"));

        var des = ExviSerializer.fromJson(UserVerificationEntry.serializer, ser);
        assertEquals(base.getEmail(), des.getEmail());
        assertEquals(base.getVerificationCode(), des.getVerificationCode());
    }
}
