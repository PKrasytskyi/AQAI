package ua.demo.agentlab.ai.rag.intelligence.service;

import java.nio.file.Path;

public record RepositoryIntelligenceConfig(
        boolean includeGeneratedArtifacts,
        int maxPreviewChars,
        Path outputDirectory,
        boolean enableIncrementalIndexing
) {
    public RepositoryIntelligenceConfig {
        maxPreviewChars = maxPreviewChars <= 0 ? 220 : maxPreviewChars;
        if (outputDirectory == null) {
            outputDirectory = Path.of("target/repository-intelligence").toAbsolutePath().normalize();
        } else {
            outputDirectory = outputDirectory.toAbsolutePath().normalize();
        }
    }

    public static RepositoryIntelligenceConfig defaults(Path outputDirectory) {
        return new RepositoryIntelligenceConfig(false, 220, outputDirectory, true);
    }
}
