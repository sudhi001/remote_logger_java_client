package in.sudhi.lib;

import okhttp3.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteLogger {
    private static OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final int THREAD_POOL_SIZE = 10; // Adjust as needed
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public static void setClient(OkHttpClient client) {
        RemoteLogger.client = client;
    }

    public static void sendLog(String name, String content) {
        executorService.submit(() -> {
            try {
                String url = "https://logger-server-z5w8.onrender.com/logs";
                String currentDateTime = Instant.now().toString();
                Map<String, String> logData = new HashMap<>();
                logData.put("name", "|" + name + "|" + currentDateTime + "|");
                logData.put("message", content);
                String jsonInputString = gson.toJson(logData);

                RequestBody body = RequestBody.create(
                        jsonInputString, MediaType.get("application/json; charset=utf-8"));

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        System.out.println("Unexpected code " + response);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error " + e.getMessage());
            }
        });
    }

    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                    System.err.println("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        sendLog("TestName", "TestContent");
        shutdown();
        // No need to explicitly call shutdown
    }
}
