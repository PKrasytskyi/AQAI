package ua.demo.agentlab.ui.discovery.selenium.model;

public record BrowserNetworkCall(
        String requestId,
        String method,
        String url,
        int status,
        String resourceType
) {
    public BrowserNetworkCall {
        requestId = requestId == null ? "" : requestId.trim();
        method = method == null ? "" : method.trim();
        url = url == null ? "" : url.trim();
        resourceType = resourceType == null ? "" : resourceType.trim();
    }
}
