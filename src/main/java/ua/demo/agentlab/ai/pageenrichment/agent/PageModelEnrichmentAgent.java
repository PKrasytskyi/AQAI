package ua.demo.agentlab.ai.pageenrichment.agent;

import ua.demo.agentlab.ai.pageenrichment.cache.PageKnowledgeCacheEntry;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentFailure;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichedKnowledgeAssembler;
import ua.demo.agentlab.ai.pageenrichment.service.PageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.OpenAiPageModelEnrichmentClient;
import ua.demo.agentlab.ai.pageenrichment.service.PageEnrichmentPersistencePolicy;
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
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.PageKnowledgeFingerprintCalculator;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.UiOperationKind;

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
    private final PageEnrichmentPersistencePolicy persistencePolicy = new PageEnrichmentPersistencePolicy();
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
                WorkflowArtifact.PAGE_KNOWLEDGE_CACHE_LOOKUP,
                WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS
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
                state.getPageKnowledgeCacheLookupResult(),
                store.get(WorkflowArtifact.SPA_STRUCTURED_BEHAVIOR_BINDINGS)
                        .map(value -> (List<ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract>) value)
                        .orElse(List.of())
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
        var failureDetails = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastFailureDetails()
                : List.<PageModelEnrichmentFailure>of();
        int openAiAttempts = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastAttempts()
                : 0;
        int openAiSuccesses = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastSuccesses()
                : 0;
        int openAiFailures = Math.max(0, openAiAttempts - openAiSuccesses);
        int openAiFallbacks = openAiFailures;
        int promptChars = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastPromptChars()
                : 0;
        int responseChars = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastResponseChars()
                : 0;
        int actualInputTokens = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastActualInputTokens()
                : 0;
        int actualOutputTokens = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastActualOutputTokens()
                : 0;
        int actualTotalTokens = enrichmentClient instanceof OpenAiPageModelEnrichmentClient openAiClient
                ? openAiClient.lastActualTotalTokens()
                : 0;
        Set<String> failedPageIds = failureDetails.stream()
                .map(PageModelEnrichmentFailure::pageId)
                .collect(java.util.stream.Collectors.toSet());
        List<PageModelEnrichmentRecord> persistableRecords = records.stream()
                .filter(record -> persistencePolicy.isEligible(record, failedPageIds))
                .toList();
        return new PageModelEnrichmentOutput(
                records,
                cachedRecords,
                generatedRecords,
                knowledgeAssembler.merge(selectedKnowledge, persistableRecords),
                failures,
                failureDetails,
                openAiAttempts,
                openAiSuccesses,
                openAiFailures,
                openAiFallbacks,
                promptChars,
                responseChars,
                actualInputTokens,
                actualOutputTokens,
                actualTotalTokens
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
                .map(page -> new PageEnrichmentCandidate(page, pageOwnedEvidence(input, page)))
                .filter(candidate -> candidate.evidence().hasOwnedEvidence())
                .map(candidate -> toInput(candidate.page(), findModel(models, candidate.page()),
                        candidate.evidence(), applicationHost))
                .toList();
    }

    private List<PageModelEnrichmentRecord> cachedRecords(PageModelEnrichmentInputBundle input) {
        if (input.cacheLookupResult() == null) {
            return List.of();
        }
        MappedUiKnowledge knowledge = selectedKnowledge(input);
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return List.of();
        }
        List<PageModelEnrichmentRecord> records = new ArrayList<>();
        for (MappedPage page : knowledge.pages()) {
            PageRequirementEvidence evidence = pageOwnedEvidence(input, page);
            if (!evidence.hasOwnedEvidence()) {
                continue;
            }
            String fingerprint = fingerprintCalculator.fingerprint(page);
            input.cacheLookupResult().hitFor(page.pageId(), fingerprint)
                    .map(PageKnowledgeCacheEntry::enrichmentRecord)
                    .map(record -> rebindCachedRecord(record, page, evidence))
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
        return new PageModelEnrichmentRecord(
                safePage == null ? cached.pageId() : safePage.pageId(),
                safePage == null ? cached.pageName() : safePage.pageName(),
                safePage == null ? cached.route() : safePage.urlPattern(),
                cached.businessIntent(),
                cached.pageSummary(),
                safeEvidence.actions(),
                cached.stableLocators(),
                safeEvidence.preconditions(),
                safeEvidence.assertions(),
                cached.risks(),
                cached.coverageGaps(),
                safeEvidence.requirementRefs(),
                safeEvidence.actionsByRequirement(),
                safeEvidence.postconditionsByRequirement(),
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
        if (rawKnowledge == null) {
            return null;
        }
        if (input.finalizedBehaviorBindings() != null) {
            Set<String> finalizedPages = input.finalizedBehaviorBindings().stream()
                    .filter(binding -> !binding.pageId().isBlank())
                    .filter(binding -> binding.executable() || !binding.steps().isEmpty()
                            || binding.assertions().stream().anyMatch(assertion -> assertion.verifiable()))
                    .flatMap(binding -> java.util.stream.Stream.of(normalize(binding.pageId()), normalize(binding.route())))
                    .filter(value -> !value.isBlank())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            List<MappedPage> finalized = rawKnowledge.pages().stream()
                    .filter(page -> finalizedPages.contains(normalize(page.pageId()))
                            || finalizedPages.contains(normalize(page.urlPattern()))
                            || finalizedPages.contains(normalize(page.url())))
                    .toList();
            return subset(rawKnowledge, finalized);
        }
        if (input.uiTestPlan() == null) {
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
        return subset(rawKnowledge, selectedPages);
    }

    private MappedUiKnowledge subset(MappedUiKnowledge rawKnowledge, List<MappedPage> selectedPages) {
        if (rawKnowledge == null || selectedPages == null || selectedPages.isEmpty()) {
            return new MappedUiKnowledge(List.of(), List.of(), List.of(), List.of(), List.of());
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
        String capability = pageCapability(page, model);
        List<String> actions = scopedActionHints(page, evidence, capability);
        List<String> locators = selectedLocatorFacts(page, evidence, applicationHost);
        locators = java.util.stream.Stream
                .concat(locators.stream(), dependencyLocatorFacts(model, evidence).stream())
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(16)
                .toList();
        List<String> forms = model == null ? List.of() : model.forms().stream()
                .map(form -> form.formName() + " fields=" + form.fieldElementIds() + " submit=" + form.submitElementIds())
                .toList();
        List<String> assertions = page.assertionHints().stream()
                .map(Object::toString)
                .filter(assertion -> assertionBelongsToPage(page, assertion))
                .limit(5)
                .toList();
        List<String> knownGaps = knownGaps(locators, evidence, capability);
        return new PageModelEnrichmentInput(
                page.pageId(), page.pageName(), page.urlPattern(), page.title(),
                capability,
                actions, locators, forms, assertions,
                evidence.actions(), evidence.assertions(), evidence.actionsByRequirement(),
                evidence.postconditionsByRequirement(), evidence.testCaseIds(),
                evidence.requirementRefs(), pagePreconditions(inputPreconditions(evidence), capability),
                capability, semanticComponents(page, model), runtimeEvidence(page, capability), knownGaps
        );
    }

    private List<String> inputPreconditions(PageRequirementEvidence evidence) {
        return evidence == null ? List.of() : evidence.preconditions();
    }

    private List<String> pagePreconditions(List<String> source, String capability) {
        List<String> values = new ArrayList<>();
        values.add("Application is available");
        if (source != null) {
            values.addAll(source);
        }
        if (isAuthenticatedCapability(capability)) {
            values.add("User is authenticated");
        }
        return values.stream().filter(value -> value != null && !value.isBlank()).distinct().toList();
    }

    private List<String> knownGaps(List<String> locators, PageRequirementEvidence evidence, String capability) {
        List<String> gaps = new ArrayList<>();
        if (locators == null || locators.isEmpty()) {
            gaps.add("No mapper-approved stable locator evidence for this page");
        }
        if (evidence == null || evidence.requirementRefs().isEmpty()) {
            gaps.add("No page-owned requirement evidence selected for this page");
        }
        if (isAuthenticatedCapability(capability) && (locators == null || locators.stream()
                .noneMatch(locator -> normalize(locator).contains("logout") || normalize(locator).contains("dashboard")))) {
            gaps.add("Authenticated page has no confirmed dashboard/logout locator evidence");
        }
        return gaps.stream().distinct().toList();
    }

    private List<String> runtimeEvidence(MappedPage page, String capability) {
        List<String> evidence = new ArrayList<>();
        if (page != null && !page.urlPattern().isBlank()) {
            evidence.add("ROUTE_CONFIRMED " + page.urlPattern());
        }
        if (isAuthenticatedCapability(capability)) {
            evidence.add("AUTHENTICATED_PAGE_REQUIRES_LOGIN_FLOW");
        }
        return evidence.stream().distinct().toList();
    }

    private List<String> semanticComponents(MappedPage page, PageModel model) {
        List<String> components = new ArrayList<>();
        if (page != null) {
            page.sections().stream()
                    .map(section -> firstNonBlank(section.sectionName(), section.sectionType(), section.sectionId())
                            + " actions=" + section.elementIds().size())
                    .filter(value -> !value.isBlank())
                    .limit(6)
                    .forEach(components::add);
        }
        if (model != null) {
            model.forms().stream()
                    .map(form -> firstNonBlank(form.formName(), "FormComponent") + " actions=[SUBMIT_FORM]")
                    .limit(3)
                    .forEach(components::add);
        }
        return components.stream().filter(value -> value != null && !value.isBlank()).distinct().limit(8).toList();
    }

    private String pageCapability(MappedPage page, PageModel model) {
        String evidence = normalize((page == null ? "" : page.pageName() + " " + page.pageType() + " " + page.urlPattern())
                + " " + (model == null ? "" : model.featureGuess() + " " + model.title()));
        if (evidence.contains("login") || evidence.contains("auth/login") || evidence.contains("authentication")) {
            return "AUTHENTICATION";
        }
        if (evidence.contains("dashboard") || evidence.contains("secure") || evidence.contains("authenticated")) {
            return "AUTHENTICATED_AREA";
        }
        if (page != null && page.canonicalPageType() != null) {
            return page.canonicalPageType().name();
        }
        return firstNonBlank(page == null ? "" : page.pageType(), model == null ? "" : model.featureGuess(), "GENERIC")
                .toUpperCase(java.util.Locale.ROOT)
                .replace('-', '_');
    }

    private boolean isAuthenticatedCapability(String capability) {
        String normalized = normalize(capability);
        return normalized.contains("authenticated") || normalized.contains("dashboard") || normalized.contains("secure");
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

        return selected.stream()
                .map(candidate -> locatorFact(candidate, isRequirementRelevant(candidate, relevantTerms)))
                .distinct()
                .toList();
    }

    private List<String> dependencyLocatorFacts(PageModel model, PageRequirementEvidence evidence) {
        if (model == null || !needsUserMenuTrigger(evidence)) {
            return List.of();
        }
        List<String> facts = new ArrayList<>();
        for (PageElementModel element : model.elements()) {
            String elementText = normalize(String.join(" ",
                    element.elementId(),
                    element.semanticType(),
                    element.technicalType(),
                    element.name(),
                    element.text(),
                    element.cssClass()
            ));
            if (!containsAny(elementText, "user-menu-trigger", "user menu trigger", "userdropdown", "oxd-userdropdown")) {
                continue;
            }
            PageLocatorModel locator = preferredUserMenuLocator(element);
            if (locator == null || locator.value().isBlank()) {
                continue;
            }
            facts.add(locator.strategy().toLowerCase(java.util.Locale.ROOT) + "=" + locator.value()
                    + " (stability=" + Math.max(locator.score(), 0.78d)
                    + ", sameOrigin=true"
                    + ", element=User menu trigger"
                    + ", relevance=requirement"
                    + ", dependency=required-for-logout-menu-flow"
                    + ")");
        }
        return facts.stream().distinct().limit(2).toList();
    }

    private boolean needsUserMenuTrigger(PageRequirementEvidence evidence) {
        String text = evidenceText(evidence);
        return containsAny(text, "logout", "sign out")
                && containsAny(text, "user menu", "menu", "dropdown", "drop-down");
    }

    private PageLocatorModel preferredUserMenuLocator(PageElementModel element) {
        if (element == null) {
            return null;
        }
        return element.locatorCandidates().stream()
                .filter(locator -> containsAny(normalize(locator.value()),
                        "userdropdown",
                        "oxd-userdropdown-tab",
                        "user-menu",
                        "dropdown-tab"))
                .findFirst()
                .orElseGet(() -> element.bestLocator() != null
                        && containsAny(normalize(element.bestLocator().value()), "userdropdown", "oxd-userdropdown-tab")
                        ? element.bestLocator()
                        : null);
    }

    private List<String> scopedActionHints(MappedPage page, PageRequirementEvidence evidence, String capability) {
        List<String> hints = new ArrayList<>();
        String text = evidenceText(evidence);
        if ("AUTHENTICATION".equals(capability)) {
            if (containsAny(text, "username", "credential", "login")) {
                hints.add("enterUsername");
            }
            if (containsAny(text, "password", "credential", "login")) {
                hints.add("enterPassword");
            }
            if (containsAny(text, "submit", "login", "authenticate")) {
                hints.add("clickLoginButton");
                hints.add("login");
            }
        } else if (isAuthenticatedCapability(capability)) {
            if (containsAny(text, "user menu", "drop-down", "dropdown", "open menu")) {
                hints.add("openUserMenu");
            }
            if (containsAny(text, "logout", "sign out")) {
                hints.add("logout");
            }
        }
        if (hints.isEmpty() && evidence != null && !evidence.actions().isEmpty()) {
            evidence.actions().stream()
                    .filter(action -> actionBelongsToPage(page, action))
                    .limit(5)
                    .forEach(hints::add);
        }
        return hints.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    private String evidenceText(PageRequirementEvidence evidence) {
        if (evidence == null) {
            return "";
        }
        return normalize(java.util.stream.Stream.of(
                        evidence.actions(),
                        evidence.assertions(),
                        evidence.preconditions()
                )
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.joining(" ")));
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
            if ((token.length() >= 4 || token.startsWith("/")) && !isLocatorStopWord(token)) {
                terms.add(token);
            }
        }
    }

    private boolean isLocatorStopWord(String value) {
        String token = normalize(value);
        return Set.of(
                "open", "click", "module", "page", "route", "user", "authenticated", "application",
                "navigation", "navigate", "visible", "displayed", "content", "action", "control",
                "expected", "result", "requirement", "current", "target", "source", "available"
        ).contains(token);
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
        if (containsAnyToken(relevantTerms, "logout", "sign out")
                && containsAny(locatorText, "logout", "auth/logout")) {
            return true;
        }
        if (containsAnyToken(relevantTerms, "user menu", "menu", "dropdown", "drop-down")
                && containsAny(locatorText, "userdropdown", "user dropdown", "oxd-userdropdown", "dropdown tab")) {
            return true;
        }
        if (containsAnyToken(relevantTerms, "dashboard", "/dashboard/index")
                && containsAny(locatorText, "dashboard", "/dashboard/index")) {
            return true;
        }
        for (String term : relevantTerms) {
            if (!term.isBlank() && locatorText.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyToken(Set<String> terms, String... needles) {
        if (terms == null || terms.isEmpty()) {
            return false;
        }
        String joined = normalize(String.join(" ", terms));
        return containsAny(joined, needles);
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

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
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
                .filter(testCase -> ownsPrimaryActionRequirement(page, testCase))
                .toList();
        List<CanonicalTestCase> assertionCases = input.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownsRequirementAssertion(page, testCase))
                .toList();
        Map<String, List<String>> actionFacts = factsByRequirement(
                actionCases,
                CanonicalTestCase::actions,
                value -> actionBelongsToPage(page, value)
        );
        Map<String, List<String>> assertionFacts = factsByRequirement(
                assertionCases,
                CanonicalTestCase::assertions,
                value -> assertionBelongsToPage(page, value)
        );
        Set<String> ownedRequirementIds = java.util.stream.Stream
                .concat(actionFacts.keySet().stream(), assertionFacts.keySet().stream())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<String> ownedTestCaseIds = java.util.stream.Stream.concat(actionCases.stream(), assertionCases.stream())
                .filter(testCase -> testCase.requirementRefs().stream().anyMatch(ownedRequirementIds::contains))
                .map(CanonicalTestCase::id)
                .distinct()
                .toList();
        List<String> preconditions = java.util.stream.Stream.concat(actionCases.stream(), assertionCases.stream())
                .filter(testCase -> testCase.requirementRefs().stream().anyMatch(ownedRequirementIds::contains))
                .map(CanonicalTestCase::precondition)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        return new PageRequirementEvidence(
                actionFacts,
                assertionFacts,
                ownedTestCaseIds,
                List.copyOf(ownedRequirementIds),
                preconditions
        );
    }

    private Map<String, List<String>> factsByRequirement(
            List<CanonicalTestCase> testCases,
            java.util.function.Function<CanonicalTestCase, List<String>> factsExtractor,
            java.util.function.Predicate<String> factFilter
    ) {
        Map<String, List<String>> facts = new java.util.LinkedHashMap<>();
        for (CanonicalTestCase testCase : testCases) {
            List<String> values = factsExtractor.apply(testCase).stream()
                    .filter(value -> factFilter == null || factFilter.test(value))
                    .toList();
            if (values.isEmpty()) {
                continue;
            }
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

    private boolean actionBelongsToPage(MappedPage page, String action) {
        if (page == null || action == null || action.isBlank()) {
            return false;
        }
        String normalized = normalize(action);
        String capability = pageCapability(page, null);
        if ("AUTHENTICATION".equals(capability)) {
            return containsAny(normalized, "username", "password", "credential", "login", "submit", "auth");
        }
        if (isAuthenticatedCapability(capability)) {
            return containsAny(normalized, "dashboard", "authenticated", "logout", "welcome", "user menu", "navigation");
        }
        return routeOrPageMentioned(page, normalized) || !containsAny(normalized, "username", "password", "login", "logout");
    }

    private boolean assertionBelongsToPage(MappedPage page, String assertion) {
        if (page == null || assertion == null || assertion.isBlank()) {
            return false;
        }
        String normalized = normalize(assertion);
        String route = normalize(page.urlPattern());
        String capability = pageCapability(page, null);
        if (!route.isBlank() && normalized.contains(route)) {
            return true;
        }
        if ("AUTHENTICATION".equals(capability)) {
            return containsAny(normalized, "username", "password", "login button", "login page", "login route", "auth/login")
                    && !containsAny(normalized, "dashboard", "authenticated area", "logout", "welcome");
        }
        if (isAuthenticatedCapability(capability)) {
            return containsAny(normalized, "dashboard", "authenticated area", "authenticated route", "logout", "welcome", "successful login")
                    && !containsAny(normalized, "username field", "password field", "login button", "login page route", "/auth/login");
        }
        return routeOrPageMentioned(page, normalized);
    }

    private boolean routeOrPageMentioned(MappedPage page, String normalizedText) {
        String route = normalize(page.urlPattern());
        String pageName = normalize(page.pageName()).replace("page", "").trim();
        return (!route.isBlank() && normalizedText.contains(route))
                || (!pageName.isBlank() && normalizedText.contains(pageName));
    }

    private boolean containsAny(String value, String... needles) {
        String normalized = normalize(value);
        for (String needle : needles) {
            if (!normalize(needle).isBlank() && normalized.contains(normalize(needle))) {
                return true;
            }
        }
        return false;
    }

    private boolean ownsRequirementAction(MappedPage page, CanonicalTestCase testCase) {
        return matchesOwnedPage(page, testCase.sourcePageName(), testCase.sourceRoute());
    }

    private boolean ownsPrimaryActionRequirement(MappedPage page, CanonicalTestCase testCase) {
        if (matchesOwnedPage(page, testCase.pageName(), testCase.route())) {
            return true;
        }
        String capability = pageCapability(page, null);
        if (!"AUTHENTICATION".equals(capability)) {
            return false;
        }
        String text = normalize(testCase.title() + " "
                + String.join(" ", testCase.actions()) + " "
                + String.join(" ", testCase.assertions()));
        if (containsAny(text, "authenticated area route", "welcome message", "logout action is visible",
                "authenticated route matches", "authenticated area displays")) {
            return false;
        }
        boolean hasLoginOperation = testCase.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.AUTHENTICATE
                        || intent.kind() == UiOperationKind.SUBMIT_FORM);
        return hasLoginOperation && containsAny(text, "valid credentials", "redirected", "submit the login form",
                "submit the target form", "login form");
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

        private boolean hasOwnedEvidence() {
            return !requirementRefs.isEmpty() && (!actionsByRequirement.isEmpty()
                    || !postconditionsByRequirement.isEmpty());
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

    private record PageEnrichmentCandidate(MappedPage page, PageRequirementEvidence evidence) {
    }
}
