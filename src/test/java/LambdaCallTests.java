import com.amazonaws.util.StringInputStream;
import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.server.test.TestContext;
import com.camackenzie.exvi.server.util.AWSResourceManager;
import com.camackenzie.exvi.server.util.RequestBodyHandler;
import com.camackenzie.exvi.server.util.RequestStreamHandlerWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LambdaCallTests {

    @Test
    public void testRawWrapper() throws IOException {
        var handler = new RequestStreamHandlerWrapper() {
            @Override
            public void handleRequestWrapped(@NotNull BufferedReader bf,
                                             @NotNull PrintWriter pw,
                                             @NotNull AWSResourceManager resourceManager) throws IOException {
                pw.write(bf.lines().collect(Collectors.joining()));
            }
        };

        var inputStream = new StringInputStream("TEST");
        var outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, new TestContext());

        assertEquals(outputStream.toString(), "TEST");
    }

    @Test
    public void testBodyHandler() throws IOException {
        var handler = new RequestBodyHandler<WorkoutPutRequest, ActualWorkout>(WorkoutPutRequest.class) {
            @NotNull
            @Override
            public ActualWorkout handleBodyRequest(@NotNull WorkoutPutRequest workoutPutRequest) {
                return workoutPutRequest.getWorkouts()[0];
            }
        };

        var input = new WorkoutPutRequest("", "", new ActualWorkout[]{
                new ActualWorkout("name", "desc", new ArrayList(), Identifiable.generateId())
        });
        var inputStream = new StringInputStream("{\"body\":" + input.toJson() + "}");
        var outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, new TestContext());
    }

    @Test
    public void testGenericDataReq() throws IOException {
        var handler = new RequestBodyHandler<GenericDataRequest, GenericDataResult>(GenericDataRequest.class) {
            @NotNull
            @Override
            public GenericDataResult handleBodyRequest(@NotNull GenericDataRequest genericDataRequest) {
                switch (genericDataRequest.getRequester().get()) {
                    case WorkoutListRequest.uid: {
                        var request = getRequestBodyAs(WorkoutListRequest.Companion.serializer());
                        assertEquals(request.getUsername().get(), "name");
                        return new WorkoutListResult(new ActualWorkout[0]);
                    }
                    default:
                        break;
                }
                return null;
            }
        };

        Function<String, String> testInput = in -> {
            try {
                StringInputStream inputStream = new StringInputStream(in);
                var outputStream = new ByteArrayOutputStream();
                handler.handleRequest(inputStream, outputStream, new TestContext());
                var ret = String.valueOf(outputStream);
                outputStream.close();
                inputStream.close();
                return ret;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };

        var o1 = testInput.apply("{\"body\":"
                + new WorkoutListRequest("name", "", WorkoutListRequest.Type.ListAllTemplates).toJson()
                + "}");

        var fn = handler.getGson().fromJson(o1, APIResult.class);
        assertTrue(fn.getBody() instanceof String);
        var decodedBody = APIResult.decodeBody((String) fn.getBody());
        System.out.println(decodedBody);
        var result = ExviSerializer.INSTANCE.fromJson(WorkoutListResult.Companion.serializer(), decodedBody);
        assertEquals(result.getWorkouts().length, 0);
    }

}
