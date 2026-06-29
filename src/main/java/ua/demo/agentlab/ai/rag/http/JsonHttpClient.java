package ua.demo.agentlab.ai.rag.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class JsonHttpClient {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public JsonHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(DEFAULT_TIMEOUT).build(), new ObjectMapper());
    }

    public JsonHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        if (httpClient == null) {
            throw new IllegalArgumentException("httpClient cannot be null");
        }
        if (objectMapper == null) {
            throw new IllegalArgumentException("objectMapper cannot be null");
        }
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode get(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .GET();
        applyHeaders(builder, headers);
        return send(builder.build());
    }

    public JsonNode put(String url, Object body, Map<String, String> headers) {
        return sendWithBody("PUT", url, body, headers);
    }

    public JsonNode post(String url, Object body, Map<String, String> headers) {
        return sendWithBody("POST", url, body, headers);
    }

    private JsonNode sendWithBody(String method, String url, Object body, Map<String, String> headers) {
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("Content-Type", "application/json");
            applyHeaders(builder, headers);
            if ("PUT".equalsIgnoreCase(method)) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.POST(HttpRequest.BodyPublishers.ofString(json));
            }
            return send(builder.build());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize JSON body for " + method + " " + url, exception);
        }
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP request failed with status " + response.statusCode()
                        + " for " + request.uri() + ": " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("HTTP request failed for " + request.uri(), exception);
        } catch (IOException exception) {
            throw new IllegalStateException("HTTP request failed for " + request.uri(), exception);
        }
    }

    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> headers) {
        if (headers == null) {
            return;
        }
        headers.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                builder.header(key, value);
            }
        });
    }
}
