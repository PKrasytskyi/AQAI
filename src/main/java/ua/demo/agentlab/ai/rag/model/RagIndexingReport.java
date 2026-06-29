package ua.demo.agentlab.ai.rag.model;

import java.nio.file.Path;

public record RagIndexingReport(
        int documentsIndexed,
        int chunksIndexed,
        int vectorSize,
        String collectionName,
        Path chunksJsonlPath
) {
}
