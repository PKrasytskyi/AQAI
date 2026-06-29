package ua.demo.agentlab.ui.discovery.selenium.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserLogEntry;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NetworkLogCollector {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<BrowserLogEntry> collectBrowserLogs(WebDriver driver) {
        return collectLogs(driver, LogType.BROWSER);
    }

    public List<BrowserLogEntry> collectPerformanceLogs(WebDriver driver) {
        return collectLogs(driver, LogType.PERFORMANCE);
    }

    public List<BrowserNetworkCall> extractNetworkCalls(List<BrowserLogEntry> performanceLogs) {
        Map<String, MutableNetworkCall> calls = new LinkedHashMap<>();
        for (BrowserLogEntry entry : performanceLogs) {
            try {
                JsonNode message = objectMapper.readTree(entry.message()).path("message");
                String method = message.path("method").asText("");
                JsonNode params = message.path("params");
                String requestId = params.path("requestId").asText("");
                if (requestId.isBlank()) {
                    continue;
                }
                MutableNetworkCall call = calls.computeIfAbsent(requestId, MutableNetworkCall::new);
                if ("Network.requestWillBeSent".equals(method)) {
                    JsonNode request = params.path("request");
                    call.method = request.path("method").asText("");
                    call.url = request.path("url").asText("");
                    call.resourceType = params.path("type").asText("");
                } else if ("Network.responseReceived".equals(method)) {
                    JsonNode response = params.path("response");
                    call.status = response.path("status").asInt(0);
                    if (call.url.isBlank()) {
                        call.url = response.path("url").asText("");
                    }
                    if (call.resourceType.isBlank()) {
                        call.resourceType = params.path("type").asText("");
                    }
                }
            } catch (Exception ignored) {
                // Performance logs are browser-specific. Unknown entries are intentionally ignored.
            }
        }
        return calls.values().stream()
                .filter(call -> !call.url.isBlank())
                .map(call -> new BrowserNetworkCall(call.requestId, call.method, call.url, call.status, call.resourceType))
                .toList();
    }

    private List<BrowserLogEntry> collectLogs(WebDriver driver, String logType) {
        try {
            LogEntries entries = driver.manage().logs().get(logType);
            List<BrowserLogEntry> logs = new ArrayList<>();
            for (LogEntry entry : entries) {
                logs.add(new BrowserLogEntry(
                        entry.getLevel() == null ? "" : entry.getLevel().getName(),
                        entry.getMessage(),
                        entry.getTimestamp()
                ));
            }
            return logs;
        } catch (Exception exception) {
            return List.of();
        }
    }

    private static final class MutableNetworkCall {
        private final String requestId;
        private String method = "";
        private String url = "";
        private int status;
        private String resourceType = "";

        private MutableNetworkCall(String requestId) {
            this.requestId = requestId;
        }
    }
}
