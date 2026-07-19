package ua.demo.agentlab.ui.discovery.interaction.inventory;

import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;
import ua.demo.agentlab.ui.discovery.component.model.ScopedLocatorCandidate;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentModel;
import ua.demo.agentlab.ui.discovery.component.model.SemanticComponentPageModel;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateActionEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.CandidateLocatorEvidence;
import ua.demo.agentlab.ui.discovery.spa.model.SemanticComponentInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceStatus;
import ua.demo.agentlab.ui.discovery.spa.SpaDiscoveryMode;
import ua.demo.agentlab.ui.discovery.spa.SpaInventoryConfig;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;
import ua.demo.agentlab.ui.discovery.semantic.model.ActionCandidate;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticPageModel;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.requirements.behavior.StructuredBehaviorContract;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds the single candidate-only interaction inventory for both document-navigation and SPA sites.
 * Promotion to POM evidence belongs to targeted live verification, never inventory construction.
 */
public class UiInteractionInventoryBuilder {

    private final ComponentBoundaryDetector componentBoundaryDetector;
    private final SemanticActionModelBuilder semanticActionModelBuilder;
    private final UiInteractionInventoryInvariantGate invariantGate;

    public UiInteractionInventoryBuilder() {
        this(new ComponentBoundaryDetector(), new SemanticActionModelBuilder(),
                new UiInteractionInventoryInvariantGate());
    }

    public UiInteractionInventoryBuilder(
            ComponentBoundaryDetector componentBoundaryDetector,
            SemanticActionModelBuilder semanticActionModelBuilder
    ) {
        this(componentBoundaryDetector, semanticActionModelBuilder, new UiInteractionInventoryInvariantGate());
    }

    UiInteractionInventoryBuilder(
            ComponentBoundaryDetector componentBoundaryDetector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            UiInteractionInventoryInvariantGate invariantGate
    ) {
        this.componentBoundaryDetector = componentBoundaryDetector == null
                ? new ComponentBoundaryDetector() : componentBoundaryDetector;
        this.semanticActionModelBuilder = semanticActionModelBuilder == null
                ? new SemanticActionModelBuilder() : semanticActionModelBuilder;
        this.invariantGate = invariantGate == null ? new UiInteractionInventoryInvariantGate() : invariantGate;
    }

    public UiInteractionInventory build(
            PageModelBundle pageModels,
            MappedUiKnowledge mappedKnowledge,
            KnowledgeRunMetadata metadata,
            SpaInventoryConfig config
    ) {
        return build(pageModels, mappedKnowledge, metadata, config, null, List.of());
    }

    public UiInteractionInventory build(
            PageModelBundle pageModels,
            MappedUiKnowledge mappedKnowledge,
            KnowledgeRunMetadata metadata,
            SpaInventoryConfig config,
            CanonicalTestCaseBundle canonicalTestCases
    ) {
        return build(pageModels, mappedKnowledge, metadata, config, canonicalTestCases, List.of());
    }

