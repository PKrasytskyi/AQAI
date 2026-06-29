package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.DocumentFingerprint;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

public class DocumentFingerprintService {

    public List<DocumentFingerprint> build(List<SourceDocument> documents) {
        return documents.stream()
                .map(this::build)
                .toList();
    }

    public DocumentFingerprint build(SourceDocument document) {
        return new DocumentFingerprint(
                document.relativePath(),
                document.language(),
                document.content().getBytes(StandardCharsets.UTF_8).length,
                checksum(document.content())
        );
    }

    private String checksum(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate source checksum", exception);
        }
    }
}
