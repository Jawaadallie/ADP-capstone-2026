package Capstone.net;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.swing.SwingWorker;
import java.net.ConnectException;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.function.Consumer;


public final class Api {

    public static final String BASE = "http://localhost:8080/api";

    // The backend checks the signed-in customer's email + password (HTTP Basic) on every request.
    // null while nobody is signed in.
    private static volatile String authHeader;

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    // Keeps the server's session cookie, so the password is only checked in full once per sign-in.
    private static volatile HttpClient client = newClient();

    private static HttpClient newClient() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    private Api() {}

    public static class ApiException extends Exception {
        private final int status;

        public ApiException(String message, int status) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }


    public static void setCredentials(String email, String password) {
        String login = email.trim().toLowerCase() + ":" + password;
        authHeader = "Basic " + Base64.getEncoder()
                .encodeToString(login.getBytes(StandardCharsets.UTF_8));
        client = newClient();
    }


    public static void clearCredentials() {
        authHeader = null;
        client = newClient();
    }

    public static <T> T get(String path, Class<T> type) throws Exception {
        return read(send("GET", path, null), type);
    }

    public static <T> T get(String path, TypeReference<T> type) throws Exception {
        String body = send("GET", path, null);
        if (body == null || body.isBlank()) {
            return null;
        }
        return MAPPER.readValue(body, type);
    }

    public static <T> T post(String path, Object payload, Class<T> type) throws Exception {
        return read(send("POST", path, payload), type);
    }

    public static void post(String path, Object payload) throws Exception {
        send("POST", path, payload);
    }

    public static void put(String path, Object payload) throws Exception {
        send("PUT", path, payload);
    }

    public static void delete(String path) throws Exception {
        send("DELETE", path, null);
    }

    private static <T> T read(String body, Class<T> type) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        return MAPPER.readValue(body, type);
    }


    public static String send(String method, String path, Object payload) throws Exception {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json");

        String auth = authHeader;
        if (auth != null) {
            builder.header("Authorization", auth);
        }

        HttpRequest.BodyPublisher body = payload == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload));

        builder.method(method, body);

        HttpResponse<String> response;
        try {
            response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (ConnectException e) {
            throw new ApiException(
                    "Cannot reach the server. Is the backend running on port 8080?", 0);
        }

        int status = response.statusCode();
        if (status >= 400) {
            throw new ApiException(describe(status, method, path, response.body()), status);
        }
        return response.body();
    }

    private static String serverMessage(String body) {
        try {
            if (body == null || body.isBlank()) {
                return null;
            }
            String message = MAPPER.readTree(body).path("message").asText("");
            return message.isBlank() ? null : message;
        } catch (Exception e) {
            return null;
        }
    }

    private static String describe(int status, String method, String path, String body) {
        String message = serverMessage(body);
        switch (status) {
            case 401:
                return "Incorrect email or password (401).";
            case 404:
                return "Not found (404): " + path;
            case 409:
                return message != null ? message : "That already exists (409).";
            case 400:
                return message != null ? message : "The server could not accept that data (400).";
            default:
                return "Server error " + status + " on " + method + " " + path
                        + ". Check the backend console for details.";
        }
    }



    public static <T> void async(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    onError.accept(cause);
                }
            }
        }.execute();
    }
}
