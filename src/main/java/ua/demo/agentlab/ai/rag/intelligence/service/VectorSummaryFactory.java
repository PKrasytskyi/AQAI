package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.DtoModelDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkDetectionResult;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkType;
import ua.demo.agentlab.ai.rag.intelligence.model.InitialTestIdea;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiEndpointDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.OpenApiSpecification;
import ua.demo.agentlab.ai.rag.intelligence.model.VectorSummary;

import java.util.ArrayList;
import java.util.List;

public class VectorSummaryFactory {

    public List<VectorSummary> build(
            FrameworkDetectionResult frameworkDetection,
            List<ControllerRouteDefinition> routes,
            List<DtoModelDefinition> dtoModels,
            List<LayerComponentDefinition> layerComponents,
            List<ExistingTestDefinition> existingTests,
            List<OpenApiSpecification> openApiSpecifications,
            List<InitialTestIdea> initialTestIdeas,
            List<KnowledgeEnrichmentRecord> knowledgeEnrichments
    ) {
        List<VectorSummary> summaries = new ArrayList<>();
        for (FrameworkType frameworkType : frameworkDetection.frameworks()) {
            summaries.add(new VectorSummary(
                    "framework:" + frameworkType.name(),
                    "",
                    "framework",
                    "Detected framework " + frameworkType.name(),
                    List.of(frameworkType.name().toLowerCase())
            ));
        }
        for (ControllerRouteDefinition route : routes) {
            summaries.add(new VectorSummary(
                    "route:" + route.relativePath() + ":" + route.methodName(),
                    route.relativePath(),
                    "controller-route",
                    "Controller route " + route.httpMethod() + " " + route.fullPath()
                            + " declared in " + route.className() + "." + route.methodName(),
                    List.of(route.className(), route.httpMethod().toLowerCase(), route.fullPath())
            ));
        }
        for (DtoModelDefinition dtoModel : dtoModels) {
            summaries.add(new VectorSummary(
                    "dto:" + dtoModel.relativePath(),
                    dtoModel.relativePath(),
                    "dto-model",
                    dtoModel.kind() + " " + dtoModel.className() + " in package " + dtoModel.packageName(),
                    List.of(dtoModel.className(), dtoModel.kind().name().toLowerCase())
            ));
        }
        for (LayerComponentDefinition component : layerComponents) {
            summaries.add(new VectorSummary(
                    "component:" + component.relativePath(),
                    component.relativePath(),
                    "layer-component",
                    component.componentType() + " " + component.className() + " in package " + component.packageName(),
                    List.of(component.className(), component.componentType().name().toLowerCase())
            ));
        }
        for (ExistingTestDefinition test : existingTests) {
            summaries.add(new VectorSummary(
                    "test:" + test.relativePath(),
                    test.relativePath(),
                    "existing-test",
                    "Existing " + test.testFramework() + " test class " + test.className()
                            + " with methods " + String.join(", ", test.testMethods()),
                    List.of(test.className(), test.testFramework(), test.packageName())
            ));
        }
        for (OpenApiSpecification specification : openApiSpecifications) {
            for (OpenApiEndpointDefinition endpoint : specification.endpoints()) {
                summaries.add(new VectorSummary(
                        "openapi:" + endpoint.sourcePath() + ":" + endpoint.httpMethod() + ":" + endpoint.path(),
                        endpoint.sourcePath(),
                        "openapi-endpoint",
                        "OpenAPI endpoint " + endpoint.httpMethod() + " " + endpoint.path()
                                + " operationId=" + endpoint.operationId(),
                        List.of(endpoint.httpMethod().toLowerCase(), endpoint.path(), endpoint.operationId())
                ));
            }
        }
        for (InitialTestIdea initialTestIdea : initialTestIdeas) {
            summaries.add(new VectorSummary(
                    "idea:" + initialTestIdea.id(),
                    "",
                    "initial-test-idea",
                    initialTestIdea.title() + ". " + initialTestIdea.rationale(),
                    initialTestIdea.relatedArtifacts()
            ));
        }
        for (KnowledgeEnrichmentRecord enrichment : knowledgeEnrichments) {
            summaries.add(new VectorSummary(
                    "enrichment:" + enrichment.id(),
                    enrichment.relativePath(),
                    "knowledge-enrichment",
                    String.join(System.lineSeparator(),
                            "Code summary: " + enrichment.codeSummary(),
                            "Business intent: " + enrichment.businessIntent(),
                            "Business meaning: " + enrichment.businessMeaning(),
                            "Dependencies: " + enrichment.dependencies(),
                            "Locator stability score: " + enrichment.locatorStabilityScore(),
                            "Stable locators: " + enrichment.stableLocators(),
                            "Coverage gaps: " + enrichment.testCoverageGaps(),
                            "Failure classification: " + enrichment.failureClassifications(),
                            "Requirement traceability: " + enrichment.requirementTraceability(),
                            "Risks: " + enrichment.risks()
                    ),
                    mergeKeywords(enrichment)
            ));
        }
        return summaries;
    }

    private List<String> mergeKeywords(KnowledgeEnrichmentRecord enrichment) {
        List<String> keywords = new ArrayList<>();
        keywords.add(enrichment.className());
        if (!enrichment.methodName().isBlank()) {
            keywords.add(enrichment.methodName());
        }
        keywords.add(enrichment.enrichmentType());
        keywords.addAll(enrichment.tags());
        keywords.addAll(enrichment.failureClassifications());
        return keywords.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(30).toList();
    }
}
