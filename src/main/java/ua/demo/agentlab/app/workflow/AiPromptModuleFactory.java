package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.assertions.agent.AssertionContractAgent;
import ua.demo.agentlab.ai.context.AiContextAssembler;
import ua.demo.agentlab.ai.context.UiKnowledgeRetrievalService;
import ua.demo.agentlab.ai.expectationenrichment.agent.TestCaseExpectationEnrichmentAgent;
import ua.demo.agentlab.ai.expectationenrichment.service.OpenAiTestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.expectationenrichment.service.RuleBasedTestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.expectationenrichment.service.TestCaseExpectationEnrichmentClient;
import ua.demo.agentlab.ai.flow.BusinessFlowResolver;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeRefreshAgent;
import ua.demo.agentlab.ai.flow.FlowScopedKnowledgeService;
import ua.demo.agentlab.ai.openai.PropertiesOpenAiRuntimeConfig;
import ua.demo.agentlab.ai.pageenrichment.agent.PageKnowledgeCacheLookupAgent;
import ua.demo.agentlab.ai.pageenrichment.agent.PageModelEnrichmentAgent;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheQueryService;
import ua.demo.agentlab.ai.pageenrichment.service.OpenAiPageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.RuleBasedPageModelEnrichmentClient;
import ua.demo.agentlab.ai.rag.config.PropertiesRagRuntimeConfig;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.ui.agent.AiContextAssemblyAgent;
import ua.demo.agentlab.ai.ui.agent.AiPageObjectSpecAgent;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectSpecGenerator;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesKnowledgeVectorRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

public class AiPromptModuleFactory {

    public AiPromptModule create(WorkflowCoreComponents core) {
        if (core == null) {
            throw new IllegalArgumentException("core workflow components cannot be null");
        }
        PropertiesOpenAiRuntimeConfig openAiRuntimeConfig = new PropertiesOpenAiRuntimeConfig();
        RagRuntimeConfig ragRuntimeConfig = new PropertiesRagRuntimeConfig();
        UiKnowledgeRetrievalService uiKnowledgeRetrievalService = new UiKnowledgeRetrievalService(
                new PropertiesKnowledgeVectorRuntimeConfig(),
                new PropertiesNeo4jRuntimeConfig(),
                openAiRuntimeConfig
        );
        AiContextAssembler aiContextAssembler = new AiContextAssembler(
                core.canonicalInteractionLayer(),
                uiKnowledgeRetrievalService
        );
        return new AiPromptModule(
                flowScopedKnowledgeAgent(core, uiKnowledgeRetrievalService),
                new TestCaseExpectationEnrichmentAgent(expectationEnrichmentClient(ragRuntimeConfig)),
                new AssertionContractAgent(),
                new PageKnowledgeCacheLookupAgent(new PageKnowledgeCacheQueryService(new PropertiesNeo4jRuntimeConfig())),
                new PageModelEnrichmentAgent(pageModelEnrichmentClient(ragRuntimeConfig)),
                flowScopedKnowledgeRefreshAgent(core, uiKnowledgeRetrievalService),
                new AiContextAssemblyAgent(aiContextAssembler),
                new AiPageObjectSpecAgent(
                        new AiPageObjectSpecGenerator(openAiRuntimeConfig),
                        currentState -> core.seleniumWriter().buildAiBaselinePageObjectSpecs(currentState.getUiTestPlan())
                )
        );
    }

    private WorkflowAgent flowScopedKnowledgeAgent(
            WorkflowCoreComponents core,
            UiKnowledgeRetrievalService uiKnowledgeRetrievalService
    ) {
        return new FlowScopedKnowledgeAgent(flowScopedKnowledgeService(core, uiKnowledgeRetrievalService));
    }

    private WorkflowAgent flowScopedKnowledgeRefreshAgent(
            WorkflowCoreComponents core,
            UiKnowledgeRetrievalService uiKnowledgeRetrievalService
    ) {
        return new FlowScopedKnowledgeRefreshAgent(flowScopedKnowledgeService(core, uiKnowledgeRetrievalService));
    }

    private FlowScopedKnowledgeService flowScopedKnowledgeService(
            WorkflowCoreComponents core,
            UiKnowledgeRetrievalService uiKnowledgeRetrievalService
    ) {
        return new FlowScopedKnowledgeService(
                new BusinessFlowResolver(),
                core.canonicalInteractionLayer(),
                uiKnowledgeRetrievalService
        );
    }

    private PageModelEnrichmentClient pageModelEnrichmentClient(RagRuntimeConfig ragRuntimeConfig) {
        return openAiAvailable(ragRuntimeConfig)
                ? new OpenAiPageModelEnrichmentClient(ragRuntimeConfig)
                : new RuleBasedPageModelEnrichmentClient();
    }

    private TestCaseExpectationEnrichmentClient expectationEnrichmentClient(RagRuntimeConfig ragRuntimeConfig) {
        return openAiAvailable(ragRuntimeConfig)
                ? new OpenAiTestCaseExpectationEnrichmentClient(ragRuntimeConfig)
                : new RuleBasedTestCaseExpectationEnrichmentClient();
    }

    private boolean openAiAvailable(RagRuntimeConfig ragRuntimeConfig) {
        return ragRuntimeConfig.enabled()
                && ragRuntimeConfig.openAiApiKey() != null
                && !ragRuntimeConfig.openAiApiKey().isBlank();
    }
}
