package ua.demo.agentlab.ui.discovery.agent;

import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.ConfirmedRouteGuard;
import ua.demo.agentlab.ui.catalog.StablePageRegistry;
import ua.demo.agentlab.ui.discovery.knowledge.MappedUiKnowledgeCurator;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeRaw;
import ua.demo.agentlab.ui.discovery.mapping.PageMapper;
import ua.demo.agentlab.ui.discovery.mapping.MappedUiKnowledgeRouteFilter;
import ua.demo.agentlab.ui.discovery.mapping.MappedUiKnowledgeRouteCollisionPolicy;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.List;
import java.util.Set;

public class UiPageMappingAgent implements WorkflowAgent,
        PipelineAgent<UiPageMappingInput, UiPageMappingOutput> {

    private final PageMapper pageMapper;
    private final ConfirmedPageSourceResolver confirmedPageSourceResolver = new ConfirmedPageSourceResolver();
    private final MappedUiKnowledgeRouteFilter routeFilter = new MappedUiKnowledgeRouteFilter();
    private final MappedUiKnowledgeRouteCollisionPolicy routeCollisionPolicy = new MappedUiKnowledgeRouteCollisionPolicy();
    private final MappedUiKnowledgeCurator knowledgeCurator = new MappedUiKnowledgeCurator();
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public UiPageMappingAgent(PageMapper pageMapper) {
        if (pageMapper == null) {
            throw new IllegalArgumentException("pageMapper cannot be null");
        }
        this.pageMapper = pageMapper;
    }

    @Override
    public String name() {
        return "ui-page-mapping-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.UI_DISCOVERY_SNAPSHOT, WorkflowArtifact.PAGE_MODEL_BUNDLE);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.MAPPED_UI_KNOWLEDGE);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PAGE_MODEL_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public UiPageMappingInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new UiPageMappingInput(
                state.getProjectProfile(),
                state.getNormalizedRequirementBundle(),
                state.getUiDiscoverySnapshot(),
                state.getSeleniumDiscoveryResult(),
                state.getPageModelBundle()
        );
    }

    @Override
    public boolean supports(UiPageMappingInput input, WorkflowRunEnvelope run) {
        return input != null && input.discoverySnapshot() != null && input.pageModelBundle() != null;
    }

    @Override
    public UiPageMappingOutput execute(UiPageMappingInput input, WorkflowRunEnvelope run) {
        if (input == null || input.discoverySnapshot() == null || input.pageModelBundle() == null) {
            throw new IllegalArgumentException("UI discovery snapshot and PageModel bundle are required");
        }
        MappedUiKnowledge mappedKnowledge = pageMapper.map(
                input.discoverySnapshot(),
                input.seleniumDiscoveryResult(),
                input.pageModelBundle()
        );
        var confirmedPages = confirmedPageSourceResolver.resolve(
                input.projectProfile(),
                input.normalizedRequirementBundle(),
                input.discoverySnapshot(),
                List.of()
        );
        ConfirmedRouteGuard guard = new ConfirmedRouteGuard(confirmedPages);
        MappedUiKnowledge filtered = routeFilter.filter(mappedKnowledge, guard);
        MappedUiKnowledge collisionResolved = routeCollisionPolicy.apply(filtered, new StablePageRegistry(confirmedPages));
        MappedUiKnowledgeRaw raw = new MappedUiKnowledgeRaw(
                mappedKnowledge,
                List.of("mapper:raw", "mapper:page-count=" + mappedKnowledge.pages().size())
        );
        return new UiPageMappingOutput(
                raw,
                knowledgeCurator.curate(new MappedUiKnowledgeRaw(
                        collisionResolved,
                        List.of("mapper:route-filter", "mapper:route-collision-policy")
                ))
        );
    }

    @Override
    public void applyOutput(UiPageMappingOutput output, WorkflowState state) {
        outputPublisher.publishMappedUiKnowledge(output, state);
    }
}
