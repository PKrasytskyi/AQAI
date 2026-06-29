package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.graph.CodeGraphEdge;
import ua.demo.agentlab.ai.rag.graph.CodeGraphNode;
import ua.demo.agentlab.ai.rag.graph.GraphNodeType;
import ua.demo.agentlab.ai.rag.graph.ProjectCodeGraph;
import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.EndpointMatch;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkDetectionResult;
import ua.demo.agentlab.ai.rag.intelligence.model.FrameworkType;
import ua.demo.agentlab.ai.rag.intelligence.model.DtoModelDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntity;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntityType;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelation;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelationType;

import java.util.ArrayList;
import java.util.List;

public class KnowledgeGraphBuilder {

    public List<RepositoryGraphEntity> buildEntities(
            FrameworkDetectionResult frameworkDetection,
            ProjectCodeGraph projectCodeGraph,
            List<ControllerRouteDefinition> controllerRoutes,
            List<EndpointMatch> endpointMatches,
            List<DtoModelDefinition> dtoModels,
            List<LayerComponentDefinition> layerComponents,
            List<ExistingTestDefinition> existingTests,
            List<KnowledgeEnrichmentRecord> knowledgeEnrichments
    ) {
        List<RepositoryGraphEntity> entities = new ArrayList<>();
        for (FrameworkType frameworkType : frameworkDetection.frameworks()) {
            entities.add(new RepositoryGraphEntity(
                    "framework:" + frameworkType.name(),
                    RepositoryGraphEntityType.FRAMEWORK,
                    frameworkType.name(),
                    "",
                    List.of("framework")
            ));
        }
        for (CodeGraphNode node : projectCodeGraph.nodes()) {
            entities.add(new RepositoryGraphEntity(
                    node.id(),
                    map(node.nodeType()),
                    node.displayName(),
                    node.relativePath(),
                    node.tags()
            ));
        }
        for (ControllerRouteDefinition route : controllerRoutes) {
            entities.add(new RepositoryGraphEntity(
                    route.relativePath() + "#route:" + route.httpMethod() + ":" + route.fullPath(),
                    RepositoryGraphEntityType.ROUTE,
                    route.httpMethod() + " " + route.fullPath(),
                    route.relativePath(),
                    List.of("route", route.httpMethod().toLowerCase())
            ));
        }
        endpointMatches.stream()
                .map(EndpointMatch::openApiEndpoint)
                .filter(java.util.Objects::nonNull)
                .forEach(endpoint -> entities.add(new RepositoryGraphEntity(
                        endpoint.sourcePath() + "#openapi:" + endpoint.httpMethod() + ":" + endpoint.path(),
                        RepositoryGraphEntityType.OPENAPI_ENDPOINT,
                        endpoint.httpMethod() + " " + endpoint.path(),
                        endpoint.sourcePath(),
                        List.of("openapi", endpoint.httpMethod().toLowerCase())
                )));
        for (DtoModelDefinition dtoModel : dtoModels) {
            entities.add(new RepositoryGraphEntity(
                    dtoModel.relativePath() + "#dto",
                    RepositoryGraphEntityType.DTO_MODEL,
                    dtoModel.className(),
                    dtoModel.relativePath(),
                    List.of(dtoModel.kind().name().toLowerCase())
            ));
        }
        for (LayerComponentDefinition layerComponent : layerComponents) {
            entities.add(new RepositoryGraphEntity(
                    layerComponent.relativePath() + "#component",
                    RepositoryGraphEntityType.SERVICE_COMPONENT,
                    layerComponent.className(),
                    layerComponent.relativePath(),
                    List.of(layerComponent.componentType().name().toLowerCase())
            ));
        }
        for (ExistingTestDefinition existingTest : existingTests) {
            entities.add(new RepositoryGraphEntity(
                    existingTest.relativePath() + "#test",
                    RepositoryGraphEntityType.TEST_CLASS,
                    existingTest.className(),
                    existingTest.relativePath(),
                    List.of(existingTest.testFramework().toLowerCase())
            ));
        }
        for (KnowledgeEnrichmentRecord enrichment : knowledgeEnrichments) {
            entities.add(new RepositoryGraphEntity(
                    enrichment.id(),
                    RepositoryGraphEntityType.KNOWLEDGE_ENRICHMENT,
                    enrichment.className() + (enrichment.methodName().isBlank() ? "" : "." + enrichment.methodName()),
                    enrichment.relativePath(),
                    enrichment.tags()
            ));
        }
        return entities;
    }

