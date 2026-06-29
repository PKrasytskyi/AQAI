package ua.demo.agentlab.ai.rag.intelligence.model;

public record DocumentFingerprint(
        String relativePath,
        String language,
        long sizeBytes,
        String checksum
) {
    public DocumentFingerprint {
        relativePath = relativePath == null ? "" : relativePath.trim();
        language = language == null ? "" : language.trim();
        checksum = checksum == null ? "" : checksum.trim();
        sizeBytes = Math.max(0L, sizeBytes);
    }
}
