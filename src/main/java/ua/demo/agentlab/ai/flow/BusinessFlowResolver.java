package ua.demo.agentlab.ai.flow;

import ua.demo.agentlab.futurefeat.testplan.model.TestScenario;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.catalog.PageCapability;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BusinessFlowResolver {

    private static final Pattern ROUTE_PATTERN = Pattern.compile("/[a-zA-Z0-9/_\\-]+");

    public BusinessFlowContext resolve(WorkflowState state) {
        if (state == null) {
            return resolve((FlowScopedKnowledgeInput) null);
        }
        return resolve(new FlowScopedKnowledgeInput(
                state.getObjective(),
                state.getProjectProfile(),
                state.getTestPlan(),
                state.getNormalizedRequirementBundle(),
                state.getCanonicalTestCaseBundle(),
                state.getMappedUiKnowledge(),
                state.getKnowledgeRunMetadata()
        ));
    }

    public BusinessFlowContext resolve(FlowScopedKnowledgeInput input) {
        Set<String> targetRoutes = new LinkedHashSet<>();
        Set<String> targetPageNames = new LinkedHashSet<>();
        Set<PageCapability> targetCapabilities = new LinkedHashSet<>();
        Set<String> targetOperations = new LinkedHashSet<>();
        Set<String> requiredTerms = new LinkedHashSet<>();
        Set<String> excludedTerms = new LinkedHashSet<>(List.of(
                "about", "blog", "contact", "footer", "header", "privacy", "policy", "terms", "career", "careers"
        ));
        List<String> notes = new ArrayList<>();

        if (input != null && input.projectProfile() != null) {
            addIfPresent(targetRoutes, input.projectProfile().homeRoute());
            addIfPresent(requiredTerms, input.projectProfile().projectName());
        }

        if (input != null && input.normalizedRequirementBundle() != null) {
            for (NormalizedRequirement requirement : input.normalizedRequirementBundle().requirements()) {
                addRequirementSignals(requirement, targetOperations, targetRoutes, targetCapabilities, requiredTerms, excludedTerms);
            }
        }

        if (input != null && input.testPlan() != null) {
            for (TestScenario scenario : input.testPlan().scenarios()) {
                addScenarioSignals(scenario, targetOperations, targetRoutes, targetCapabilities, requiredTerms);
            }
        }
        if (input != null && input.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : input.canonicalTestCaseBundle().testCases()) {
                addCanonicalTestCaseSignals(testCase, targetOperations, targetRoutes, targetCapabilities, requiredTerms);
            }
        }

        addProjectProfileRoutesForDetectedFlow(
                input == null ? null : input.projectProfile(),
                targetOperations,
                targetRoutes,
                targetCapabilities,
                targetPageNames,
                requiredTerms
        );
        inferTargetPageArchetypes(targetOperations, targetCapabilities, targetPageNames);

        if (targetOperations.isEmpty()) {
            targetOperations.addAll(List.of(
                    "OPEN_PAGE",
                    "INSPECT_PAGE_CONTENT"
            ));
            notes.add("Fallback target operations applied");
        }

        requiredTerms.removeIf(String::isBlank);
        excludedTerms.removeIf(term -> term.isBlank() || requiredTerms.contains(term));
        notes.add("Business flow resolved from normalized requirements and optional planning artifacts");

        return new BusinessFlowContext(
                "default-business-flow",
                input == null ? "" : input.objective(),
                List.copyOf(targetPageNames),
                List.copyOf(targetRoutes),
                List.copyOf(targetCapabilities),
                List.copyOf(targetOperations),
                List.copyOf(requiredTerms),
                List.copyOf(excludedTerms),
                List.copyOf(notes)
        );
    }

    private void addRequirementSignals(
            NormalizedRequirement requirement,
            Set<String> targetOperations,
            Set<String> targetRoutes,
            Set<PageCapability> targetCapabilities,
            Set<String> requiredTerms,
            Set<String> excludedTerms
    ) {
        if (requirement == null) {
            return;
        }

        String text = normalize(requirement.title() + " " + requirement.statement() + " " + String.join(" ", requirement.tags()));
        tokenize(text).forEach(requiredTerms::add);
        extractRoutes(text).forEach(targetRoutes::add);

        if (containsAny(text, "open", "navigate", "reach", "visit", "go to", "access")) {
            targetOperations.add("OPEN_PAGE");
        }
        if (containsAny(text, "public", "reachable without authentication", "without authentication", "without login", "guest")) {
            targetOperations.add("VERIFY_PAGE_ACCESS");
            targetCapabilities.add(PageCapability.NAVIGATION);
            requiredTerms.add("public");
        }
        if (containsAny(text, "list", "listing", "grid", "table", "catalog", "results", "overview", "collection")) {
            targetOperations.add("INSPECT_COLLECTION");
            targetCapabilities.add(PageCapability.RECORD_LIST);
        }
        if (containsAny(text, "card", "row", "tile", "record", "item summary", "entity information", "record information")
                || (text.contains("entry") && !text.contains("entry point"))) {
            targetOperations.add("INSPECT_ENTITY_SUMMARY");
            targetCapabilities.add(PageCapability.RECORD_LIST);
        }
        if (containsAny(text, "content", "details content", "information", "field", "summary content")) {
            targetOperations.add("INSPECT_PAGE_CONTENT");
            targetOperations.add("REVIEW_ENTITY_CONTENT");
        }
        if (containsAny(text, "details", "drill down", "open item", "open entity", "open record", "open details")) {
            targetOperations.add("OPEN_DETAILS");
            targetCapabilities.add(PageCapability.RECORD_DETAILS);
        }
        if (containsAny(text, "login", "sign in", "authenticate", "authentication")) {
            targetOperations.add("AUTHENTICATE");
            targetCapabilities.add(PageCapability.AUTHENTICATION);
            targetCapabilities.add(PageCapability.AUTHENTICATED_AREA);
        }
        if (containsAny(text, "submit", "save", "send", "create", "update", "form")) {
            targetOperations.add("SUBMIT_FORM");
            targetCapabilities.add(PageCapability.FORM);
        }
        if (containsAny(text, "search", "find", "lookup", "query")) {
            targetOperations.add("SEARCH");
        }
        if (containsAny(text, "filter", "refine")) {
            targetOperations.add("FILTER");
        }
        if (containsAny(text, "sort", "order by")) {
            targetOperations.add("SORT");
        }
        if (containsAny(text, "add") && containsAny(text, "cart", "basket", "container", "wishlist", "favorites", "queue", "selection")) {
            targetOperations.add("ADD_ENTITY_TO_CONTAINER");
            targetOperations.add("OPEN_TARGET_CONTAINER");
            targetCapabilities.add(PageCapability.CONTAINER);
        }
        if (containsAny(text, "present") && containsAny(text, "cart", "basket", "container", "wishlist", "favorites", "queue", "selection")) {
            targetOperations.add("OPEN_TARGET_CONTAINER");
            targetCapabilities.add(PageCapability.CONTAINER);
        }
        if (containsAny(text, "remove") && containsAny(text, "cart", "basket", "container", "wishlist", "favorites", "queue", "selection")) {
            targetOperations.add("REMOVE_ENTITY_FROM_CONTAINER");
            targetOperations.add("OPEN_TARGET_CONTAINER");
            targetCapabilities.add(PageCapability.CONTAINER);
        }
        if (containsAny(text, "upload", "attach", "import")) {
            targetOperations.add("UPLOAD_FILE");
        }
        if (containsAny(text, "download", "export")) {
            targetOperations.add("DOWNLOAD_FILE");
        }
        if (containsAny(text, "logout", "log out", "sign out")) {
            targetOperations.add("LOGOUT");
            targetCapabilities.add(PageCapability.AUTHENTICATED_AREA);
        }

        if (text.contains("out of scope")) {
            if (containsAny(text, "checkout")) {
                excludedTerms.add("checkout");
            }
            if (containsAny(text, "registration", "register", "signup")) {
                excludedTerms.add("registration");
                excludedTerms.add("register");
                excludedTerms.add("signup");
            }
            if (containsAny(text, "payment")) {
                excludedTerms.add("payment");
            }
            if (containsAny(text, "profile")) {
                excludedTerms.add("profile");
            }
        }
    }

    private void addScenarioSignals(
            TestScenario scenario,
            Set<String> targetOperations,
            Set<String> targetRoutes,
            Set<PageCapability> targetCapabilities,
            Set<String> requiredTerms
    ) {
        if (scenario == null) {
            return;
        }

        String text = normalize(scenario.title() + " " + scenario.expectedResult());
        tokenize(text).forEach(requiredTerms::add);
        extractRoutes(text).forEach(targetRoutes::add);

        if (containsAny(text, "details")) {
            targetOperations.add("OPEN_DETAILS");
            targetCapabilities.add(PageCapability.RECORD_DETAILS);
        }
        if (containsAny(text, "search", "find", "lookup")) {
            targetOperations.add("SEARCH");
        }
        if (containsAny(text, "filter", "refine")) {
            targetOperations.add("FILTER");
        }
        if (containsAny(text, "sort")) {
            targetOperations.add("SORT");
        }
        if (containsAny(text, "list", "listing", "grid", "table", "collection")) {
            targetOperations.add("INSPECT_COLLECTION");
            targetCapabilities.add(PageCapability.RECORD_LIST);
        }
        if (containsAny(text, "card", "row", "record", "entry")) {
            targetOperations.add("INSPECT_ENTITY_SUMMARY");
            targetCapabilities.add(PageCapability.RECORD_LIST);
        }
        if (containsAny(text, "add") && containsAny(text, "cart", "basket", "container", "wishlist", "favorites")) {
            targetOperations.add("ADD_ENTITY_TO_CONTAINER");
            targetOperations.add("OPEN_TARGET_CONTAINER");
            targetCapabilities.add(PageCapability.CONTAINER);
        }
        if (containsAny(text, "remove") && containsAny(text, "cart", "basket", "container", "wishlist", "favorites")) {
            targetOperations.add("REMOVE_ENTITY_FROM_CONTAINER");
            targetOperations.add("OPEN_TARGET_CONTAINER");
            targetCapabilities.add(PageCapability.CONTAINER);
        }
    }

    private void addCanonicalTestCaseSignals(
            CanonicalTestCase testCase,
            Set<String> targetOperations,
            Set<String> targetRoutes,
            Set<PageCapability> targetCapabilities,
            Set<String> requiredTerms
    ) {
        if (testCase == null) {
            return;
        }

        String text = normalize(testCase.title()
                + " "
                + String.join(" ", testCase.llmSteps())
                + " "
                + String.join(" ", testCase.requirementRefs()));
        tokenize(text).forEach(requiredTerms::add);
        extractRoutes(text).forEach(targetRoutes::add);
        addIfPresent(targetRoutes, testCase.route());
        addIfPresent(targetRoutes, testCase.sourceRoute());

        testCase.operationIntents().forEach(intent -> {
            if (intent != null && intent.kind() != null) {
                targetOperations.add(intent.kind().name());
                addIfPresent(targetRoutes, intent.target());
                addCapabilityForOperation(targetCapabilities, intent.kind().name());
            }
        });
    }

    private void inferTargetPageArchetypes(
            Set<String> operations,
            Set<PageCapability> targetCapabilities,
            Set<String> targetPageNames
    ) {
        operations.forEach(operation -> addCapabilityForOperation(targetCapabilities, operation));
        for (PageCapability capability : targetCapabilities) {
            targetPageNames.add(capability.defaultPageName());
        }
        if (targetCapabilities.contains(PageCapability.DASHBOARD)) {
            targetPageNames.add(PageCapability.DASHBOARD.defaultPageName());
        }
        if (operations.contains("AUTHENTICATE")) {
            targetPageNames.add(PageCapability.AUTHENTICATION.defaultPageName());
            targetPageNames.add(PageCapability.AUTHENTICATED_AREA.defaultPageName());
        }
        if (operations.contains("OPEN_PAGE") && targetPageNames.isEmpty()) {
            targetPageNames.add(PageCapability.NAVIGATION.defaultPageName());
        }
    }

    private void addCapabilityForOperation(Set<PageCapability> targetCapabilities, String operation) {
        if (operation == null) {
            return;
        }
        switch (operation) {
            case "AUTHENTICATE" -> {
                targetCapabilities.add(PageCapability.AUTHENTICATION);
                targetCapabilities.add(PageCapability.AUTHENTICATED_AREA);
            }
            case "SUBMIT_FORM", "UPLOAD_FILE" -> targetCapabilities.add(PageCapability.FORM);
            case "OPEN_DETAILS" -> targetCapabilities.add(PageCapability.RECORD_DETAILS);
            case "OPEN_TARGET_CONTAINER", "ADD_ENTITY_TO_CONTAINER", "REMOVE_ENTITY_FROM_CONTAINER" ->
                    targetCapabilities.add(PageCapability.CONTAINER);
            case "INSPECT_COLLECTION", "INSPECT_ENTITY_SUMMARY", "SEARCH", "FILTER", "SORT" ->
                    targetCapabilities.add(PageCapability.RECORD_LIST);
            case "LOGOUT" -> targetCapabilities.add(PageCapability.AUTHENTICATED_AREA);
            case "OPEN_PAGE", "VERIFY_PAGE_ACCESS", "INSPECT_PAGE_CONTENT" ->
                    targetCapabilities.add(PageCapability.NAVIGATION);
            default -> {
            }
        }
    }

    private void addProjectProfileRoutesForDetectedFlow(
            ua.demo.agentlab.config.ProjectProfile profile,
            Set<String> targetOperations,
            Set<String> targetRoutes,
            Set<PageCapability> targetCapabilities,
            Set<String> targetPageNames,
            Set<String> requiredTerms
    ) {
        if (profile == null) {
            return;
        }
        boolean authenticationFlow = targetOperations.contains("AUTHENTICATE")
                || requiredTerms.contains("authenticated")
                || requiredTerms.contains("authentication")
                || requiredTerms.contains("credentials")
                || requiredTerms.contains("welcome");
        if (authenticationFlow) {
            addIfPresent(targetRoutes, profile.loginRoute());
            addIfPresent(targetRoutes, profile.authenticatedRoute());
            addIfPresent(targetRoutes, profile.securityRoute());
            targetCapabilities.add(PageCapability.AUTHENTICATION);
            targetCapabilities.add(PageCapability.AUTHENTICATED_AREA);
            targetPageNames.add(PageCapability.AUTHENTICATION.defaultPageName());
            targetPageNames.add(PageCapability.AUTHENTICATED_AREA.defaultPageName());
            requiredTerms.add("secure");
            requiredTerms.add("logout");
        }

        boolean containerFlow = targetOperations.contains("OPEN_TARGET_CONTAINER")
                || targetOperations.contains("ADD_ENTITY_TO_CONTAINER")
                || targetOperations.contains("REMOVE_ENTITY_FROM_CONTAINER");
        if (containerFlow) {
            addIfPresent(targetRoutes, profile.cartRoute());
            targetCapabilities.add(PageCapability.CONTAINER);
        }

        boolean listingFlow = targetOperations.contains("INSPECT_COLLECTION")
                || targetOperations.contains("SEARCH")
                || targetOperations.contains("FILTER")
                || targetOperations.contains("SORT");
        if (listingFlow) {
            addIfPresent(targetRoutes, profile.catalogRoute());
            targetCapabilities.add(PageCapability.RECORD_LIST);
        }
    }

    private List<String> extractRoutes(String text) {
        List<String> routes = new ArrayList<>();
        Matcher matcher = ROUTE_PATTERN.matcher(text);
        while (matcher.find()) {
            String route = matcher.group();
            if (route.length() > 1) {
                routes.add(route);
            }
        }
        return routes;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        for (String token : normalize(text).replaceAll("[^a-z0-9/_\\- ]", " ").split("\\s+")) {
            if (!token.isBlank() && (token.length() >= 3 || token.startsWith("/"))) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean containsAny(String text, String... fragments) {
        for (String fragment : fragments) {
            if (text.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
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
}
