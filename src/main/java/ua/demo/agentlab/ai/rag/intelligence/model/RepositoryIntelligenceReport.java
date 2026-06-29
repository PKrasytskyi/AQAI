package ua.demo.agentlab.ai.rag.intelligence.model;

import java.nio.file.Path;

public record RepositoryIntelligenceReport(
        int scannedFiles,
        int warnings,
        int changedDocuments,
        int reusedDocuments,
        int controllerRoutes,
        int dtoModels,
        int layerComponents,
        int existingTests,
        int openApiEndpoints,
        int endpointMatches,
        int graphEntities,
        int graphRelations,
        int knowledgeEnrichments,
        int vectorSummaries,
        int initialTestIdeas,
        int vectorSummariesIndexed,
        Path outputDirectory
) {
}