    public UiInteractionInventory build(
            PageModelBundle pageModels,
            MappedUiKnowledge mappedKnowledge,
            KnowledgeRunMetadata metadata,
            SpaInventoryConfig config,
            CanonicalTestCaseBundle canonicalTestCases,
            List<StructuredBehaviorContract> structuredContracts
    ) {
        SpaInventoryConfig effectiveConfig = config == null
                ? new SpaInventoryConfig(false, SpaDiscoveryMode.INVENTORY, 30, true, 0.80d, 2, 2,
                false, false, false, 14, 30, false)
                : config;
        if (pageModels == null || pageModels.pages().isEmpty()) {
            return UiInteractionInventory.empty(effectiveConfig.mode(), "interaction-inventory:no-page-models");
        }

        ComponentDiscoveryModel componentModel = componentBoundaryDetector.detect(pageModels);
        SemanticActionModel actionModel = semanticActionModelBuilder.build(pageModels, mappedKnowledge);
        Map<String, SemanticComponentPageModel> componentsByPage = indexComponents(componentModel);
        Map<String, SemanticPageModel> actionsByPage = indexActions(actionModel);

        List<PageModel> scopedPageModels = scopedPages(
                pageModels.pages(), effectiveConfig.mode(), canonicalTestCases, structuredContracts);
        List<UiInteractionPage> pages = scopedPageModels.stream()
                .sorted(Comparator.comparing(PageModel::pageId))
                .map(page -> inventoryForPage(
                        page,
                        componentsByPage.get(page.pageId()),
                        actionsByPage.get(page.pageId()),
                        metadata,
                        effectiveConfig
                ))
                .toList();
        UiInteractionInventory inventory = new UiInteractionInventory(
                UiInteractionInventory.SCHEMA_VERSION,
                effectiveConfig.mode(),
                pages,
                List.of(
                        "interaction-inventory:generic-core",
                        effectiveConfig.spaExtensionsEnabled()
                                ? "interaction-inventory:spa-extensions-enabled"
                                : "interaction-inventory:spa-extensions-disabled",
                        effectiveConfig.mode() == SpaDiscoveryMode.INVENTORY || effectiveConfig.mode() == SpaDiscoveryMode.FORCE
                                ? "interaction-inventory:full-discovery" : "interaction-inventory:requirement-scoped",
                        "mode=" + effectiveConfig.mode().name().toLowerCase(Locale.ROOT),
                        "candidate-only-no-pom-promotion",
                        "component-pages=" + componentModel.pages().size(),
                        "semantic-pages=" + actionModel.pages().size(),
                        "inventory-pages=" + scopedPageModels.size()
                )
        );
        invariantGate.enforce(inventory);
        return inventory;
    }

    private List<PageModel> scopedPages(
            List<PageModel> pages,
            SpaDiscoveryMode mode,
            CanonicalTestCaseBundle testCases,
            List<StructuredBehaviorContract> structuredContracts
    ) {
        if (mode == SpaDiscoveryMode.INVENTORY || mode == SpaDiscoveryMode.FORCE) {
            return pages;
        }
        List<StructuredBehaviorContract> contracts = structuredContracts == null ? List.of() : structuredContracts;
        List<CanonicalTestCase> cases = testCases == null ? List.of() : testCases.testCases();
        if (cases.isEmpty() && contracts.isEmpty()) return List.of();

        Set<String> selectedIds = new LinkedHashSet<>();
        pages.stream()
                .filter(page -> cases.stream().anyMatch(testCase -> pageMatchesCase(page, testCase))
                        || contracts.stream().anyMatch(contract -> pageMatchesContract(page, contract)))
                .map(PageModel::pageId)
                .forEach(selectedIds::add);

        // A requirement can describe a protected target by capability/name while discovery proves its route
        // through a transition. Include only the discovered transition targets whose label/destination matches
        // a meaningful requirement token; never synthesize a product route.
        boolean changed;
        do {
            changed = false;
            for (PageModel source : pages) {
                if (!selectedIds.contains(source.pageId())) continue;
                for (PageFlowModel flow : source.flows()) {
                    if (!flow.success() || !flowMatchesContracts(flow, contracts)) continue;
                    String targetId = flow.toPageId();
                    if (!targetId.isBlank() && selectedIds.add(targetId)) changed = true;
                }
            }
        } while (changed);
        return pages.stream().filter(page -> selectedIds.contains(page.pageId())).toList();
    }

    private boolean pageMatchesContract(PageModel page, StructuredBehaviorContract contract) {
        if (contract == null) return false;
        String evidence = normalize(String.join(" ", page.pageId(), page.route(), page.url(), page.visibleText()));
        return meaningfulTokens(contract.targetContext() + " " + String.join(" ", contract.actions())).stream()
                .anyMatch(token -> evidence.contains(token));
    }

