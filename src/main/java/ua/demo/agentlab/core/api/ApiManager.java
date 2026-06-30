package ua.demo.agentlab.core.api;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import ua.demo.agentlab.core.config.ConfigReader;

public class ApiManager {

    private final String baseUrl;
    private final String bearerToken;

    public ApiManager() {
        this(resolveBaseUrl(), resolveBearerToken());
    }

    public ApiManager(String baseUrl, String bearerToken) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.bearerToken = bearerToken == null ? "" : bearerToken.trim();
    }

    public RequestSpecification newRequest() {
        return RestAssured
                .given()
                .baseUri(baseUrl)
                .contentType("application/json")
                .accept("application/json");
    }

    public RequestSpecification newAuthorizedRequest() {
        RequestSpecification request = newRequest();
        if (!bearerToken.isBlank()) {
            request.header("Authorization", "Bearer " + bearerToken);
        }
        return request;
    }

    public RequestSpecification newUnAuthorizedRequest() {
        return newRequest().header("Authorization", "");
    }

    private static String resolveBaseUrl() {
        return ConfigReader.getApiBaseUrl();
    }

    private static String resolveBearerToken() {
        return ConfigReader.getApiAuthToken();
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalStateException("API base URL is not configured");
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
