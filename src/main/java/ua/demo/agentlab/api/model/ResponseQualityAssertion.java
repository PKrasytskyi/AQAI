package ua.demo.agentlab.api.model;

public record ResponseQualityAssertion(
        ApiAssertionType type,
        String metric,
        long thresholdMillis
) {
    public ResponseQualityAssertion {
        type = type == null ? ApiAssertionType.RESPONSE_TIME_UNDER : type;
        metric = metric == null || metric.isBlank() ? "responseTime" : metric.trim();
        thresholdMillis = Math.max(0L, thresholdMillis);
    }
}
