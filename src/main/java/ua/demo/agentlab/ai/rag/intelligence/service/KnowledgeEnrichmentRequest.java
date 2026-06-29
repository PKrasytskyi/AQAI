package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.List;

public record KnowledgeEnrichmentRequest(
        List<SourceDocument> documents,
        List<JavaAstParseResult> javaAstResults,
        List<LayerComponentDefinition> layerComponents,
        List<ExistingTestDefinition> existingTests,
        List<ControllerRouteDefinition> controllerRoutes
) {
    public KnowledgeEnrichmentRequest {
        documents = documents == null ? List.of() : List.copyOf(documents);
        javaAstResults = javaAstResults == null ? List.of() : List.copyOf(javaAstResults);
        layerComponents = layerComponents == null ? List.of() : List.copyOf(layerComponents);
        existingTests = existingTests == null ? List.of() : List.copyOf(existingTests);
        controllerRoutes = controllerRoutes == null ? List.of() : List.copyOf(controllerRoutes);
    }
}
