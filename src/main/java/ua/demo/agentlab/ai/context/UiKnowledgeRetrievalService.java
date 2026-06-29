package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;
import ua.demo.agentlab.ai.rag.store.VectorStore;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.ConfirmedRouteGuard;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.catalog.PageSource;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.KnowledgeVectorRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeNamespaceFilterBuilder;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRetrievalMode;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UiKnowledgeRetrievalService {

    private final KnowledgeVectorRuntimeConfig vectorConfig;
    private final Neo4jRuntimeConfig neo4jConfig;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final Neo4jUiKnowledgeQueryService graphQueryService;
    private final KnowledgeNamespaceFilterBuilder namespaceFilterBuilder = new KnowledgeNamespaceFilterBuilder();
    private final ConfirmedPageSourceResolver confirmedPageSourceResolver = new ConfirmedPageSourceResolver();

    public UiKnowledgeRetrievalService(
            KnowledgeVectorRuntimeConfig vectorConfig,
            Neo4jRuntimeConfig neo4jConfig,
            OpenAiRuntimeConfig openAiRuntimeConfig
    ) {
        this(
                vectorConfig,
                neo4jConfig,
                vectorConfig == null ? null : new OpenAiEmbeddingClient(
                        new UiKnowledgeRagRuntimeConfigAdapter(vectorConfig, openAiRuntimeConfig)
                ),
                vectorConfig == null ? null : new QdrantVectorStore(
                        new UiKnowledgeRagRuntimeConfigAdapter(vectorConfig, openAiRuntimeConfig)
                ),
                neo4jConfig == null ? null : new Neo4jUiKnowledgeQueryService(neo4jConfig)
        );
    }

    UiKnowledgeRetrievalService(
            KnowledgeVectorRuntimeConfig vectorConfig,
            Neo4jRuntimeConfig neo4jConfig,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            Neo4jUiKnowledgeQueryService graphQueryService
    ) {
        this.vectorConfig = vectorConfig;
        this.neo4jConfig = neo4jConfig;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.graphQueryService = graphQueryService;
    }

    public UiKnowledgeRetrievalContext retrieve(WorkflowState state, CanonicalUiInteractionModel canonicalModel) {
        return retrieve(state, canonicalModel, List.of(), List.of(), "");
    }

    public UiKnowledgeRetrievalContext retrieve(
            WorkflowState state,
            CanonicalUiInteractionModel canonicalModel,
            List<String> preferredPageIds,
            List<String> preferredTerms,
            String querySeed
    ) {
        if (state == null) {
            return UiKnowledgeRetrievalContext.empty("Mapped UI knowledge is not available for retrieval");
        }
        return retrieve(new UiKnowledgeRetrievalRequest(
                state.getProjectProfile(),
                state.getNormalizedRequirementBundle(),
                state.getTestPlan(),
                state.getUiTestPlan(),
                state.getCanonicalTestCaseBundle(),
                state.getMappedUiKnowledge(),
                state.getKnowledgeRunMetadata(),
                canonicalModel,
                preferredPageIds,
                preferredTerms,
                querySeed
        ));
    }

    public UiKnowledgeRetrievalContext retrieve(UiKnowledgeRetrievalRequest request) {
        if (request == null || request.mappedUiKnowledge() == null) {
            return UiKnowledgeRetrievalContext.empty("Mapped UI knowledge is not available for retrieval");
        }

        String query = request.querySeed().isBlank()
                ? buildQuery(request)
                : request.querySeed();
        List<String> queryTerms = request.preferredTerms().isEmpty()
                ? extractTerms(query)
                : mergeTerms(request.preferredTerms(), extractTerms(query));
        List<String> relevantPageIds = request.preferredPageIds().isEmpty()
                ? resolveRelevantPageIds(request.mappedUiKnowledge(), request)
                : request.preferredPageIds();
        List<String> notes = new ArrayList<>();
        KnowledgeRunMetadata runMetadata = request.runMetadata();
        if (runMetadata == null) {
            notes.add("Knowledge namespace is unavailable; DB retrieval is skipped to avoid stale evidence");
            return new UiKnowledgeRetrievalContext(
                    query,
                    queryTerms,
                    List.of(),
                    List.of(),
                    "SKIPPED_NO_NAMESPACE",
                    "SKIPPED_NO_NAMESPACE",
                    List.copyOf(notes)
            );
        }

        if (relevantPageIds.isEmpty()) {
            notes.add("No exact route-scoped page evidence is available for retrieval");
            return new UiKnowledgeRetrievalContext(
                    query,
                    queryTerms,
                    List.of(),
                    List.of(),
                    "SKIPPED_NO_ROUTE_MATCH",
                    "SKIPPED_NO_ROUTE_MATCH",
                    List.copyOf(notes)
            );
        }

        List<RetrievedChunk> vectorMatches = List.of();
        String vectorSource = "DISABLED";
        Map<String, String> currentRunFilter = namespaceFilterBuilder.currentRunOnly(runMetadata);
        if (vectorConfig != null && vectorConfig.enabled() && embeddingService != null && vectorStore != null) {
            try {
                vectorMatches = filterVectorMatches(
                        vectorStore.search(embeddingService.embed(query), 8, currentRunFilter),
                        relevantPageIds
                );
                vectorSource = "QDRANT_UI_KNOWLEDGE";
                notes.add("Vector retrieval enabled with mode=" + KnowledgeRetrievalMode.CURRENT_RUN_ONLY
                        + " for runId=" + runMetadata.runId());
            } catch (Exception exception) {
                vectorSource = "QDRANT_UI_KNOWLEDGE_UNAVAILABLE";
                notes.add("Vector retrieval unavailable: " + exception.getMessage());
            }
        } else {
            notes.add("Vector retrieval disabled");
        }

        List<String> seedNodeIds = resolveSeedNodeIds(vectorMatches, request.mappedUiKnowledge());
        List<UiKnowledgeGraphMatch> graphMatches = List.of();
        String graphSource = "DISABLED";
        if (neo4jConfig != null && neo4jConfig.enabled() && graphQueryService != null) {
            try {
                graphMatches = graphQueryService.search(seedNodeIds, relevantPageIds, queryTerms, 12, currentRunFilter);
                graphSource = "NEO4J_UI_KNOWLEDGE";
                notes.add("Graph retrieval enabled with mode=" + KnowledgeRetrievalMode.CURRENT_RUN_ONLY
                        + " for runId=" + runMetadata.runId());
            } catch (Exception exception) {
                graphSource = "NEO4J_UI_KNOWLEDGE_UNAVAILABLE";
                notes.add("Graph retrieval unavailable: " + exception.getMessage());
            }
        } else {
            notes.add("Graph retrieval disabled");
        }

        return new UiKnowledgeRetrievalContext(
                query,
                queryTerms,
                vectorMatches,
                graphMatches,
                vectorSource,
                graphSource,
                List.copyOf(notes)
        );
    }

    private List<String> mergeTerms(List<String> preferredTerms, List<String> extractedTerms) {
        Set<String> merged = new LinkedHashSet<>();
        if (preferredTerms != null) {
            preferredTerms.stream()
                    .map(this::normalize)
                    .filter(value -> !value.isBlank())
                    .forEach(merged::add);
        }
        if (extractedTerms != null) {
            extractedTerms.stream()
                    .map(this::normalize)
                    .filter(value -> !value.isBlank())
                    .forEach(merged::add);
        }
        return List.copyOf(merged);
    }

    private List<RetrievedChunk> filterVectorMatches(List<RetrievedChunk> matches, List<String> relevantPageIds) {
        if (matches == null || matches.isEmpty()) {
            return List.of();
        }
        if (relevantPageIds == null || relevantPageIds.isEmpty()) {
            return List.of();
        }
        return matches.stream()
                .filter(match -> match.metadata() != null
                        && match.metadata().tags().stream().anyMatch(relevantPageIds::contains))
                .limit(8)
                .toList();
    }

    private List<String> resolveSeedNodeIds(List<RetrievedChunk> vectorMatches, MappedUiKnowledge knowledge) {
        if (vectorMatches == null || vectorMatches.isEmpty() || knowledge == null) {
            return List.of();
        }
        Set<String> knownNodeIds = knowledge.graphNodes().stream()
                .map(node -> node.nodeId())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> pageIds = knowledge.pages().stream()
                .map(MappedPage::pageId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> seedIds = new LinkedHashSet<>();
        for (RetrievedChunk match : vectorMatches) {
            if (match.metadata() == null) {
                continue;
            }
            for (String tag : match.metadata().tags()) {
                if (knownNodeIds.contains(tag) || pageIds.contains(tag)) {
                    seedIds.add(tag);
                }
            }
        }
        return List.copyOf(seedIds);
    }

    private List<String> resolveRelevantPageIds(MappedUiKnowledge knowledge, UiKnowledgeRetrievalRequest request) {
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return List.of();
        }
        Set<String> pageNames = new LinkedHashSet<>();
        Set<String> routes = new LinkedHashSet<>();
        if (request.uiTestPlan() != null) {
            for (UiTestScenario scenario : request.uiTestPlan().scenarios()) {
                addIfPresent(pageNames, scenario.pageName());
                addIfPresent(pageNames, scenario.sourcePageName());
                addIfPresent(routes, scenario.route());
                addIfPresent(routes, scenario.sourceRoute());
            }
        } else if (request.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : request.canonicalTestCaseBundle().testCases()) {
                addIfPresent(pageNames, testCase.pageName());
                addIfPresent(pageNames, testCase.sourcePageName());
                testCase.targetPages().forEach(page -> addIfPresent(pageNames, page));
                addIfPresent(routes, testCase.route());
                addIfPresent(routes, testCase.sourceRoute());
            }
        } else if (request.projectProfile() != null) {
            for (String route : request.projectProfile().configuredRoutes()) {
                addIfPresent(routes, route);
            }
        }
        ConfirmedRouteGuard guard = confirmedRouteGuard(knowledge, request);
        Set<String> strictRoutes = routes.stream()
                .filter(guard::isConfirmedRoute)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> routeMatchedPageIds = knowledge.pages().stream()
                .filter(page -> guard.isConfirmed(page.pageName(), page.urlPattern())
                        || guard.isConfirmed(page.pageName(), page.url()))
                .filter(page -> strictRoutes.stream().anyMatch(route -> PageReferenceMatcher.routeMatches(page.urlPattern(), route)
                        || PageReferenceMatcher.routeMatches(page.url(), route)))
                .map(MappedPage::pageId)
                .distinct()
                .toList();
        if (!routeMatchedPageIds.isEmpty()) {
            return routeMatchedPageIds;
        }
        if (!strictRoutes.isEmpty()) {
            return List.of();
        }
        return knowledge.pages().stream()
                .filter(page -> guard.isConfirmed(page.pageName(), page.urlPattern())
                        || guard.isConfirmed(page.pageName(), page.url()))
                .filter(page -> pageNames.isEmpty() || PageReferenceMatcher.matchesAny(page, pageNames))
                .map(MappedPage::pageId)
                .distinct()
                .toList();
    }

    private ConfirmedRouteGuard confirmedRouteGuard(MappedUiKnowledge knowledge, UiKnowledgeRetrievalRequest request) {
        List<ConfirmedPageCandidate> currentMappedPages = knowledge == null
                ? List.of()
                : knowledge.pages().stream()
                .map(page -> new ConfirmedPageCandidate(
                        page.pageName(),
                        !page.urlPattern().isBlank() ? page.urlPattern() : page.url(),
                        PageCapability.GENERIC,
                        PageSource.DB_STABLE_CACHE,
                        0.86d,
                        List.of("current-mapped-ui-knowledge")
                ))
                .toList();
        return new ConfirmedRouteGuard(confirmedPageSourceResolver.resolve(
                request.projectProfile(),
                request.normalizedRequirementBundle(),
                currentMappedPages
        ));
    }

    private String buildQuery(UiKnowledgeRetrievalRequest request) {
        StringBuilder builder = new StringBuilder();
        if (request.projectProfile() != null) {
            builder.append(request.projectProfile().projectName()).append(' ');
            builder.append(request.projectProfile().baseUrl()).append(' ');
        }
        if (request.uiTestPlan() != null) {
            for (UiTestScenario scenario : request.uiTestPlan().scenarios()) {
                builder.append(scenario.id()).append(' ')
                        .append(scenario.title()).append(' ')
                        .append(String.join(" ", scenario.actions())).append(' ')
                        .append(String.join(" ", scenario.assertions())).append(' ');
            }
        } else if (request.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : request.canonicalTestCaseBundle().testCases()) {
                builder.append(testCase.id()).append(' ')
                        .append(testCase.title()).append(' ')
                        .append(String.join(" ", testCase.llmSteps())).append(' ')
                        .append(String.join(" ", testCase.requirementRefs())).append(' ');
            }
        } else if (request.testPlan() != null) {
            request.testPlan().scenarios().stream().limit(8)
                    .forEach(scenario -> builder.append(scenario.title()).append(' '));
        }
        if (request.canonicalModel() != null) {
            request.canonicalModel().interactions().stream().limit(16).forEach(interaction -> builder
                    .append(interaction.pageName()).append(' ')
                    .append(interaction.canonicalName()).append(' ')
                    .append(interaction.interactionType()).append(' ')
                    .append(interaction.subjectType()).append(' ')
                    .append(interaction.targetType()).append(' ')
                    .append(interaction.targetRoute()).append(' ')
                    .append(String.join(" ", interaction.domainHints())).append(' ')
                    .append(String.join(" ", interaction.keywords())).append(' '));
        }
        return builder.toString().trim();
    }

    private List<String> extractTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        if (query == null || query.isBlank()) {
            return List.of();
        }
        for (String token : query.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/ ]", " ").split("\\s+")) {
            if (!token.isBlank() && token.length() >= 4) {
                terms.add(token);
            }
        }
        return List.copyOf(terms);
    }

    private void addIfPresent(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static final class UiKnowledgeRagRuntimeConfigAdapter implements RagRuntimeConfig {

        private final KnowledgeVectorRuntimeConfig vectorConfig;
        private final OpenAiRuntimeConfig openAiRuntimeConfig;

        private UiKnowledgeRagRuntimeConfigAdapter(
                KnowledgeVectorRuntimeConfig vectorConfig,
                OpenAiRuntimeConfig openAiRuntimeConfig
        ) {
            this.vectorConfig = vectorConfig;
            this.openAiRuntimeConfig = openAiRuntimeConfig;
        }

        @Override
        public boolean enabled() {
            return vectorConfig.enabled();
        }

        @Override
        public String qdrantUrl() {
            return vectorConfig.qdrantUrl();
        }

        @Override
        public String qdrantApiKey() {
            return vectorConfig.qdrantApiKey();
        }

        @Override
        public String collectionName() {
            return vectorConfig.collectionName();
        }

        @Override
        public Path chunksJsonlPath() {
            return Path.of("target", "ui-discovery", "vector-documents", "chunks.jsonl").toAbsolutePath().normalize();
        }

        @Override
        public int chunkMaxChars() {
            return 1800;
        }

        @Override
        public int chunkOverlapChars() {
            return 0;
        }

        @Override
        public int retrievalLimit() {
            return 8;
        }

        @Override
        public String embeddingModel() {
            return vectorConfig.embeddingModel();
        }

        @Override
        public String generationModel() {
            return openAiRuntimeConfig == null ? "gpt-5-mini" : openAiRuntimeConfig.model();
        }

        @Override
        public String openAiApiKey() {
            String key = vectorConfig.openAiApiKey();
            if (key != null && !key.isBlank()) {
                return key;
            }
            return openAiRuntimeConfig == null ? "" : openAiRuntimeConfig.apiKey();
        }

        @Override
        public String openAiBaseUrl() {
            String baseUrl = vectorConfig.openAiBaseUrl();
            if (baseUrl != null && !baseUrl.isBlank()) {
                return baseUrl;
            }
            return openAiRuntimeConfig == null ? "https://api.openai.com/v1" : openAiRuntimeConfig.baseUrl();
        }

        @Override
        public int maxOutputTokens() {
            return openAiRuntimeConfig == null ? 1800 : openAiRuntimeConfig.maxOutputTokens();
        }
    }
}
