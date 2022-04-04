import com.amazonaws.util.StringInputStream;
import com.camackenzie.exvi.server.test.TestContext;
import com.camackenzie.exvi.server.util.AWSResourceManager;
import com.camackenzie.exvi.server.util.RequestStreamHandlerWrapper;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;

public class TestLog {

    @Test
    public void testLogging() throws IOException {
        var handler = new RequestStreamHandlerWrapper() {
            @Override
            public void handleRequestWrapped(@NotNull BufferedReader bf,
                                             @NotNull PrintWriter pw,
                                             @NotNull AWSResourceManager resourceManager) throws IOException {
                getLogger().e("Test", new Exception("Message"), "TEST TAG");
                pw.write("OUT");
            }
        };
        var inputStream = new StringInputStream("");
        var outputStream = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, outputStream, new TestContext());

        assertEquals(outputStream.toString(), "OUT");
    }

}
