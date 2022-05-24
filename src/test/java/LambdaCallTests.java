import com.amazonaws.util.StringInputStream;
import com.camackenzie.exvi.core.api.*;
import com.camackenzie.exvi.core.model.ActualWorkout;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.Identifiable;
import com.camackenzie.exvi.server.lambdas.MainAction;
import com.camackenzie.exvi.server.test.TestContext;
import com.camackenzie.exvi.server.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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

// TODO: Fix test

//    @Test
//    public void testBodyHandler() throws IOException {
//        var handler = new RequestBodyHandler<>(WorkoutPutRequest.Companion.serializer(),
//                ActualWorkout.Companion.serializer()) {
//            @NotNull
//            @Override
//            public ActualWorkout handleBodyRequest(@NotNull WorkoutPutRequest workoutPutRequest) {
//                return workoutPutRequest.getWorkouts()[0];
//            }
//        };
//
//        var input = new WorkoutPutRequest("", "", new ActualWorkout[]{
//                new ActualWorkout("name", "desc", new ArrayList<>(), Identifiable.generateId())
//        });
//        var inputStream = new StringInputStream("{\"body\":" + input.toJson() + "}");
//        var outputStream = new ByteArrayOutputStream();
//        handler.handleRequest(inputStream, outputStream, new TestContext());
//    }

    @Test
    public void testGenericDataReq() throws IOException {
        var handler = new RequestBodyHandler<>(GenericDataRequest.Companion.serializer(),
                GenericDataResult.Companion.serializer()) {
            @NotNull
            @Override
            public GenericDataResult handleBodyRequest(@NotNull GenericDataRequest genericDataRequest) {
                if (genericDataRequest instanceof WorkoutListRequest) {
                    var request = (WorkoutListRequest) genericDataRequest;
                    assertEquals(request.getUsername().get(), "name");
                    return new WorkoutListResult(new ActualWorkout[0]);
                } else if (genericDataRequest instanceof VerificationRequest) {
                    return new AccountAccessKeyResult("");
                }
                throw new ApiException(0, "");
            }
        };

        var o1 = RequestTester.testRequest(handler, new APIRequest<>(
                new WorkoutListRequest("name", "", WorkoutListRequest.Type.ListAllTemplates)
        ).toJson(WorkoutListRequest.Companion.serializer()));

        System.out.println(o1);

        var fn = ExviSerializer.fromJson(APIResult.Companion.serializer(
                Serializers.string
        ), o1);

        var decodedBody = APIResult.decodeBody(fn.getBody());
        System.out.println(decodedBody);
        var result = ExviSerializer.fromJson(GenericDataResult.Companion.serializer(), decodedBody);
        assertTrue(result instanceof WorkoutListResult);
        assertEquals(((WorkoutListResult) result).getWorkouts().length, 0);
    }

    @Test
    public void testLoginAction() {
        var loginAction = new MainAction();

        APIResult<String> output = RequestTester.testAPIRequest(loginAction, new LoginRequest(
                "", ""
        ), LoginRequest.Companion.serializer());
        assertEquals(output.getStatusCode(), 400);

        output = RequestTester.testAPIRequest(loginAction, new LoginRequest("sdakdsdkja", ""),
                LoginRequest.Companion.serializer());
        assertEquals(output.getStatusCode(), 400);
    }

    @Test
    public void testRetrieveSaltAction() {
        var action = new MainAction();

        APIResult<String> output = RequestTester.testAPIRequest(action, new RetrieveSaltRequest(
                ""
        ), RetrieveSaltRequest.Companion.serializer());
        assertEquals(output.getStatusCode(), 400);
    }

}
