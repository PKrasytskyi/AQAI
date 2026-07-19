package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.futurefeat.testplan.model.TestScenario;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.catalog.ConfirmedPageCandidate;
import ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver;
import ua.demo.agentlab.ui.catalog.ConfirmedRouteGuard;
import ua.demo.agentlab.ui.catalog.PageCapability;
import ua.demo.agentlab.ui.catalog.PageSource;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphEdge;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeGraphNode;
import ua.demo.agentlab.ui.discovery.mapping.model.PageKnowledgeVectorDocument;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;
import ua.demo.agentlab.ui.flow.model.CanonicalPage;
import ua.demo.agentlab.ui.flow.model.CanonicalPageFlowModel;
import ua.demo.agentlab.ai.context.slicing.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TargetAwareContextSlicer {

    private final PromptUiEvidenceBuilder promptUiEvidenceBuilder = new PromptUiEvidenceBuilder();
    private final TargetPageScopeResolver targetScopeResolver = new TargetPageScopeResolver();
    private final RequirementScopeSlicer requirementSlicer = new RequirementScopeSlicer();
    private final FlowScopeSlicer flowSlicer = new FlowScopeSlicer();
    private final AssertionScopeSlicer assertionSlicer = new AssertionScopeSlicer();
    private final RetrievalScopeSlicer retrievalSlicer = new RetrievalScopeSlicer();
    private final EnrichmentScopeSlicer enrichmentSlicer = new EnrichmentScopeSlicer();
    private final ScopedContextAssembler contextAssembler = new ScopedContextAssembler(promptUiEvidenceBuilder);

    public AiContextPackage slice(AiContextPackage context, AiContextScope scope) {
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (scope == null) {
            return context;
        }

        TargetPageScopeResolver.TargetPageScope targetScope = targetScopeResolver.resolve(context, scope);
        NormalizedRequirementBundle requirements = requirementSlicer.slice(context.normalizedRequirementBundle(), scope);
        CanonicalPageFlowModel flows = flowSlicer.slice(context.canonicalPageFlowModel(), targetScope);
        MappedUiKnowledge mappedUiKnowledge = sliceMappedKnowledge(context, scope, targetScope.routeGuard());
        PageModelBundle pageModelBundle = slicePageModels(context, scope, mappedUiKnowledge, targetScope.routeGuard());
        CanonicalUiInteractionModel canonicalInteractions = sliceCanonicalInteractions(context, scope);
        UiKnowledgeRetrievalContext retrievalContext = retrievalSlicer.slice(context, targetScope);
        List<PageModelEnrichmentRecord> pageModelEnrichments = enrichmentSlicer.slice(context.pageModelEnrichments(), targetScope);
        TestPlan testPlan = sliceTestPlan(context, scope);
        CanonicalTestCaseBundle canonicalTestCaseBundle = sliceCanonicalTestCaseBundle(context, scope);
        List<AssertionContract> assertionContracts = assertionSlicer.slice(context.assertionContracts(), scope,
                canonicalTestCaseBundle);
        UiTestPlan uiTestPlan = sliceUiTestPlan(context, scope);

        MappedUiKnowledgeCurated curatedScope = sliceCuratedKnowledge(context, mappedUiKnowledge);
        RetrievalScopeSlicer.LocatorEvidenceScope locatorEvidence =
                retrievalSlicer.sliceLocatorEvidence(context, mappedUiKnowledge);
        return contextAssembler.assemble(context, new ScopedContextAssembler.ScopedContextParts(requirements,
                testPlan, canonicalTestCaseBundle, uiTestPlan, flows, mappedUiKnowledge, curatedScope,
                pageModelBundle, canonicalInteractions, retrievalContext, assertionContracts, pageModelEnrichments,
                locatorEvidence.dbStable(), locatorEvidence.confirmedCatalog()));
    }

    private MappedUiKnowledgeCurated sliceCuratedKnowledge(AiContextPackage context, MappedUiKnowledge mappedUiKnowledge) {
        if (context.mappedUiKnowledgeCurated() == null) {
            return new MappedUiKnowledgeCurated(mappedUiKnowledge, List.of(), List.of("curated-scope:no-parent-curated"), 0.0d);
        }
        Set<String> pageIds = mappedUiKnowledge == null
                ? Set.of()
                : mappedUiKnowledge.pages().stream()
                .map(MappedPage::pageId)
                .map(this::normalize)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new MappedUiKnowledgeCurated(
                mappedUiKnowledge,
                context.mappedUiKnowledgeCurated().excludedEvidence().stream()
                        .filter(evidence -> pageIds.isEmpty() || pageIds.contains(normalize(evidence.pageId())))
                        .toList(),
                context.mappedUiKnowledgeCurated().sourceTrace(),
                context.mappedUiKnowledgeCurated().confidence()
        );
    }

    private MappedUiKnowledge sliceMappedKnowledge(AiContextPackage context, AiContextScope scope,
                                                    ConfirmedRouteGuard guard) {
        if (context.mappedUiKnowledge() == null) {
            return null;
        }
        boolean strictPageScope = isPageObjectScope(scope);

        List<MappedPage> directPages = context.mappedUiKnowledge().pages().stream()
                .filter(page -> mappedPageMatchesScope(page, scope))
                .filter(page -> !guard.hasConfirmedPages() || confirmedMappedPage(guard, page))
                .toList();
        if (strictPageScope) {
            List<MappedPage> routePages = context.mappedUiKnowledge().pages().stream()
                    .filter(page -> !guard.hasConfirmedPages() || confirmedMappedPage(guard, page))
                    .filter(page -> routeMatchesAny(scope.targetRoutes(), page.urlPattern())
                            || routeMatchesAny(scope.targetRoutes(), page.url()))
                    .toList();
            if (!scope.targetRoutes().isEmpty()) {
                directPages = routePages;
            } else {
                List<MappedPage> namedPages = directPages.stream()
                        .filter(page -> mappedPageNameMatchesScope(page, scope))
                        .toList();
                if (!namedPages.isEmpty()) {
                    directPages = namedPages;
                }
            }
        }

        if (directPages.isEmpty() && scope.targetPageNames().isEmpty() && !strictPageScope) {
            directPages = context.mappedUiKnowledge().pages().stream().limit(2).toList();
        }

        Set<String> directPageIds = directPages.stream()
                .map(MappedPage::pageId)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> relatedPageIds = new LinkedHashSet<>(directPageIds);
        if (!strictPageScope) {
            context.mappedUiKnowledge().transitions().stream()
                    .filter(transition -> directPageIds.contains(normalize(transition.fromPageId()))
                            || directPageIds.contains(normalize(transition.toPageId())))
                    .forEach(transition -> {
                        addIfPresent(relatedPageIds, transition.fromPageId());
                        addIfPresent(relatedPageIds, transition.toPageId());
                    });
        }

        List<MappedPage> pages = context.mappedUiKnowledge().pages().stream()
                .filter(page -> relatedPageIds.contains(normalize(page.pageId())))
                .filter(page -> !guard.hasConfirmedPages() || confirmedMappedPage(guard, page))
                .toList();

        Set<String> pageIds = pages.stream()
                .map(MappedPage::pageId)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<MappedTransition> transitions = context.mappedUiKnowledge().transitions().stream()
                .filter(transition -> strictPageScope
                        ? pageIds.contains(normalize(transition.fromPageId()))
                        && pageIds.contains(normalize(transition.toPageId()))
                        : pageIds.contains(normalize(transition.fromPageId()))
                        || pageIds.contains(normalize(transition.toPageId())))
                .toList();

        List<PageKnowledgeGraphNode> graphNodes = context.mappedUiKnowledge().graphNodes().stream()
                .filter(node -> pageIds.contains(normalize(node.pageId())))
                .toList();

        Set<String> graphNodeIds = graphNodes.stream()
                .map(PageKnowledgeGraphNode::nodeId)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        List<PageKnowledgeGraphEdge> graphEdges = context.mappedUiKnowledge().graphEdges().stream()
                .filter(edge -> graphNodeIds.contains(normalize(edge.fromId()))
                        && graphNodeIds.contains(normalize(edge.toId())))
                .toList();

        List<PageKnowledgeVectorDocument> vectorDocuments = context.mappedUiKnowledge().vectorDocuments().stream()
                .filter(document -> pageIds.contains(normalize(document.sourcePageId())))
                .limit(12)
                .toList();

        return new MappedUiKnowledge(pages, transitions, graphNodes, graphEdges, vectorDocuments);
    }

    private PageModelBundle slicePageModels(
            AiContextPackage context,
            AiContextScope scope,
            MappedUiKnowledge slicedMappedKnowledge,
            ConfirmedRouteGuard guard
    ) {
        if (context.pageModelBundle() == null || context.pageModelBundle().pages().isEmpty()) {
            return new PageModelBundle(List.of());
        }

        Set<String> mappedPageIds = slicedMappedKnowledge == null
                ? Set.of()
                : slicedMappedKnowledge.pages().stream()
                .map(MappedPage::pageId)
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<String> relatedPageIds = new LinkedHashSet<>(mappedPageIds);
        if (slicedMappedKnowledge != null && !isPageObjectScope(scope)) {
            slicedMappedKnowledge.transitions().forEach(transition -> {
                addIfPresent(relatedPageIds, transition.fromPageId());
                addIfPresent(relatedPageIds, transition.toPageId());
            });
        }

        List<PageModel> pages = context.pageModelBundle().pages().stream()
                .filter(page -> pageModelMatchesScope(page, scope, relatedPageIds))
                .filter(page -> !guard.hasConfirmedPages() || confirmedPageModel(guard, page))
                .toList();
        if (isPageObjectScope(scope) && scopeHasRootRoute(scope)) {
            List<PageModel> namedPages = pages.stream()
                    .filter(page -> pageModelNameMatchesScope(page, scope))
                    .toList();
            if (!namedPages.isEmpty()) {
                pages = namedPages;
            }
        }

        if (pages.isEmpty() && scope.targetPageNames().isEmpty() && !isPageObjectScope(scope)) {
            pages = context.pageModelBundle().pages().stream().limit(2).toList();
        }

        return new PageModelBundle(pages);
    }

    private TestPlan sliceTestPlan(AiContextPackage context, AiContextScope scope) {
        if (context.testPlan() == null) {
            return null;
        }

        Set<String> requirementSourceReferences = resolveRequirementSourceReferences(context, scope);
        List<TestScenario> scenarios = context.testPlan().scenarios().stream()
                .filter(scenario -> requirementSourceReferences.isEmpty()
                        || matchesRequirementSource(scenario.sourceReference(), requirementSourceReferences))
                .toList();
        if (scenarios.isEmpty()) {
            scenarios = context.testPlan().scenarios().stream().limit(12).toList();
        }

        return new TestPlan(
                context.testPlan().objective(),
                context.testPlan().source(),
                context.testPlan().functionalAreas(),
                scenarios,
                context.testPlan().assumptions(),
                context.testPlan().risks()
        );
    }

    private CanonicalUiInteractionModel sliceCanonicalInteractions(AiContextPackage context, AiContextScope scope) {
        if (context.canonicalInteractionModel() == null || context.canonicalInteractionModel().interactions().isEmpty()) {
            return CanonicalUiInteractionModel.empty();
        }

        List<CanonicalUiInteraction> interactions = context.canonicalInteractionModel().interactions().stream()
                .filter(interaction -> canonicalInteractionMatchesScope(interaction, scope))
                .toList();
        if (interactions.isEmpty()) {
            interactions = context.canonicalInteractionModel().interactions().stream().limit(12).toList();
        }
        return new CanonicalUiInteractionModel(interactions);
    }

    private boolean confirmedMappedPage(ConfirmedRouteGuard guard, MappedPage page) {
        return page != null && (guard.isConfirmed(page.pageName(), page.urlPattern())
                || guard.isConfirmed(page.pageName(), page.url()));
    }

    private boolean confirmedPageModel(ConfirmedRouteGuard guard, PageModel page) {
        return page != null && (guard.isConfirmed(page.pageId(), page.route())
                || guard.isConfirmed(page.pageId(), page.url())
                || guard.isConfirmed(page.title(), page.route())
                || guard.isConfirmed(page.title(), page.url()));
    }

    private UiTestPlan sliceUiTestPlan(AiContextPackage context, AiContextScope scope) {
        if (context.uiTestPlan() == null) {
            return null;
        }

        List<UiTestScenario> scenarios = context.uiTestPlan().scenarios().stream()
                .filter(scenario -> uiScenarioMatchesScope(scenario, scope))
                .filter(scenario -> !unsupportedOptionalCapability(context, scenarioText(scenario)))
                .toList();

        List<String> pageNames = scenarios.stream()
                .flatMap(scenario -> java.util.stream.Stream.of(
                        scenario.sourcePageName(),
                        scenario.pageName()
                ))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        return new UiTestPlan(
                context.uiTestPlan().sourceTestPlan(),
                pageNames.isEmpty() ? context.uiTestPlan().targetPage() : pageNames.get(0),
                pageNames,
                scenarios
        );
    }

    private CanonicalTestCaseBundle sliceCanonicalTestCaseBundle(AiContextPackage context, AiContextScope scope) {
        if (context.canonicalTestCaseBundle() == null) {
            return null;
        }

        List<CanonicalTestCase> testCases = context.canonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> canonicalTestCaseMatchesScope(testCase, scope))
                .filter(testCase -> !unsupportedOptionalCapability(context, testCaseText(testCase)))
                .toList();

        List<String> pageNames = testCases.stream()
                .flatMap(testCase -> java.util.stream.Stream.concat(
                        testCase.targetPages().stream(),
                        java.util.stream.Stream.of(testCase.sourcePageName(), testCase.pageName())
                ))
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        String primaryPage = pageNames.isEmpty()
                ? context.canonicalTestCaseBundle().primaryPage()
                : pageNames.get(0);

        return new CanonicalTestCaseBundle(
                context.canonicalTestCaseBundle().source(),
                primaryPage,
                pageNames,
                testCases
        );
    }

    private boolean mappedPageMatchesScope(MappedPage page, AiContextScope scope) {
        return scope.targetPageNames().stream().anyMatch(reference -> PageReferenceMatcher.matches(page, reference))
                || routeMatchesAny(scope.targetRoutes(), page.urlPattern())
                || routeMatchesAny(scope.targetRoutes(), page.url());
    }

    private boolean mappedPageNameMatchesScope(MappedPage page, AiContextScope scope) {
        if (page == null) {
            return false;
        }
        return scope.targetPageNames().stream().anyMatch(reference -> {
            String normalizedReference = PageReferenceMatcher.normalize(reference);
            return PageReferenceMatcher.normalize(page.pageName()).equals(normalizedReference)
                    || PageReferenceMatcher.normalize(page.pageId()).equals(normalizedReference)
                    || page.pageIdentity() != null && page.pageIdentity().matches(reference);
        });
    }

    private boolean pageModelMatchesScope(PageModel page, AiContextScope scope, Set<String> mappedPageIds) {
        if (page == null) {
            return false;
        }
        String pageId = normalize(page.pageId());
        if (isPageObjectScope(scope) && !scope.targetRoutes().isEmpty()) {
            return mappedPageIds.contains(pageId)
                    || routeMatchesAny(scope.targetRoutes(), page.route())
                    || routeMatchesAny(scope.targetRoutes(), page.url());
        }
        return mappedPageIds.contains(pageId)
                || scope.targetPageNames().stream().anyMatch(reference -> PageReferenceMatcher.matches(
                null,
                null,
                page.pageId(),
                page.route(),
                page.url(),
                page.title(),
                page.featureGuess(),
                reference
        ))
                || routeMatchesAny(scope.targetRoutes(), page.route())
                || routeMatchesAny(scope.targetRoutes(), page.url());
    }

    private boolean pageModelNameMatchesScope(PageModel page, AiContextScope scope) {
        if (page == null) {
            return false;
        }
        return scope.targetPageNames().stream().anyMatch(reference -> {
            String normalizedReference = PageReferenceMatcher.normalize(reference);
            return PageReferenceMatcher.normalize(page.pageId()).equals(normalizedReference)
                    || PageReferenceMatcher.normalize(page.title()).equals(normalizedReference);
        });
    }

    private boolean scopeHasRootRoute(AiContextScope scope) {
        return scope != null && scope.targetRoutes().stream()
                .anyMatch(route -> PageReferenceMatcher.routeMatches(route, "/"));
    }

    private boolean uiScenarioMatchesScope(UiTestScenario scenario, AiContextScope scope) {
        if (!scope.targetScenarioIds().isEmpty()) {
            return scope.targetScenarioIds().contains(normalize(scenario.id()));
        }
        return scope.targetScenarioIds().contains(normalize(scenario.id()))
                || scope.targetPageNames().stream().anyMatch(reference ->
                        PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), reference)
                                || PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), reference)
                                || (scenario.prerequisite() != null
                                && PageReferenceMatcher.matchesScenarioPage(
                                        scenario.prerequisite().sourcePageName(),
                                        scenario.prerequisite().sourceRoute(),
                                        reference)))
                || routeMatchesAny(scope.targetRoutes(), scenario.route())
                || routeMatchesAny(scope.targetRoutes(), scenario.sourceRoute());
    }

    private boolean canonicalTestCaseMatchesScope(CanonicalTestCase testCase, AiContextScope scope) {
        if (!scope.targetScenarioIds().isEmpty()) {
            return scope.targetScenarioIds().contains(normalize(testCase.id()));
        }
        return scope.targetScenarioIds().contains(normalize(testCase.id()))
                || scope.targetPageNames().stream().anyMatch(reference ->
                        PageReferenceMatcher.matchesScenarioPage(testCase.pageName(), testCase.route(), reference)
                                || PageReferenceMatcher.matchesScenarioPage(testCase.sourcePageName(), testCase.sourceRoute(), reference)
                                || testCase.targetPages().stream()
                                .anyMatch(targetPage -> PageReferenceMatcher.matchesScenarioPage(targetPage, testCase.route(), reference)))
                || routeMatchesAny(scope.targetRoutes(), testCase.route())
                || routeMatchesAny(scope.targetRoutes(), testCase.sourceRoute());
    }

    private boolean unsupportedOptionalCapability(AiContextPackage context, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(java.util.Locale.ROOT);
        if ((normalized.contains("registration") || normalized.contains("register"))
                && !hasMappedCapability(context, PageCapability.REGISTRATION)) {
            return true;
        }
        if ((normalized.contains("forgot password") || normalized.contains("password recovery") || normalized.contains("recovery"))
                && !hasMappedCapability(context, PageCapability.RECOVERY)) {
            return true;
        }
        return false;
    }

    private boolean hasMappedCapability(AiContextPackage context, PageCapability capability) {
        if (context == null || context.mappedUiKnowledge() == null || capability == null) {
            return false;
        }
        return context.mappedUiKnowledge().pages().stream()
                .anyMatch(page -> page.canonicalPageType() == capability.canonicalPageType());
    }

    private String scenarioText(UiTestScenario scenario) {
        if (scenario == null) {
            return "";
        }
        return String.join(" ",
                safe(scenario.title()),
                safe(scenario.canonicalFlowType()),
                String.join(" ", scenario.actions()),
                String.join(" ", scenario.assertions())
        );
    }

    private String testCaseText(CanonicalTestCase testCase) {
        if (testCase == null) {
            return "";
        }
        return String.join(" ",
                safe(testCase.title()),
                safe(testCase.canonicalFlowType()),
                String.join(" ", testCase.actions()),
                String.join(" ", testCase.assertions())
        );
    }

    private boolean isPageObjectScope(AiContextScope scope) {
        return scope != null && "page-object-spec".equalsIgnoreCase(scope.stage());
    }

    private boolean canonicalInteractionMatchesScope(CanonicalUiInteraction interaction, AiContextScope scope) {
        return scope.targetPageNames().contains(normalize(interaction.pageName()))
                || scope.targetPageNames().contains(normalize(interaction.pageId()))
                || scope.targetPageNames().contains(normalize(interaction.targetPageId()))
                || scope.targetPageNames().contains(normalize(interaction.subjectType()))
                || scope.targetPageNames().contains(normalize(interaction.targetType()))
                || routeMatchesAny(scope.targetRoutes(), interaction.targetRoute())
                || scope.targetRoutes().stream().anyMatch(route -> interaction.keywords().stream()
                .map(this::normalize)
                .anyMatch(route::equals))
                || scope.targetRoutes().stream().anyMatch(route -> interaction.domainHints().stream()
                .map(this::normalize)
                .anyMatch(route::equals));
    }

    private boolean matchesRequirementSource(String sourceReference, Set<String> targetRequirementSources) {
        if (sourceReference == null || sourceReference.isBlank()) {
            return false;
        }
        String normalizedSource = normalize(sourceReference);
        return targetRequirementSources.stream().anyMatch(normalizedSource::contains);
    }

    private Set<String> resolveRequirementSourceReferences(AiContextPackage context, AiContextScope scope) {
        Set<String> references = new LinkedHashSet<>();
        if (context.normalizedRequirementBundle() == null) {
            return references;
        }

        for (NormalizedRequirement requirement : context.normalizedRequirementBundle().requirements()) {
            if (scope.targetRequirementIds().contains(requirement.id()) && requirement.sourceReference() != null) {
                String source = normalize(requirement.sourceReference().source());
                if (!source.isBlank()) {
                    references.add(source);
                }
                if (requirement.sourceReference().startLine() > 0) {
                    references.add("%s [L%d]".formatted(source, requirement.sourceReference().startLine()));
                }
            }
        }

        return references;
    }

    private void addIfPresent(Set<String> values, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            values.add(normalized);
        }
    }

    private boolean routeMatchesAny(Iterable<String> routeReferences, String candidateRoute) {
        if (routeReferences == null || candidateRoute == null || candidateRoute.isBlank()) {
            return false;
        }
        for (String routeReference : routeReferences) {
            if (PageReferenceMatcher.routeMatches(candidateRoute, routeReference)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
