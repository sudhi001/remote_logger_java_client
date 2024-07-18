package in.sudhi.lib;

import com.google.gson.Gson;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class RemoteLoggerTest {

    private static OkHttpClient mockClient;
    private static Call mockCall;
    private static RemoteLogger remoteLogger;

    @BeforeAll
    public static void setUpBeforeClass() {
        mockClient = mock(OkHttpClient.class);
        mockCall = mock(Call.class);
        RemoteLogger.setClient(mockClient);
        remoteLogger = new RemoteLogger();
    }

    @AfterAll
    public static void tearDownAfterClass() {
        RemoteLogger.shutdown();
    }

    @BeforeEach
    public void setUp() {
        when(mockClient.newCall(any(Request.class))).thenReturn(mockCall);
    }

    @Test
    public void testSendLog() throws IOException {
        Response mockResponse = new Response.Builder()
                .request(new Request.Builder().url("https://logger-server-z5w8.onrender.com/logs").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("", MediaType.get("application/json; charset=utf-8")))
                .build();

        when(mockCall.execute()).thenReturn(mockResponse);

        remoteLogger.sendLog("TestName", "TestContent");

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(mockClient, timeout(1000)).newCall(requestCaptor.capture());

        Request capturedRequest = requestCaptor.getValue();
        assertEquals("https://logger-server-z5w8.onrender.com/logs", capturedRequest.url().toString());
        assertEquals("POST", capturedRequest.method());

        RequestBody body = capturedRequest.body();
        Buffer buffer = new Buffer();
        body.writeTo(buffer);
        String bodyString = buffer.readUtf8();

        Map<String, String> expectedBody = new HashMap<>();
        expectedBody.put("name", "|TestName|" + Instant.now().toString() + "|");
        expectedBody.put("message", "TestContent");
        String expectedBodyString = new Gson().toJson(expectedBody);

        assertEquals(expectedBodyString, bodyString);
    }

    @Test
    public void testSendLog_withIOException() throws IOException {
        when(mockCall.execute()).thenThrow(new IOException("Test IOException"));

        remoteLogger.sendLog("TestName", "TestContent");

        verify(mockClient, timeout(1000)).newCall(any(Request.class));
    }
}
