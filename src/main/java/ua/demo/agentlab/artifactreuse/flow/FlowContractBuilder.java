package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.UiOperationIntent;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedTransition;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class FlowContractBuilder {

    public FlowContractBundle build(FlowContractBuilderInput input) {
        if (input == null || input.canonicalTestCases() == null) {
            return new FlowContractBundle("flow-contract-bundle.v1", input == null ? null : input.runMetadata(), List.of());
        }
        Map<String, Draft> drafts = new LinkedHashMap<>();
        for (CanonicalTestCase testCase : input.canonicalTestCases().testCases()) {
            add(testCase, input, drafts);
        }
        List<FlowContract> contracts = drafts.values().stream()
                .map(draft -> materialize(draft, input))
                .sorted(Comparator.comparing(FlowContract::flowId))
                .toList();
        return new FlowContractBundle("flow-contract-bundle.v1", input.runMetadata(), contracts);
    }

    private void add(CanonicalTestCase testCase, FlowContractBuilderInput input, Map<String, Draft> drafts) {
        if (testCase == null || (testCase.operationIntents().isEmpty() && testCase.actions().isEmpty())) {
            return;
        }
        List<FlowActionType> actions = actions(testCase);
        if (actions.isEmpty()) {
            return;
        }
        FlowContractType type = type(actions);
        FlowEndpoint source = new FlowEndpoint(testCase.sourcePageName(), testCase.sourceRoute());
        FlowEndpoint target = new FlowEndpoint(firstNonBlank(testCase.pageName(), testCase.sourcePageName()),
                firstNonBlank(testCase.route(), testCase.sourceRoute()));
        if (type == FlowContractType.LOGOUT) {
            String owner = testCase.operationIntents().stream()
                    .filter(intent -> intent != null && intent.kind() == UiOperationKind.LOGOUT)
                    .map(UiOperationIntent::target)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(testCase.pageName());
            if (!PageReferenceMatcher.matchesScenarioPage(testCase.sourcePageName(), testCase.sourceRoute(), owner)) {
                source = new FlowEndpoint(owner, routeFor(owner, testCase));
                target = new FlowEndpoint(testCase.sourcePageName(), testCase.sourceRoute());
            }
        }
        FlowEndpoint flowSource = source;
        FlowEndpoint flowTarget = target;
        String key = type + "|" + endpointKey(flowSource) + "|" + endpointKey(flowTarget);
        Draft draft = drafts.computeIfAbsent(key, ignored -> new Draft(type, flowSource, flowTarget));
        testCase.requirementRefs().forEach(draft.requirementIds::add);
        if (!testCase.id().isBlank()) {
            draft.requirementIds.add(testCase.id());
        }
        draft.assertions.addAll(testCase.assertions());
        draft.evidence.add("requirement:" + firstNonBlank(testCase.id(), String.join(",", testCase.requirementRefs())));
        appendIntentSteps(testCase, draft);
        appendSupplementalSteps(testCase, draft);
    }

    private void appendIntentSteps(CanonicalTestCase testCase, Draft draft) {
        int order = draft.steps.size() + 1;
        for (UiOperationIntent intent : testCase.operationIntents()) {
            if (intent == null || intent.kind() == null) {
                continue;
            }
            FlowActionType action = FlowActionType.from(intent.kind());
            String owner = firstNonBlank(intent.target(), ownerPage(intent.kind(), testCase));
            String route = routeFor(owner, testCase);
            draft.addStep(new FlowContractStep(order++, action, owner, route, intent.dataKey(), false));
        }
    }

    private void appendSupplementalSteps(CanonicalTestCase testCase, Draft draft) {
        String text = normalize(testCase.title() + " " + String.join(" ", testCase.actions()));
        int order = draft.steps.size() + 1;
        String owner = firstNonBlank(testCase.pageName(), testCase.sourcePageName());
        String route = routeFor(owner, testCase);
        if (containsAny(text, "open user menu", "open menu", "dropdown menu")) {
            draft.addStep(new FlowContractStep(order++, FlowActionType.OPEN_MENU, owner, route, "", false));
        }
        if (containsAny(text, "select ", "dropdown", "option")) {
            draft.addStep(new FlowContractStep(order++, FlowActionType.SELECT_OPTION, owner, route, "", false));
        }
        if (containsAny(text, "checkbox", "toggle", "check ", "uncheck")) {
            draft.addStep(new FlowContractStep(order++, FlowActionType.TOGGLE_CONTROL, owner, route, "", false));
        }
        if (containsAny(text, "clear field", "clear input")) {
            draft.addStep(new FlowContractStep(order++, FlowActionType.CLEAR_FIELD, owner, route, "", false));
        }
    }

    private FlowContract materialize(Draft draft, FlowContractBuilderInput input) {
        Evidence evidence = evidence(draft, input);
        List<FlowContractStep> steps = renumber(draft.steps.values());
        String id = flowId(draft.type, draft.source, draft.target);
        return new FlowContract(
                "flow-contract.v1",
                id,
                displayName(draft.type, draft.source, draft.target),
                draft.type,
                draft.source,
                draft.target,
                requiredStates(draft.type, steps),
                producedStates(draft.type, steps),
                steps,
                List.copyOf(draft.assertions),
                List.copyOf(draft.requirementIds),
                evidence.sources(),
                List.of(),
                evidence.confidence(),
                evidence.confirmed() ? FlowContractStatus.CONFIRMED : FlowContractStatus.NEEDS_REVIEW
        );
    }

    private Evidence evidence(Draft draft, FlowContractBuilderInput input) {
        List<String> sources = new ArrayList<>(draft.evidence);
        List<MappedPage> pages = input.mappedUiKnowledge() == null ? List.of() : input.mappedUiKnowledge().pages();
        boolean sourcePage = matchesPage(pages, draft.source);
        boolean targetPage = matchesPage(pages, draft.target);
        if (sourcePage) {
            sources.add("page:" + endpointKey(draft.source));
        }
        if (targetPage) {
            sources.add("page:" + endpointKey(draft.target));
        }
        boolean transition = matchesTransition(input, draft.source, draft.target);
        if (transition) {
            sources.add("transition:" + endpointKey(draft.source) + "->" + endpointKey(draft.target));
        }
        boolean form = hasFormEvidence(pages, draft.source, draft.target);
        if (form) {
            sources.add("form:" + endpointKey(draft.source));
        }
        boolean action = hasActionEvidence(pages, draft.steps.values());
        if (action) {
            sources.add("action-evidence:" + draft.type.name());
        }
        double confidence = 0.35d + (sourcePage ? 0.15d : 0.0d) + (targetPage ? 0.15d : 0.0d);
        boolean confirmed;
        if (isTransitionFlow(draft.type)) {
            confidence += transition ? 0.30d : 0.0d;
            confirmed = sourcePage && targetPage && transition;
        } else if (isFormFlow(draft.type)) {
            confidence += form ? 0.30d : 0.0d;
            confidence += action ? 0.05d : 0.0d;
            confirmed = (sourcePage || targetPage) && form;
        } else {
            confidence += action ? 0.25d : 0.0d;
            confirmed = (sourcePage || targetPage) && action;
        }
        return new Evidence(Math.min(1.0d, confidence), confirmed, List.copyOf(new LinkedHashSet<>(sources)));
    }

    private boolean matchesPage(List<MappedPage> pages, FlowEndpoint endpoint) {
        if (endpoint == null) {
            return false;
        }
        return pages.stream().anyMatch(page -> PageReferenceMatcher.matches(page, endpoint.pageName())
                || PageReferenceMatcher.matches(page, endpoint.route()));
    }

    private boolean matchesTransition(FlowContractBuilderInput input, FlowEndpoint source, FlowEndpoint target) {
        if (input.mappedUiKnowledge() != null) {
            Set<String> sourcePageIds = input.mappedUiKnowledge().pages().stream()
                    .filter(page -> PageReferenceMatcher.matches(page, source.pageName())
                            || PageReferenceMatcher.matches(page, source.route()))
                    .map(MappedPage::pageId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            if (input.mappedUiKnowledge().transitions().stream()
                    .anyMatch(transition -> sourcePageIds.contains(transition.fromPageId())
                            && routeMatches(transition.toUrl(), target.route()))) {
                return true;
            }
        }
        return input.canonicalPageFlows() != null && input.canonicalPageFlows().flows().stream()
                .anyMatch(flow -> endpointMatches(flow.sourcePageName(), flow.sourceRoute(), source)
                        && endpointMatches(flow.targetPageName(), flow.targetRoute(), target));
    }

    private boolean hasFormEvidence(List<MappedPage> pages, FlowEndpoint source, FlowEndpoint target) {
        return pages.stream()
                .filter(page -> PageReferenceMatcher.matches(page, source.pageName())
                        || PageReferenceMatcher.matches(page, source.route())
                        || PageReferenceMatcher.matches(page, target.pageName())
                        || PageReferenceMatcher.matches(page, target.route()))
                .anyMatch(page -> !page.forms().isEmpty());
    }

    private boolean hasActionEvidence(List<MappedPage> pages, Iterable<FlowContractStep> steps) {
        String expected = "";
        for (FlowContractStep step : steps) {
            expected += " " + step.action().name().toLowerCase(Locale.ROOT);
        }
        String expectedActions = expected;
        return pages.stream()
                .filter(page -> ownsAnyStep(page, steps))
                .flatMap(page -> page.actions().stream())
                .map(this::actionText)
                .anyMatch(action -> overlaps(action, expectedActions));
    }

    private boolean ownsAnyStep(MappedPage page, Iterable<FlowContractStep> steps) {
        for (FlowContractStep step : steps) {
            if (PageReferenceMatcher.matches(page, step.ownerPage())
                    || PageReferenceMatcher.matches(page, step.route())) {
                return true;
            }
        }
        return false;
    }

    private String actionText(MappedAction action) {
        return normalize(action.actionName() + " " + action.actionType() + " " + action.description());
    }

    private boolean overlaps(String action, String expected) {
        return containsAny(action, "search") && expected.contains("search")
                || containsAny(action, "filter") && expected.contains("filter")
                || containsAny(action, "sort") && expected.contains("sort")
                || containsAny(action, "upload") && expected.contains("upload")
                || containsAny(action, "download") && expected.contains("download")
                || containsAny(action, "delete") && expected.contains("delete")
                || containsAny(action, "create") && expected.contains("create")
                || containsAny(action, "edit") && expected.contains("edit")
                || containsAny(action, "modal") && expected.contains("modal")
                || containsAny(action, "menu") && expected.contains("open_menu");
    }

    private List<FlowState> requiredStates(FlowContractType type, List<FlowContractStep> steps) {
        Set<FlowState> states = new LinkedHashSet<>();
        switch (type) {
            case AUTHENTICATION -> states.add(FlowState.UNAUTHENTICATED);
            case LOGOUT -> states.add(FlowState.AUTHENTICATED);
            case FORM_ENTRY, FORM_SUBMISSION, FORM_COMPLETION -> states.add(FlowState.FORM_READY);
            case SEARCH, FILTER, SORT, PAGINATION, COLLECTION_INSPECTION -> states.add(FlowState.RECORD_LIST_VISIBLE);
            case RECORD_OPEN, RECORD_CREATE, RECORD_EDIT, RECORD_DELETE -> states.add(FlowState.NAVIGATION_READY);
            default -> states.add(FlowState.NAVIGATION_READY);
        }
        if (steps.stream().anyMatch(step -> step.action() == FlowActionType.CONFIRM_ACTION)) {
            states.add(FlowState.MODAL_OPEN);
        }
        if (steps.stream().anyMatch(step -> step.action() == FlowActionType.OPEN_MENU)) {
            states.add(FlowState.USER_MENU_OPEN);
        }
        return List.copyOf(states);
    }

    private List<FlowState> producedStates(FlowContractType type, List<FlowContractStep> steps) {
        Set<FlowState> states = new LinkedHashSet<>();
        switch (type) {
            case AUTHENTICATION -> states.add(FlowState.AUTHENTICATED);
            case LOGOUT -> states.add(FlowState.UNAUTHENTICATED);
            case NAVIGATION -> states.add(FlowState.NAVIGATION_COMPLETE);
            case FORM_ENTRY -> states.add(FlowState.FORM_DIRTY);
            case FORM_SUBMISSION, FORM_COMPLETION -> states.add(FlowState.FORM_SUBMITTED);
            case SEARCH, FILTER -> states.add(FlowState.FILTER_APPLIED);
            case SORT -> states.add(FlowState.SORT_APPLIED);
            case PAGINATION -> states.add(FlowState.PAGE_CHANGED);
            case RECORD_OPEN -> states.add(FlowState.RECORD_DETAILS_VISIBLE);
            case RECORD_CREATE, RECORD_EDIT, RECORD_DELETE -> states.add(FlowState.RECORD_LIST_VISIBLE);
            case MODAL_OPEN -> states.add(FlowState.MODAL_OPEN);
            case FILE_UPLOAD -> states.add(FlowState.FILE_SELECTED);
            case FILE_DOWNLOAD -> states.add(FlowState.DOWNLOAD_REQUESTED);
            case CONTAINER_MUTATION -> states.add(FlowState.CONTAINER_UPDATED);
            default -> {
            }
        }
        if (steps.stream().anyMatch(step -> step.action() == FlowActionType.OPEN_MENU)) {
            states.add(FlowState.USER_MENU_OPEN);
        }
        return List.copyOf(states);
    }

    private List<FlowActionType> actions(CanonicalTestCase testCase) {
        LinkedHashSet<FlowActionType> values = new LinkedHashSet<>();
        testCase.operationIntents().stream()
                .filter(intent -> intent != null && intent.kind() != null)
                .map(intent -> FlowActionType.from(intent.kind()))
                .forEach(values::add);
        String text = normalize(testCase.title() + " " + String.join(" ", testCase.actions()));
        if (containsAny(text, "open user menu", "open menu", "dropdown menu")) values.add(FlowActionType.OPEN_MENU);
        if (containsAny(text, "select ", "dropdown", "option")) values.add(FlowActionType.SELECT_OPTION);
        if (containsAny(text, "checkbox", "toggle", "uncheck")) values.add(FlowActionType.TOGGLE_CONTROL);
        return List.copyOf(values);
    }

    private FlowContractType type(List<FlowActionType> actions) {
        if (actions.contains(FlowActionType.LOGOUT)) return FlowContractType.LOGOUT;
        if (actions.contains(FlowActionType.AUTHENTICATE)) return FlowContractType.AUTHENTICATION;
        if (actions.contains(FlowActionType.DELETE_RECORD)) return FlowContractType.RECORD_DELETE;
        if (actions.contains(FlowActionType.EDIT_RECORD)) return FlowContractType.RECORD_EDIT;
        if (actions.contains(FlowActionType.CREATE_RECORD)) return FlowContractType.RECORD_CREATE;
        if (actions.contains(FlowActionType.OPEN_RECORD)) return FlowContractType.RECORD_OPEN;
        if (actions.contains(FlowActionType.UPLOAD_FILE)) return FlowContractType.FILE_UPLOAD;
        if (actions.contains(FlowActionType.DOWNLOAD_FILE)) return FlowContractType.FILE_DOWNLOAD;
        if (actions.contains(FlowActionType.CONFIRM_ACTION)) return FlowContractType.ACTION_CONFIRMATION;
        if (actions.contains(FlowActionType.OPEN_MODAL)) return FlowContractType.MODAL_OPEN;
        if (actions.contains(FlowActionType.SEARCH)) return FlowContractType.SEARCH;
        if (actions.contains(FlowActionType.FILTER)) return FlowContractType.FILTER;
        if (actions.contains(FlowActionType.SORT)) return FlowContractType.SORT;
        if (actions.contains(FlowActionType.PAGINATE)) return FlowContractType.PAGINATION;
        if (actions.contains(FlowActionType.MUTATE_CONTAINER)) return FlowContractType.CONTAINER_MUTATION;
        if (actions.contains(FlowActionType.ENTER_TEXT) && actions.contains(FlowActionType.SUBMIT_FORM)) return FlowContractType.FORM_COMPLETION;
        if (actions.contains(FlowActionType.SUBMIT_FORM)) return FlowContractType.FORM_SUBMISSION;
        if (actions.contains(FlowActionType.ENTER_TEXT) || actions.contains(FlowActionType.SELECT_OPTION)
                || actions.contains(FlowActionType.TOGGLE_CONTROL)) return FlowContractType.FORM_ENTRY;
        if (actions.contains(FlowActionType.INSPECT_COLLECTION)) return FlowContractType.COLLECTION_INSPECTION;
        if (actions.contains(FlowActionType.OPEN_PAGE) || actions.contains(FlowActionType.NAVIGATE)) return FlowContractType.NAVIGATION;
        return FlowContractType.GENERIC;
    }

    private boolean isTransitionFlow(FlowContractType type) {
        return type == FlowContractType.AUTHENTICATION || type == FlowContractType.LOGOUT
                || type == FlowContractType.NAVIGATION || type == FlowContractType.RECORD_OPEN;
    }

    private boolean isFormFlow(FlowContractType type) {
        return type == FlowContractType.FORM_ENTRY || type == FlowContractType.FORM_SUBMISSION
                || type == FlowContractType.FORM_COMPLETION || type == FlowContractType.FILE_UPLOAD;
    }

    private List<FlowContractStep> renumber(Iterable<FlowContractStep> values) {
        List<FlowContractStep> steps = new ArrayList<>();
        int order = 1;
        for (FlowContractStep step : values) {
            steps.add(new FlowContractStep(order++, step.action(), step.ownerPage(), step.route(), step.dataKey(), step.setup()));
        }
        return List.copyOf(steps);
    }

    private String ownerPage(UiOperationKind kind, CanonicalTestCase testCase) {
        return switch (kind) {
            case AUTHENTICATE, ENTER_TEXT, SUBMIT_FORM -> firstNonBlank(testCase.sourcePageName(), testCase.pageName());
            default -> firstNonBlank(testCase.pageName(), testCase.sourcePageName());
        };
    }

    private String routeFor(String page, CanonicalTestCase testCase) {
        if (PageReferenceMatcher.matchesScenarioPage(testCase.sourcePageName(), testCase.sourceRoute(), page)) {
            return testCase.sourceRoute();
        }
        return firstNonBlank(testCase.route(), testCase.sourceRoute());
    }

    private boolean endpointMatches(String pageName, String route, FlowEndpoint endpoint) {
        return PageReferenceMatcher.matchesScenarioPage(pageName, route, endpoint.pageName())
                || routeMatches(route, endpoint.route());
    }

    private boolean routeMatches(String left, String right) {
        return !left.isBlank() && !right.isBlank() && RouteCanonicalizer.routeEqualsOrSuffix(left, right);
    }

    private String flowId(FlowContractType type, FlowEndpoint source, FlowEndpoint target) {
        return (type.name() + "_" + endpointKey(source) + "_TO_" + endpointKey(target))
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String displayName(FlowContractType type, FlowEndpoint source, FlowEndpoint target) {
        return type.name() + ": " + endpointKey(source) + " -> " + endpointKey(target);
    }

    private String endpointKey(FlowEndpoint endpoint) {
        if (endpoint == null) return "unknown";
        return firstNonBlank(endpoint.pageName(), endpoint.route(), "unknown");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private record Evidence(double confidence, boolean confirmed, List<String> sources) {
    }

    private static final class Draft {
        private final FlowContractType type;
        private final FlowEndpoint source;
        private final FlowEndpoint target;
        private final Set<String> requirementIds = new LinkedHashSet<>();
        private final Set<String> assertions = new LinkedHashSet<>();
        private final Set<String> evidence = new LinkedHashSet<>();
        private final Map<String, FlowContractStep> steps = new LinkedHashMap<>();

        private Draft(FlowContractType type, FlowEndpoint source, FlowEndpoint target) {
            this.type = type;
            this.source = source;
            this.target = target;
        }

        private void addStep(FlowContractStep step) {
            String key = step.action() + "|" + step.ownerPage() + "|" + step.route() + "|" + step.dataKey();
            steps.putIfAbsent(key, step);
        }
    }
}
