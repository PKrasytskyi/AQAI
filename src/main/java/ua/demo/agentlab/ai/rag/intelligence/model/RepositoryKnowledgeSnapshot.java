package ua.demo.agentlab.ai.rag.intelligence.model;

import java.util.List;

public record RepositoryKnowledgeSnapshot(
        FrameworkDetectionResult frameworkDetection,
        ParsingDiagnostics parsingDiagnostics,
        List<SourceInventoryItem> sourceInventory,
        List<DocumentFingerprint> documentFingerprints,
        List<JavaAstParseResult> javaAstResults,
        List<ControllerRouteDefinition> controllerRoutes,
        List<DtoModelDefinition> dtoModels,
        List<LayerComponentDefinition> layerComponents,
        List<ExistingTestDefinition> existingTests,
        List<OpenApiSpecification> openApiSpecifications,
        List<EndpointMatch> endpointMatches,
        List<RepositoryGraphEntity> graphEntities,
        List<RepositoryGraphRelation> graphRelations,
        List<KnowledgeEnrichmentRecord> knowledgeEnrichments,
        List<VectorSummary> vectorSummaries,
        List<InitialTestIdea> initialTestIdeas
) {
    public RepositoryKnowledgeSnapshot {
        sourceInventory = sourceInventory == null ? List.of() : List.copyOf(sourceInventory);
        documentFingerprints = documentFingerprints == null ? List.of() : List.copyOf(documentFingerprints);
        javaAstResults = javaAstResults == null ? List.of() : List.copyOf(javaAstResults);
        controllerRoutes = controllerRoutes == null ? List.of() : List.copyOf(controllerRoutes);
        dtoModels = dtoModels == null ? List.of() : List.copyOf(dtoModels);
        layerComponents = layerComponents == null ? List.of() : List.copyOf(layerComponents);
        existingTests = existingTests == null ? List.of() : List.copyOf(existingTests);
        openApiSpecifications = openApiSpecifications == null ? List.of() : List.copyOf(openApiSpecifications);
        endpointMatches = endpointMatches == null ? List.of() : List.copyOf(endpointMatches);
        graphEntities = graphEntities == null ? List.of() : List.copyOf(graphEntities);
        graphRelations = graphRelations == null ? List.of() : List.copyOf(graphRelations);
        knowledgeEnrichments = knowledgeEnrichments == null ? List.of() : List.copyOf(knowledgeEnrichments);
        vectorSummaries = vectorSummaries == null ? List.of() : List.copyOf(vectorSummaries);
        initialTestIdeas = initialTestIdeas == null ? List.of() : List.copyOf(initialTestIdeas);
    }
}
