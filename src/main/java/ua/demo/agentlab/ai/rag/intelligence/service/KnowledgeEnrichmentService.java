package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.List;

public class KnowledgeEnrichmentService {

    private final KnowledgeEnrichmentClient enrichmentClient;

    public KnowledgeEnrichmentService() {
        this(new RuleBasedKnowledgeEnrichmentClient());
    }

    public KnowledgeEnrichmentService(KnowledgeEnrichmentClient enrichmentClient) {
        this.enrichmentClient = enrichmentClient == null
                ? new RuleBasedKnowledgeEnrichmentClient()
                : enrichmentClient;
    }

    public List<KnowledgeEnrichmentRecord> enrich(
            List<SourceDocument> documents,
            List<JavaAstParseResult> javaAstResults,
            List<LayerComponentDefinition> layerComponents,
            List<ExistingTestDefinition> existingTests,
            List<ControllerRouteDefinition> controllerRoutes
    ) {
        return enrichmentClient.enrich(new KnowledgeEnrichmentRequest(
                documents,
                javaAstResults,
                layerComponents,
                existingTests,
                controllerRoutes
        ));
    }
}
