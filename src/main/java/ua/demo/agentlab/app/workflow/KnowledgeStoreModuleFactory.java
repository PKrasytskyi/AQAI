package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.context.RuleBasedCanonicalInteractionLayer;
import ua.demo.agentlab.ai.flow.BusinessFlowResolver;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeService;
import ua.demo.agentlab.ui.discovery.agent.UiPageKnowledgePersistenceAgent;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.GraphPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.QdrantPageKnowledgeWriter;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesKnowledgeVectorRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.util.List;

public class KnowledgeStoreModuleFactory {

    public KnowledgeStoreModule create() {
        RuleBasedCanonicalInteractionLayer canonicalInteractionLayer = new RuleBasedCanonicalInteractionLayer();
        return new KnowledgeStoreModule(
                canonicalInteractionLayer,
                new UiPageKnowledgePersistenceAgent(List.of(
                        new GraphPageKnowledgeWriter(new PropertiesNeo4jRuntimeConfig()),
                        new QdrantPageKnowledgeWriter(new PropertiesKnowledgeVectorRuntimeConfig())
                )),
                new FlowScopedKnowledgeAgent(
                        new FlowScopedKnowledgeService(
                                new BusinessFlowResolver(),
                                canonicalInteractionLayer,
                                null
                        )
                )
        );
    }
}
