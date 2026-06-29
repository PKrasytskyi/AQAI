package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheEntry;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheLookupResult;
import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheQueryService;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.orchestration.pipeline.WorkflowStatePipelineAdapter;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PageKnowledgeCacheLookupAgent implements WorkflowAgent,
        PipelineAgent<PageKnowledgeCacheLookupInput, PageKnowledgeCacheLookupOutput>,
        WorkflowStatePipelineAdapter<PageKnowledgeCacheLookupOutput> {

    private static final double MIN_CACHE_CONFIDENCE = 0.80d;

    private final PageKnowledgeCacheQueryService cacheQueryService;
    private final PageKnowledgeFingerprintCalculator fingerprintCalculator = new PageKnowledgeFingerprintCalculator();
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public PageKnowledgeCacheLookupAgent(PageKnowledgeCacheQueryService cacheQueryService) {
        if (cacheQueryService == null) {
            throw new IllegalArgumentException("cacheQueryService cannot be null");
        }
        this.cacheQueryService = cacheQueryService;
    }

    @Override
    public String name() {
        return "page-knowledge-cache-lookup-agent";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_PLAN,
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE,
                WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.MAPPED_UI_KNOWLEDGE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP;
    }

    @Override
    public boolean supports(WorkflowState state) {
        return state.getMappedUiKnowledge() != null
                && state.getPageKnowledgeCacheLookupResult() == null;
    }

    @Override
    public void execute(WorkflowState state) {
        applyOutput(execute(inputFrom(null, state), WorkflowRunEnvelope.from(state)), state);
    }

    @Override
    public PageKnowledgeCacheLookupInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        KnowledgeRunMetadata runMetadata = state.getKnowledgeRunMetadata();
        if (runMetadata == null) {
            runMetadata = KnowledgeRunMetadata.from(state, name());
        }
        return new PageKnowledgeCacheLookupInput(
                state.getUiTestPlan(),
                state.getMappedUiKnowledge(),
                state.getFlowScopedKnowledgePackage(),
                runMetadata
        );
    }

    @Override
    public boolean supports(PageKnowledgeCacheLookupInput input, WorkflowRunEnvelope run) {
        return input != null && input.mappedUiKnowledge() != null;
    }

    @Override
    public PageKnowledgeCacheLookupOutput execute(PageKnowledgeCacheLookupInput input, WorkflowRunEnvelope run) {
        List<PageKnowledgeCacheEntry> entries = new ArrayList<>();
        for (MappedPage page : selectedPages(input)) {
            String fingerprint = fingerprintCalculator.fingerprint(page);
            var cached = cacheQueryService.findCachedEnrichment(input.runMetadata(), page.pageId(), fingerprint);
            if (cached.isPresent() && acceptsCachedRecord(cached.get(), page)) {
                entries.add(new PageKnowledgeCacheEntry(
                        page.pageId(),
                        page.pageName(),
                        page.urlPattern(),
                        fingerprint,
                        cached.get(),
                        "NEO4J_PAGE_KNOWLEDGE_CACHE",
                        true,
                        "Fingerprint matched; AI enrichment can be skipped for this page"
                ));
            } else {
                String note = cached.isPresent()
                        ? "Cached enrichment matched fingerprint but was rejected by page identity or confidence policy"
                        : "No cached enrichment record matched the current page fingerprint";
                entries.add(PageKnowledgeCacheEntry.miss(
                        page.pageId(),
                        page.pageName(),
                        page.urlPattern(),
                        fingerprint,
                        note
                ));
            }
        }

        PageKnowledgeCacheLookupResult result = new PageKnowledgeCacheLookupResult(
                entries,
                "NEO4J_PAGE_KNOWLEDGE_CACHE",
                "Cache lookup mode=" + cacheQueryService.retrievalMode()
                        + " uses appId/baseUrlHash/schemaVersion/pageId/pageFingerprintHash/enrichmentCacheVersion"
        );
        return new PageKnowledgeCacheLookupOutput(result, input.runMetadata(), cacheQueryService.retrievalMode().name());
    }

    @Override
    public void applyOutput(PageKnowledgeCacheLookupOutput output, WorkflowState state) {
        if (output == null) {
            return;
        }
        outputPublisher.publishPageKnowledgeCacheLookup(output.result(), state, output.runMetadata(), output.retrievalMode());
    }

    private List<MappedPage> selectedPages(PageKnowledgeCacheLookupInput input) {
        MappedUiKnowledge knowledge = input.mappedUiKnowledge();
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return List.of();
        }
        if (input.uiTestPlan() == null) {
            return input.flowScopedKnowledgePackage() == null
                    ? knowledge.pages()
                    : input.flowScopedKnowledgePackage().mappedUiKnowledge().pages();
        }

        Set<String> exactRoutes = new LinkedHashSet<>();
        Set<String> exactPageNames = new LinkedHashSet<>();
        for (UiTestScenario scenario : input.uiTestPlan().scenarios()) {
            addReference(exactRoutes, scenario.route());
            addReference(exactRoutes, scenario.sourceRoute());
            addReference(exactPageNames, scenario.pageName());
            addReference(exactPageNames, scenario.sourcePageName());
        }
        List<MappedPage> routeMatched = knowledge.pages().stream()
                .filter(page -> exactRoutes.contains(normalize(page.urlPattern()))
                        || exactRoutes.contains(normalize(page.url())))
                .toList();
        if (!routeMatched.isEmpty()) {
            return routeMatched;
        }
        List<MappedPage> nameMatched = knowledge.pages().stream()
                .filter(page -> exactPageNames.contains(normalize(page.pageName())))
                .toList();
        return nameMatched.isEmpty() && input.flowScopedKnowledgePackage() != null
                ? input.flowScopedKnowledgePackage().mappedUiKnowledge().pages()
                : nameMatched;
    }

    private void addReference(Set<String> target, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private boolean acceptsCachedRecord(
            ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord record,
            MappedPage page
    ) {
        if (record == null || page == null || record.confidenceScore() < MIN_CACHE_CONFIDENCE) {
            return false;
        }
        if (!normalizeRoute(record.route()).equals(normalizeRoute(page.urlPattern()))) {
            return false;
        }
        return normalize(record.pageName()).equals(normalize(page.pageName()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeRoute(String value) {
        return RouteCanonicalizer.canonicalize(value);
    }
}
