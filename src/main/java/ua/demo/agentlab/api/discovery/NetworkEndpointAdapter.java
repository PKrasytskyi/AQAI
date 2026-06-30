package ua.demo.agentlab.api.discovery;

import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.ApiEndpointEvidence;
import ua.demo.agentlab.api.model.ApiEndpointEvidenceSource;
import ua.demo.agentlab.api.model.ApiEndpointModel;
import ua.demo.agentlab.api.model.ApiResponseModel;
import ua.demo.agentlab.api.model.HttpMethod;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NetworkEndpointAdapter {

    public ApiEndpointBundle adapt(SeleniumDiscoveryResult discoveryResult) {
        if (discoveryResult == null || discoveryResult.pages().isEmpty()) {
            return new ApiEndpointBundle("network-scan", List.of());
        }
        String baseHost = host(discoveryResult.baseUrl());
        Map<String, ApiEndpointModel> endpoints = new LinkedHashMap<>();
        for (DiscoveredPageSnapshot page : discoveryResult.pages()) {
            if (page.rawPageSnapshot() == null) {
                continue;
            }
            for (BrowserNetworkCall call : page.rawPageSnapshot().networkCalls()) {
                if (!apiCandidate(call, baseHost)) {
                    continue;
                }
                ApiEndpointModel endpoint = endpoint(call, page.pageId());
                endpoints.merge(endpoint.endpointId(), endpoint, this::mergeNetworkEndpoint);
            }
        }
        return new ApiEndpointBundle("network-scan", new ArrayList<>(endpoints.values()));
    }

    private ApiEndpointModel mergeNetworkEndpoint(ApiEndpointModel left, ApiEndpointModel right) {
        List<ApiResponseModel> responses = new ArrayList<>(left.responses());
        for (ApiResponseModel response : right.responses()) {
            if (responses.stream().noneMatch(existing -> existing.statusCode() == response.statusCode())) {
                responses.add(response);
            }
        }
        List<ApiEndpointEvidence> evidence = new ArrayList<>(left.evidence());
        evidence.addAll(right.evidence());
        return new ApiEndpointModel(
                left.endpointId(),
                left.method(),
                left.path(),
                left.operationName(),
                left.businessCapability(),
                left.parameters(),
                left.requestBody(),
                responses,
                left.authRequirements(),
                evidence,
                Math.max(left.confidence(), right.confidence())
        );
    }

    private ApiEndpointModel endpoint(BrowserNetworkCall call, String pageId) {
        HttpMethod method = parseMethod(call.method());
        String path = path(call.url());
        return new ApiEndpointModel(
                endpointId(method, path),
                method,
                path,
                "",
                "",
                List.of(),
                null,
                call.status() > 0 ? List.of(new ApiResponseModel(call.status(), "", "", List.of())) : List.of(),
                List.of(),
                List.of(new ApiEndpointEvidence(ApiEndpointEvidenceSource.NETWORK_SCAN, pageId + ":" + call.url(), 0.78d)),
                0.78d
        );
    }

    private boolean apiCandidate(BrowserNetworkCall call, String baseHost) {
        if (call == null || call.url().isBlank()) {
            return false;
        }
        String host = host(call.url());
        if (!baseHost.isBlank() && !host.isBlank() && !baseHost.equalsIgnoreCase(host)) {
            return false;
        }
        String resourceType = call.resourceType().toLowerCase(Locale.ROOT);
        String path = path(call.url()).toLowerCase(Locale.ROOT);
        return resourceType.contains("xhr")
                || resourceType.contains("fetch")
                || path.contains("/api/")
                || path.contains("/public/")
                || path.endsWith(".json");
    }

    private HttpMethod parseMethod(String value) {
        try {
            return HttpMethod.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return HttpMethod.GET;
        }
    }

    private String endpointId(HttpMethod method, String path) {
        return method.name() + ":" + path;
    }

    private String host(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String path(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            return path.startsWith("/") ? path : "/" + path;
        } catch (Exception ignored) {
            String value = url == null ? "" : url.trim();
            return value.startsWith("/") ? value : "/" + value;
        }
    }
}