    public List<RepositoryGraphRelation> buildRelations(
            FrameworkDetectionResult frameworkDetection,
            ProjectCodeGraph projectCodeGraph,
            List<ControllerRouteDefinition> controllerRoutes,
            List<EndpointMatch> endpointMatches,
            List<KnowledgeEnrichmentRecord> knowledgeEnrichments
    ) {
        List<RepositoryGraphRelation> relations = new ArrayList<>();
        for (CodeGraphEdge edge : projectCodeGraph.edges()) {
            relations.add(new RepositoryGraphRelation(
                    edge.fromNodeId(),
                    edge.toNodeId(),
                    map(edge.edgeType().name())
            ));
        }
        for (CodeGraphNode node : projectCodeGraph.nodes()) {
            for (FrameworkType frameworkType : frameworkDetection.frameworks()) {
                relations.add(new RepositoryGraphRelation(
                        "framework:" + frameworkType.name(),
                        node.id(),
                        RepositoryGraphRelationType.DETECTED_AS
                ));
            }
        }
        for (ControllerRouteDefinition route : controllerRoutes) {
            relations.add(new RepositoryGraphRelation(
                    route.relativePath(),
                    route.relativePath() + "#route:" + route.httpMethod() + ":" + route.fullPath(),
                    RepositoryGraphRelationType.DECLARES_ROUTE
            ));
        }
        for (EndpointMatch endpointMatch : endpointMatches) {
            if (!endpointMatch.matched()) {
                continue;
            }
            relations.add(new RepositoryGraphRelation(
                    endpointMatch.controllerRoute().relativePath() + "#route:"
                            + endpointMatch.controllerRoute().httpMethod() + ":" + endpointMatch.controllerRoute().fullPath(),
                    endpointMatch.openApiEndpoint().sourcePath() + "#openapi:"
                            + endpointMatch.openApiEndpoint().httpMethod() + ":" + endpointMatch.openApiEndpoint().path(),
                    RepositoryGraphRelationType.MATCHES_OPENAPI
            ));
        }
        for (KnowledgeEnrichmentRecord enrichment : knowledgeEnrichments) {
            relations.add(new RepositoryGraphRelation(
                    enrichment.id(),
                    enrichment.relativePath(),
                    RepositoryGraphRelationType.ENRICHES
            ));
            for (String trace : enrichment.requirementTraceability()) {
                if (trace.toLowerCase(java.util.Locale.ROOT).startsWith("requirement ")) {
                    relations.add(new RepositoryGraphRelation(
                            enrichment.id(),
                            trace,
                            RepositoryGraphRelationType.TRACES_TO
                    ));
                }
            }
        }
        return relations;
    }

    private RepositoryGraphEntityType map(GraphNodeType nodeType) {
        return switch (nodeType) {
            case PAGE_OBJECT, API_CLIENT, BASE_CLASS, UTILITY, METHOD, UNKNOWN -> RepositoryGraphEntityType.CODE_ARTIFACT;
            case TEST_CLASS -> RepositoryGraphEntityType.TEST_CLASS;
            case POLICY_DOC, FEATURE_FILE, CONFIGURATION -> RepositoryGraphEntityType.CODE_ARTIFACT;
            case TEST_DATA -> RepositoryGraphEntityType.CODE_ARTIFACT;
        };
    }

    private RepositoryGraphRelationType map(String edgeType) {
        return switch (edgeType) {
            case "USES" -> RepositoryGraphRelationType.USES;
            case "EXTENDS" -> RepositoryGraphRelationType.EXTENDS;
            case "IMPLEMENTS" -> RepositoryGraphRelationType.IMPLEMENTS;
            case "DECLARES" -> RepositoryGraphRelationType.DECLARES;
            case "CALLS" -> RepositoryGraphRelationType.CALLS;
            default -> RepositoryGraphRelationType.BELONGS_TO;
        };
    }
}