    private boolean flowMatchesContracts(PageFlowModel flow, List<StructuredBehaviorContract> contracts) {
        if (contracts.isEmpty()) return false;
        String evidence = normalize(String.join(" ", flow.actionLabel(), flow.toPageId(), flow.toUrl()));
        return contracts.stream().flatMap(contract -> meaningfulTokens(
                        contract.targetContext() + " " + String.join(" ", contract.actions())).stream())
                .anyMatch(evidence::contains);
    }

    private Set<String> meaningfulTokens(String text) {
        Set<String> excluded = Set.of("target", "source", "route", "page", "component", "capability", "discovery",
                "confirmed", "authentication", "authenticated", "application", "navigation", "visible", "open");
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalize(text).split("[^a-z0-9]+")) {
            if (token.length() >= 4 && !excluded.contains(token)) tokens.add(token);
        }
        return tokens;
    }

    private boolean pageMatchesCase(PageModel page, CanonicalTestCase testCase) {
        return routeMatches(page.route(), testCase.route())
                || routeMatches(page.route(), testCase.sourceRoute())
                || pageNameMatches(page.pageId(), testCase.pageName())
                || pageNameMatches(page.pageId(), testCase.sourcePageName())
                || testCase.targetPages().stream().anyMatch(target -> pageNameMatches(page.pageId(), target));
    }

    private boolean routeMatches(String pageRoute, String candidateRoute) {
        String left = normalize(pageRoute);
        String right = normalize(candidateRoute);
        return !left.isBlank() && !right.isBlank() && (left.equals(right) || left.endsWith(right) || right.endsWith(left));
    }

    private boolean pageNameMatches(String pageId, String pageName) {
        String left = normalize(pageId).replaceAll("[^a-z0-9]", "");
        String right = normalize(pageName).replaceAll("[^a-z0-9]", "");
        return !left.isBlank() && !right.isBlank() && (left.equals(right) || left.contains(right) || right.contains(left));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private UiInteractionPage inventoryForPage(
            PageModel page,
            SemanticComponentPageModel componentPage,
            SemanticPageModel semanticPage,
            KnowledgeRunMetadata metadata,
            SpaInventoryConfig config
    ) {
        List<SemanticComponentModel> sourceComponents = componentPage == null
                ? List.of() : componentPage.components();
        Map<String, List<ActionCandidate>> actionsByElement = actionsByElement(semanticPage);
        List<SemanticComponentInventory> components = sourceComponents.stream()
                .sorted(Comparator.comparingDouble(SemanticComponentModel::confidence).reversed()
                        .thenComparing(SemanticComponentModel::componentId))
                .limit(config.maxComponents())
                .map(component -> inventoryComponent(page, component, actionsByElement))
                .toList();
        String capability = derivedCapabilities(page, semanticPage, components);
        String pageName = resolvedPageName(page, semanticPage);
        String fingerprint = fingerprint(page, components);
        return new UiInteractionPage(
                page.pageId(), pageName, page.route(), capability, fingerprint, metadata, components,
                List.of("interaction-inventory:page-model", "page-url=" + page.url(), "candidate-status-only")
        );
    }

    /**
     * Page-model labels are intentionally low-level (for example, "overview" or "detail").
     * The inventory exposes a small, evidence-derived capability set so requirement binding can
     * remain product-neutral while still requiring concrete component proof.
     */
    private String derivedCapabilities(
            PageModel page,
            SemanticPageModel semanticPage,
            List<SemanticComponentInventory> components
    ) {
        Set<String> capabilities = new LinkedHashSet<>();
        String primary = semanticPage == null || semanticPage.capability().isBlank()
                ? page.featureGuess() : semanticPage.capability();
        if (primary != null && !primary.isBlank()) {
            capabilities.add(primary.trim());
        }

        Set<ua.demo.agentlab.ui.discovery.component.model.ComponentType> componentTypes = components.stream()
                .map(SemanticComponentInventory::type)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.NAVIGATION)) {
            capabilities.add("MODULE_NAVIGATION");
        }
        if (componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.FILTER_PANEL)
                && (componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.RESULTS_COLLECTION)
                || componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.TABLE))) {
            capabilities.add("RECORD_LIST");
        }
        if (componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.FILTER_PANEL)) {
            capabilities.add("FILTER");
        }
        // A generic page header plus a footer/navigation link is not authentication evidence.
        // Preserve AUTHENTICATED_AREA only when upstream semantic classification already proved it
        // or a user-menu component provides concrete session UI evidence.
        if (normalize(primary).replace('-', '_').replace(' ', '_').contains("authenticated_area")
                || componentTypes.contains(ua.demo.agentlab.ui.discovery.component.model.ComponentType.USER_MENU)) {
            capabilities.add("AUTHENTICATED_AREA");
        }
        return String.join("|", capabilities);
    }

    private SemanticComponentInventory inventoryComponent(
            PageModel page,
            SemanticComponentModel component,
            Map<String, List<ActionCandidate>> actionsByElement
    ) {
        List<CandidateLocatorEvidence> locators = component.locators().stream()
                .sorted(Comparator.comparing(ScopedLocatorCandidate::elementId)
                        .thenComparing(ScopedLocatorCandidate::strategy)
                        .thenComparing(ScopedLocatorCandidate::value))
                .map(locator -> locatorEvidence(component.componentId(), locator))
                .toList();
        Map<String, List<String>> locatorIdsByElement = locatorIdsByElement(locators);
        List<CandidateActionEvidence> actions = component.elementIds().stream()
                .flatMap(elementId -> actionsByElement.getOrDefault(elementId, List.of()).stream())
                .sorted(Comparator.comparing(ActionCandidate::action).thenComparing(ActionCandidate::targetElementId))
                .map(action -> actionEvidence(component.componentId(), page.route(), action,
                        locatorIdsByElement.getOrDefault(action.targetElementId(), List.of())))
                .toList();
        return new SemanticComponentInventory(
                component.componentId(), component.name(), component.type(),
                component.rootLocatorStrategy(), component.rootLocatorValue(), "", component.elementIds(),
                component.confidence(), locators, actions, component.risks(), component.sourceTrace()
        );
    }

    private CandidateLocatorEvidence locatorEvidence(String componentId, ScopedLocatorCandidate locator) {
        return new CandidateLocatorEvidence(
                componentId + ":locator:" + sanitize(locator.elementId() + "-" + locator.strategy() + "-" + locator.value()),
                componentId, locator.elementId(), locator.strategy(), locator.value(), locator.finalScore(), sameOrigin(locator),
                locator.globalMatchCount(), locator.scopedMatchCount(), locator.uniqueOnPage(),
                locator.uniqueWithinComponent(), locator.stableAcrossRuns(), locator.evidenceType(),
                SpaEvidenceStatus.CANDIDATE, locator.risks()
        );
    }

    private boolean sameOrigin(ScopedLocatorCandidate locator) {
        if (locator == null) return false;
        String value = locator.value() == null ? "" : locator.value().toLowerCase(Locale.ROOT);
        String strategy = locator.strategy() == null ? "" : locator.strategy().toLowerCase(Locale.ROOT);
        // XPath starts with //; that syntax is not an origin indicator. Origin is relevant only
        // when a selector itself embeds a navigation URL. Live verification re-checks the
        // resolved href before executing a navigation action.
        if ("xpath".equals(strategy)) {
            return !value.contains("http://") && !value.contains("https://");
        }
        return !value.contains("http://") && !value.contains("https://") && !value.contains("href^='//")
                && !value.contains("href^=\"//");
    }

    private CandidateActionEvidence actionEvidence(
            String componentId,
            String route,
            ActionCandidate action,
            List<String> requiredLocatorIds
    ) {
        List<String> sourceTrace = new ArrayList<>(action.evidence());
        sourceTrace.add("interaction-inventory:semantic-action");
        return new CandidateActionEvidence(
                componentId + ":action:" + sanitize(action.action() + "-" + action.targetElementId()),
                componentId, action.action(), action.targetElementId(), action.confidence(), requiredLocatorIds,
                route == null || route.isBlank() ? List.of("page route is active") : List.of("route=" + route),
                List.of(), sourceTrace, SpaEvidenceStatus.CANDIDATE
        );
    }

    private Map<String, SemanticComponentPageModel> indexComponents(ComponentDiscoveryModel model) {
        Map<String, SemanticComponentPageModel> result = new LinkedHashMap<>();
        if (model != null) {
            model.pages().forEach(page -> result.putIfAbsent(page.pageId(), page));
        }
        return result;
    }

    private Map<String, SemanticPageModel> indexActions(SemanticActionModel model) {
        Map<String, SemanticPageModel> result = new LinkedHashMap<>();
        if (model != null) {
            model.pages().forEach(page -> result.putIfAbsent(page.pageId(), page));
        }
        return result;
    }

    private Map<String, List<ActionCandidate>> actionsByElement(SemanticPageModel page) {
        Map<String, List<ActionCandidate>> result = new LinkedHashMap<>();
        if (page == null) {
            return result;
        }
        // pageActionCandidates is a compact summary capped for reporting. Interaction inventory must keep
        // every component-owned action; requirement scoping happens later in SourceStateBinding.
        page.elements().forEach(element -> element.actionCandidates().forEach(action ->
                result.computeIfAbsent(action.targetElementId(), ignored -> new ArrayList<>()).add(action)));
        return result;
    }

    private Map<String, List<String>> locatorIdsByElement(List<CandidateLocatorEvidence> locators) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (CandidateLocatorEvidence locator : locators) {
            result.computeIfAbsent(locator.elementId(), ignored -> new ArrayList<>()).add(locator.locatorId());
        }
        return result;
    }

    private String fingerprint(PageModel page, List<SemanticComponentInventory> components) {
        Set<String> material = new LinkedHashSet<>();
        material.add(page.pageId());
        material.add(page.route());
        for (SemanticComponentInventory component : components) {
            material.add(component.componentId());
            for (CandidateLocatorEvidence locator : component.locators()) {
                material.add(locator.strategy() + "|" + locator.value());
            }
        }
        return sha256(String.join("\n", material));
    }

    private String inferredPageName(PageModel page) {
        String route = page.route().replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (route.isBlank()) {
            return page.pageId();
        }
        StringBuilder name = new StringBuilder();
        for (String part : route.split("\\s+")) {
            name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.append("Page").toString();
    }

    private String resolvedPageName(PageModel page, SemanticPageModel semanticPage) {
        String semanticName = semanticPage == null ? "" : semanticPage.pageName();
        if (!genericAuthenticatedName(semanticName)) {
            return semanticName == null || semanticName.isBlank() ? inferredPageName(page) : semanticName;
        }
        String routeName = meaningfulRoutePageName(page.route());
        return routeName.isBlank() ? semanticName : routeName;
    }

    private boolean genericAuthenticatedName(String value) {
        String normalized = sanitize(value).replace("-", "");
        return normalized.equals("secureareapage") || normalized.equals("authenticatedpage")
                || normalized.equals("authenticatedareapage");
    }

    private String meaningfulRoutePageName(String route) {
        List<String> segments = java.util.Arrays.stream((route == null ? "" : route).split("/+"))
                .map(this::sanitize)
                .filter(value -> !value.isBlank())
                .filter(value -> !Set.of("web", "index", "php", "view", "secure", "auth", "authenticated").contains(value))
                .toList();
        if (segments.isEmpty()) return "";
        String token = segments.get(segments.size() - 1);
        StringBuilder name = new StringBuilder();
        for (String part : token.split("-+")) {
            if (!part.isBlank()) name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return name.isEmpty() ? "" : name.append("Page").toString();
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte valueByte : bytes) {
                result.append(String.format("%02x", valueByte));
            }
            return result.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private String sanitize(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        normalized = normalized.replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "evidence" : normalized;
    }
}
