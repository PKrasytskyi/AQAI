package ua.demo.agentlab.api.model;

public record ApiEndpointEvidence(
        ApiEndpointEvidenceSource source,
        String reference,
        double confidence
) {
    public ApiEndpointEvidence {
        source = source == null ? ApiEndpointEvidenceSource.MANUAL : source;
        reference = safe(reference);
        confidence = Math.max(0.0d, Math.min(1.0d, confidence));
    }

    public boolean confirmed() {
        return confidence >= 0.70d && !reference.isBlank();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
