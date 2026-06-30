package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheEntry;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichedKnowledgeAssembler;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.OpenAiPageModelEnrichmentClient;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.ArrayList;
import java.net.URI;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageModelEnrichmentAgent implements WorkflowAgent,
        PipelineAgent<PageModelEnrichmentInputBundle, PageModelEnrichmentOutput> {

    private final PageModelEnrichmentClient enrichmentClient;
    private final PageModelEnrichedKnowledgeAssembler knowledgeAssembler;
    private final PageKnowledgeFingerprintCalculator fingerprintCalculator = new PageKnowledgeFingerprintCalculator();
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public PageModelEnrichmentAgent(PageModelEnrichmentClient enrichmentClient) {
        this(enrichmentClient, new PageModelEnrichedKnowledgeAssembler());
    }

    PageModelEnrichmentAgent(PageModelEnrichmentClient enrichmentClient, PageModelEnrichedKnowledgeAssembler knowledgeAssembler) {
        if (enrichmentClient == null || knowledgeAssembler == null) {
            throw new IllegalArgumentException("enrichment collaborators cannot be null");
        }
        this.enrichmentClient = enrichmentClient;
        this.knowledgeAssembler = knowledgeAssembler;
    }

    @Override
    public String name() {
        return "page-model-enrichment-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(
                WorkflowArtifact.UI_TEST_PLAN,
                WorkflowArtifact.PAGE_MODEL_BUNDLE,
                WorkflowArtifact.MAPPED_UI_KNOWLEDGE,
                WorkflowArtifact.FLOW_SCOPED_KNOWLEDGE_PACKAGE,
                WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP
        );
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(
                WorkflowArtifact.PAGE_MODEL_ENRICHMENT_RECORDS,
                WorkflowArtifact.ENRICHED_MAPPED_UI_KNOWLEDGE,
                WorkflowArtifact.PAGE_MODEL_ENRICHMENT_OUTPUT
        );
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.PAGE_MODEL_BUNDLE;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.PAGE_MODEL_ENRICHMENT_OUTPUT;
    }

    @Override
    public PageModelEnrichmentInputBundle inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new PageModelEnrichmentInputBundle(
                state.getProjectProfile(),
                state.getUiTestPlan(),
                state.getCanonicalTestCaseBundle(),
                state.getPageModelBundle(),
                state.getMappedUiKnowledge(),
                state.getFlowScopedKnowledgePackage(),
                state.getPageKnowledgeCacheLookupResult()
        );
    }

    @Override
    public boolean supports(PageModelEnrichmentInputBundle input, WorkflowRunEnvelope run) {
        return input != null
                && input.uiTestPlan() != null
                && input.flowScopedKnowledgePackage() != null
                && input.pageModelBundle() != null;
    }

    @Override
    public PageModelEnrichmentOutput execute(PageModelEnrichmentInputBundle input, WorkflowRunEnvelope run) {
        List<PageModelEnrichmentInput> inputs = buildInputs(input);
        List<PageModelEnrichmentRecord> cachedRecords = cachedRecords(input);
        List<PageModelEnrichmentRecord> generatedRecords = inputs.isEmpty()
                ? List.of()
                : enrichmentClient.enrich(inputs);
        List<PageModelEnrichmentRecord> records = new ArrayList<>();
        MappedUiKnowledge selectedKnowledge = selectedKnowledge(input);
        records.addAll(rebindBusinessIntent(cachedRecords, selectedKnowledge));
        records.addAll(rebindBusinessIntent(generatedRecords, selectedKnowledge));
        List<String> failures = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastFailures()
                : List.of();
        return new PageModelEnrichmentOutput(
                records,
                cachedRecords,
                generatedRecords,
                knowledgeAssembler.merge(selectedKnowledge, records),
                failures
        );
    }

    private List<PageModelEnrichmentRecord> rebindBusinessIntent(
            List<PageModelEnrichmentRecord> records,
            MappedUiKnowledge knowledge
    ) {
        if (records == null || records.isEmpty() || knowledge == null) {
            return records == null ? List.of() : records;
        }
        Map<String, MappedPage> pagesById = knowledge.pages().stream()
                .collect(java.util.stream.Collectors.toMap(
                        page -> normalize(page.pageId()),
                        page -> page,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ));
        return records.stream()
                .map(record -> rebindBusinessIntent(record, pagesById.get(normalize(record.pageId()))))
                .toList();
    }

    private PageModelEnrichmentRecord rebindBusinessIntent(PageModelEnrichmentRecord record, MappedPage page) {
        if (record == null || page == null || page.canonicalPageType() == null) {
            return record;
        }
        String deterministicIntent = switch (page.canonicalPageType()) {
            case DASHBOARD -> "dashboard";
            case AUTHENTICATED_AREA -> "authenticated-area";
            case AUTHENTICATION -> "authentication";
            case REGISTRATION -> "registration";
            case RECOVERY -> "recovery";
            case SECURITY -> "security";
            case FORM -> "form";
            case LISTING, SEARCH -> "record-list";
            case DETAILS -> "record-details";
            case CART -> "container";
            case LANDING -> "navigation";
            case GENERIC -> record.businessIntent();
        };
        if (deterministicIntent.equals(record.businessIntent()) || deterministicIntent.isBlank()) {
            return record;
        }
        return new PageModelEnrichmentRecord(
                record.pageId(),
                record.pageName(),
                record.route(),
                deterministicIntent,
                record.pageSummary(),
                record.supportedActions(),
                record.stableLocators(),
                record.preconditions(),
                record.postconditions(),
                record.risks(),
                record.coverageGaps(),
                record.requirementTraceability(),
                record.actionsByRequirement(),
                record.postconditionsByRequirement(),
                record.confidenceScore(),
                record.enrichmentSource()
        );
    }

    @Override
    public void applyOutput(PageModelEnrichmentOutput output, WorkflowState state) {
        outputPublisher.publishPageModelEnrichment(output, state);
    }

    private List<PageModelEnrichmentInput> buildInputs(PageModelEnrichmentInputBundle input) {
        MappedUiKnowledge knowledge = selectedKnowledge(input);
        PageModelBundle models = input.pageModelBundle();
        if (knowledge == null || models == null) {
            return List.of();
        }
        String applicationHost = applicationHost(input);
        return knowledge.pages().stream()
                .filter(page -> cacheMiss(input, page))
                .map(page -> toInput(page, findModel(models, page), pageOwnedEvidence(input, page), applicationHost))
                .toList();
    }

    private List<PageModelEnrichmentRecord> cachedRecords(PageModelEnrichmentInputBundle input) {
        if (input.cacheLookupResult() == null) {
            return List.of();
        }
        MappedUiKnowledge knowledge = selectedKnowledge(input);
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return input.cacheLookupResult().cachedRecords();
        }
        List<PageModelEnrichmentRecord> records = new ArrayList<>();
        for (MappedPage page : knowledge.pages()) {
            String fingerprint = fingerprintCalculator.fingerprint(page);
            input.cacheLookupResult().hitFor(page.pageId(), fingerprint)
                    .map(PageKnowledgeCacheEntry::enrichmentRecord)
                    .map(record -> rebindCachedRecord(record, page, pageOwnedEvidence(input, page)))
                    .ifPresent(records::add);
        }
        return records;
    }

    private PageModelEnrichmentRecord rebindCachedRecord(
            PageModelEnrichmentRecord cached,
            MappedPage currentPage,
            PageRequirementEvidence evidence
    ) {
        if (cached == null) {
            return null;
        }
        MappedPage safePage = currentPage;
        PageRequirementEvidence safeEvidence = evidence == null ? PageRequirementEvidence.empty() : evidence;
        boolean hasCurrentRequirementEvidence = !safeEvidence.testCaseIds().isEmpty()
                || !safeEvidence.requirementRefs().isEmpty()
                || !safeEvidence.actionsByRequirement().isEmpty()
                || !safeEvidence.postconditionsByRequirement().isEmpty();
        if (!hasCurrentRequirementEvidence) {
            return safePage == null
                    ? cached
                    : new PageModelEnrichmentRecord(
                    safePage.pageId(),
                    safePage.pageName(),
                    safePage.urlPattern(),
                    cached.businessIntent(),
                    cached.pageSummary(),
                    cached.supportedActions(),
                    cached.stableLocators(),
                    cached.preconditions(),
                    cached.postconditions(),
                    cached.risks(),
                    cached.coverageGaps(),
                    cached.requirementTraceability(),
                    cached.actionsByRequirement(),
                    cached.postconditionsByRequirement(),
                    cached.confidenceScore(),
                    "db-cache"
            );
        }
        return new PageModelEnrichmentRecord(
                safePage == null ? cached.pageId() : safePage.pageId(),
                safePage == null ? cached.pageName() : safePage.pageName(),
                safePage == null ? cached.route() : safePage.urlPattern(),
                cached.businessIntent(),
                cached.pageSummary(),
                safeEvidence.actions().isEmpty() ? cached.supportedActions() : safeEvidence.actions(),
                cached.stableLocators(),
                safeEvidence.preconditions().isEmpty() ? cached.preconditions() : safeEvidence.preconditions(),
                safeEvidence.assertions().isEmpty() ? cached.postconditions() : safeEvidence.assertions(),
                cached.risks(),
                cached.coverageGaps(),
                safeEvidence.requirementRefs().isEmpty() ? cached.requirementTraceability() : safeEvidence.requirementRefs(),
                safeEvidence.actionsByRequirement().isEmpty() ? cached.actionsByRequirement() : safeEvidence.actionsByRequirement(),
                safeEvidence.postconditionsByRequirement().isEmpty()
                        ? cached.postconditionsByRequirement()
                        : safeEvidence.postconditionsByRequirement(),
                cached.confidenceScore(),
                "db-cache"
        );
    }

    private boolean cacheMiss(PageModelEnrichmentInputBundle input, MappedPage page) {
        if (input.cacheLookupResult() == null || page == null) {
            return true;
        }
        String fingerprint = fingerprintCalculator.fingerprint(page);
        return input.cacheLookupResult()
                .hitFor(page.pageId(), fingerprint)
                .map(PageKnowledgeCacheEntry::hit)
                .map(hit -> !hit)
                .orElse(true);
    }

    private MappedUiKnowledge selectedKnowledge(PageModelEnrichmentInputBundle input) {
        MappedUiKnowledge rawKnowledge = input.mappedUiKnowledge();
        if (rawKnowledge == null || input.uiTestPlan() == null) {
            return input.flowScopedKnowledgePackage() == null ? rawKnowledge : input.flowScopedKnowledgePackage().mappedUiKnowledge();
        }
        Set<String> exactRoutes = new LinkedHashSet<>();
        Set<String> exactPageNames = new LinkedHashSet<>();
        for (UiTestScenario scenario : input.uiTestPlan().scenarios()) {
            addReference(exactRoutes, scenario.route());
            addReference(exactRoutes, scenario.sourceRoute());
            addReference(exactPageNames, scenario.pageName());
            addReference(exactPageNames, scenario.sourcePageName());
        }
        List<MappedPage> routeMatched = rawKnowledge.pages().stream()
                .filter(page -> exactRoutes.contains(normalize(page.urlPattern())) || exactRoutes.contains(normalize(page.url())))
                .toList();
        List<MappedPage> selectedPages = routeMatched.isEmpty()
                ? rawKnowledge.pages().stream().filter(page -> exactPageNames.contains(normalize(page.pageName()))).toList()
                : routeMatched;
        if (selectedPages.isEmpty()) {
            return input.flowScopedKnowledgePackage() == null ? rawKnowledge : input.flowScopedKnowledgePackage().mappedUiKnowledge();
        }
        Set<String> pageIds = selectedPages.stream().map(MappedPage::pageId).map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<PageKnowledgeGraphNode> graphNodes = rawKnowledge.graphNodes().stream()
                .filter(node -> pageIds.contains(normalize(node.pageId()))).toList();
        Set<String> graphNodeIds = graphNodes.stream().map(PageKnowledgeGraphNode::nodeId).map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<PageKnowledgeGraphEdge> graphEdges = rawKnowledge.graphEdges().stream()
                .filter(edge -> graphNodeIds.contains(normalize(edge.fromId())) && graphNodeIds.contains(normalize(edge.toId()))).toList();
        List<MappedTransition> transitions = rawKnowledge.transitions().stream()
                .filter(transition -> pageIds.contains(normalize(transition.fromPageId())) && pageIds.contains(normalize(transition.toPageId()))).toList();
        List<PageKnowledgeVectorDocument> vectorDocuments = rawKnowledge.vectorDocuments().stream()
                .filter(document -> pageIds.contains(normalize(document.sourcePageId()))).toList();
        return new MappedUiKnowledge(selectedPages, transitions, graphNodes, graphEdges, vectorDocuments);
    }

    private void addReference(Set<String> target, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private PageModel findModel(PageModelBundle models, MappedPage page) {
        return models.pages().stream()
                .filter(model -> model.pageId().equals(page.pageId()))
                .findFirst()
                .orElse(null);
    }

    private PageModelEnrichmentInput toInput(
            MappedPage page,
            PageModel model,
            PageRequirementEvidence evidence,
            String applicationHost
    ) {
        List<String> actions = page.actions().stream()
                .map(action -> action.actionName().isBlank() ? action.description() : action.actionName())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        List<String> locators = selectedLocatorFacts(page, evidence, applicationHost);
        List<String> forms = model == null ? List.of() : model.forms().stream()
                .map(form -> form.formName() + " fields=" + form.fieldElementIds() + " submit=" + form.submitElementIds())
                .toList();
        List<String> assertions = page.assertionHints().stream().map(Object::toString).limit(5).toList();
        return new PageModelEnrichmentInput(
                page.pageId(), page.pageName(), page.urlPattern(), page.title(),
                model == null ? page.pageType() : model.featureGuess(),
                actions, locators, forms, assertions,
                evidence.actions(), evidence.assertions(), evidence.actionsByRequirement(),
                evidence.postconditionsByRequirement(), evidence.testCaseIds(),
                evidence.requirementRefs(), evidence.preconditions()
        );
    }

    private List<String> selectedLocatorFacts(
            MappedPage page,
            PageRequirementEvidence evidence,
            String applicationHost
    ) {
        List<LocatorEvidence> candidates = page.elements().stream()
                .flatMap(element -> element.locatorCandidates().stream()
                        .filter(locator -> !targetsExternalOrigin(locator, applicationHost))
                        .map(locator -> new LocatorEvidence(element, locator)))
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> relevantTerms = locatorRelevantTerms(evidence);
        List<LocatorEvidence> selected = new ArrayList<>();
        candidates.stream()
                .filter(candidate -> isRequirementRelevant(candidate, relevantTerms))
                .sorted(locatorComparator(relevantTerms))
                .limit(12)
                .forEach(selected::add);

        Set<String> selectedKeys = selected.stream()
                .map(this::locatorKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        candidates.stream()
                .filter(candidate -> !selectedKeys.contains(locatorKey(candidate)))
                .sorted(locatorComparator(relevantTerms))
                .limit(Math.max(0, 20 - selected.size()))
                .forEach(selected::add);

        return selected.stream()
                .map(candidate -> locatorFact(candidate, isRequirementRelevant(candidate, relevantTerms)))
                .distinct()
                .toList();
    }

    private Comparator<LocatorEvidence> locatorComparator(Set<String> relevantTerms) {
        return Comparator
                .comparing((LocatorEvidence candidate) -> isRequirementRelevant(candidate, relevantTerms)).reversed()
                .thenComparing((LocatorEvidence candidate) -> candidate.locator().stabilityScore(), Comparator.reverseOrder())
                .thenComparing((LocatorEvidence candidate) -> candidate.locator().stableAcrossRuns(), Comparator.reverseOrder())
                .thenComparing((LocatorEvidence candidate) -> candidate.locator().uniqueOnPage(), Comparator.reverseOrder())
                .thenComparing(candidate -> candidate.element().elementId())
                .thenComparing(candidate -> candidate.locator().value());
    }

    private Set<String> locatorRelevantTerms(PageRequirementEvidence evidence) {
        Set<String> terms = new LinkedHashSet<>();
        if (evidence == null) {
            return terms;
        }
        java.util.stream.Stream.of(
                        evidence.actions(),
                        evidence.assertions(),
                        evidence.testCaseIds(),
                        evidence.requirementRefs(),
                        evidence.preconditions()
                )
                .flatMap(List::stream)
                .forEach(value -> addSearchTokens(terms, value));
        evidence.actionsByRequirement().values().stream().flatMap(List::stream)
                .forEach(value -> addSearchTokens(terms, value));
        evidence.postconditionsByRequirement().values().stream().flatMap(List::stream)
                .forEach(value -> addSearchTokens(terms, value));
        return terms;
    }

    private void addSearchTokens(Set<String> terms, String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return;
        }
        terms.add(normalized);
        java.util.regex.Matcher routeMatcher = java.util.regex.Pattern
                .compile("/[a-z0-9/_\\-]+")
                .matcher(normalized);
        while (routeMatcher.find()) {
            terms.add(routeMatcher.group());
        }
        for (String token : normalized.split("[^a-z0-9/_\\-]+")) {
            if (token.length() >= 4 || token.startsWith("/")) {
                terms.add(token);
            }
        }
    }

    private boolean isRequirementRelevant(LocatorEvidence candidate, Set<String> relevantTerms) {
        if (candidate == null || relevantTerms == null || relevantTerms.isEmpty()) {
            return false;
        }
        String locatorText = normalize(candidate.locator().value()
                + " " + candidate.locator().href()
                + " " + candidate.locator().accessibleName()
                + " " + candidate.locator().visibleText()
                + " " + candidate.element().text()
                + " " + candidate.element().semanticName()
                + " " + candidate.element().elementType());
        for (String term : relevantTerms) {
            if (!term.isBlank() && locatorText.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String locatorFact(LocatorEvidence candidate, boolean requirementRelevant) {
        LocatorCandidate locator = candidate.locator();
        return locator.strategy().wireName() + "=" + locator.value()
                + " (stability=" + locator.stabilityScore()
                + ", sameOrigin=" + locator.sameOrigin()
                + ", element=" + safe(candidate.element().text(), candidate.element().semanticName())
                + (locator.href().isBlank() ? "" : ", href=" + locator.href())
                + (locator.originHost().isBlank() ? "" : ", originHost=" + locator.originHost())
                + (requirementRelevant ? ", relevance=requirement" : "")
                + (locator.risks().isEmpty() ? "" : ", risks=" + locator.risks())
                + ")";
    }

    private String locatorKey(LocatorEvidence candidate) {
        return candidate.locator().strategy().wireName() + "=" + candidate.locator().value();
    }

    private String safe(String primary, String fallback) {
        String value = primary == null ? "" : primary.trim();
        if (!value.isBlank()) {
            return value.length() > 80 ? value.substring(0, 80) : value;
        }
        value = fallback == null ? "" : fallback.trim();
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private String applicationHost(PageModelEnrichmentInputBundle input) {
        if (input.projectProfile() == null || input.projectProfile().baseUrl() == null) {
            return "";
        }
        try {
            String host = URI.create(input.projectProfile().baseUrl()).getHost();
            return host == null ? "" : host.trim().toLowerCase();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private boolean targetsExternalOrigin(LocatorCandidate locator, String applicationHost) {
        if (locator == null || locator.value() == null) {
            return false;
        }
        if (!locator.sameOrigin()) {
            return true;
        }
        if (locator.risks().contains("external-origin") || locator.risks().contains("external-link-text-xpath")) {
            return true;
        }
        String value = locator.value().trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("https?://[^'\\\"\\]\\s]+", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        while (matcher.find()) {
            try {
                String host = URI.create(matcher.group()).getHost();
                if (host != null && (applicationHost.isBlank() || !applicationHost.equalsIgnoreCase(host))) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        }
        return false;
    }

    private PageRequirementEvidence pageOwnedEvidence(PageModelEnrichmentInputBundle input, MappedPage page) {
        if (input.canonicalTestCaseBundle() == null) {
            return PageRequirementEvidence.empty();
        }
        List<CanonicalTestCase> actionCases = input.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownsRequirementAction(page, testCase))
                .toList();
        List<CanonicalTestCase> assertionCases = input.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownsRequirementAssertion(page, testCase))
                .toList();
        List<CanonicalTestCase> ownedCases = java.util.stream.Stream.concat(actionCases.stream(), assertionCases.stream())
                .collect(java.util.stream.Collectors.toMap(
                        CanonicalTestCase::id,
                        testCase -> testCase,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ))
                .values().stream()
                .toList();
        return new PageRequirementEvidence(
                factsByRequirement(actionCases, CanonicalTestCase::actions),
                factsByRequirement(assertionCases, CanonicalTestCase::assertions),
                ownedCases.stream().map(CanonicalTestCase::id).toList(),
                ownedCases.stream().flatMap(testCase -> testCase.requirementRefs().stream()).distinct().toList(),
                ownedCases.stream().map(CanonicalTestCase::precondition)
                        .filter(value -> value != null && !value.isBlank()).distinct().toList()
        );
    }

    private Map<String, List<String>> factsByRequirement(
            List<CanonicalTestCase> testCases,
            java.util.function.Function<CanonicalTestCase, List<String>> factsExtractor
    ) {
        Map<String, List<String>> facts = new java.util.LinkedHashMap<>();
        for (CanonicalTestCase testCase : testCases) {
            List<String> values = factsExtractor.apply(testCase);
            for (String requirementId : testCase.requirementRefs()) {
                facts.merge(requirementId, values, (current, incoming) -> java.util.stream.Stream
                        .concat(current.stream(), incoming.stream())
                        .filter(value -> value != null && !value.isBlank())
                        .distinct()
                        .toList());
            }
        }
        return Map.copyOf(facts);
    }

    private boolean ownsRequirementAction(MappedPage page, CanonicalTestCase testCase) {
        return matchesOwnedPage(page, testCase.sourcePageName(), testCase.sourceRoute());
    }

    private boolean ownsRequirementAssertion(MappedPage page, CanonicalTestCase testCase) {
        return matchesOwnedPage(page, testCase.pageName(), testCase.route());
    }

    private boolean matchesOwnedPage(MappedPage page, String pageName, String route) {
        String normalizedRoute = normalize(route);
        if (!normalizedRoute.isBlank()) {
            return normalizedRoute.equals(normalize(page.urlPattern()))
                    || normalizedRoute.equals(normalize(page.url()));
        }
        return pageName != null && !pageName.isBlank() && page.pageName().equalsIgnoreCase(pageName);
    }

    private record PageRequirementEvidence(
            Map<String, List<String>> actionsByRequirement,
            Map<String, List<String>> postconditionsByRequirement,
            List<String> testCaseIds,
            List<String> requirementRefs,
            List<String> preconditions
    ) {
        private List<String> actions() {
            return flatten(actionsByRequirement);
        }

        private List<String> assertions() {
            return flatten(postconditionsByRequirement);
        }

        private static PageRequirementEvidence empty() {
            return new PageRequirementEvidence(Map.of(), Map.of(), List.of(), List.of(), List.of());
        }

        private static List<String> flatten(Map<String, List<String>> facts) {
            return facts == null ? List.of() : facts.values().stream()
                    .flatMap(List::stream)
                    .filter(value -> value != null && !value.isBlank())
                    .distinct()
                    .toList();
        }
    }

    private record LocatorEvidence(MappedElement element, LocatorCandidate locator) {
    }
}
