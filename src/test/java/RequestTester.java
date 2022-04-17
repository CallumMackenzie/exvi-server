import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.StringInputStream;
import com.camackenzie.exvi.core.api.APIRequest;
import com.camackenzie.exvi.core.api.APIResult;
import com.camackenzie.exvi.core.model.ExviSerializer;
import com.camackenzie.exvi.core.util.SelfSerializable;
import com.camackenzie.exvi.server.test.TestContext;
import kotlinx.serialization.KSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RequestTester {

    public static <IN extends SelfSerializable>
    APIResult<String> testAPIRequest(RequestStreamHandler handler, IN in,
                       KSerializer<IN> inSerializer) {
        var request = new APIRequest<>("", in);
        var serialized = testRequest(handler, request.toJson(inSerializer));
        APIResult<String> og = APIResult.fromJson(serialized);
        String body = APIResult.decodeBody(og.getBody());
        return new APIResult<>(og, body);
    }

    public static String testRequest(RequestStreamHandler handler, String in) {
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
            return null;
        }
    }

}
