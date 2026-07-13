package ua.demo.agentlab.artifactreuse.planner;

import ua.demo.agentlab.artifactreuse.flow.FlowActionType;
import ua.demo.agentlab.artifactreuse.flow.FlowContract;
import ua.demo.agentlab.artifactreuse.flow.FlowContractType;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.contract.UiOperationKind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Produces explainable reuse decisions. It never mutates canonical tests or prompt evidence.
 */
public class ReusePlanner {

    public ReusePlannerResult plan(ReusePlannerInput input) {
        if (input == null || input.canonicalTestCases() == null) {
            return new ReusePlannerResult("reuse-planner.v1", false, List.of(), 0, 0, 0, "");
        }
        List<RequirementReusePlan> plans = new ArrayList<>();
        Counters counters = new Counters();
        for (CanonicalTestCase testCase : input.canonicalTestCases().testCases()) {
            if (testCase != null) {
                plans.add(plan(testCase, input, counters));
            }
        }
        return new ReusePlannerResult("reuse-planner.v1", input.enabled(), plans, counters.stable, counters.discover,
                counters.review, input.semanticCandidates() == null ? "" : input.semanticCandidates().vectorUnavailableReason());
    }

    private RequirementReusePlan plan(CanonicalTestCase testCase, ReusePlannerInput input, Counters counters) {
        String requirementId = firstNonBlank(testCase.id(), testCase.requirementRefs().isEmpty() ? "" : testCase.requirementRefs().get(0));
        List<ReusePrecondition> preconditions = new ArrayList<>();
        List<ReuseMissingKnowledge> missing = new ArrayList<>();
        List<String> candidates = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        if (!input.enabled()) {
            preconditions.add(new ReusePrecondition(ReuseKnowledgeType.FLOW, "", ReuseDecisionType.DISABLED, 0.0d,
                    "artifact reuse is disabled"));
            counters.review++;
            return new RequirementReusePlan(requirementId, testCase.pageName(), testCase.route(), preconditions, missing,
                    candidates, notes);
        }

        if (testCase.pageName() == null || testCase.pageName().isBlank() || testCase.route() == null || testCase.route().isBlank()) {
            missing.add(new ReuseMissingKnowledge(ReuseKnowledgeType.PAGE, firstNonBlank(testCase.pageName(), testCase.route()),
                    ReuseDecisionType.DISCOVER, "target page or confirmed route is missing"));
            counters.discover++;
        }

        FlowContractType requested = requestedType(testCase);
        List<FlowSemanticCandidate> relevantCandidates = relevantSemanticCandidates(requested, testCase, input.semanticCandidates());
        List<FlowSemanticCandidate> matches = relevantCandidates.stream().filter(FlowSemanticCandidate::reusable).toList();
        matches.forEach(candidate -> candidates.add(candidate.flowId()));
        if (!matches.isEmpty()) {
            FlowSemanticCandidate best = matches.get(0);
            preconditions.add(new ReusePrecondition(ReuseKnowledgeType.FLOW, best.flowId(), ReuseDecisionType.REUSE_STABLE,
                    best.semanticScore(), "Qdrant candidate was confirmed by Neo4j exact flow lookup"));
            counters.stable++;
        } else if (hasDegradedConfirmedCandidate(relevantCandidates)) {
            FlowSemanticCandidate degraded = firstDegradedCandidate(relevantCandidates);
            preconditions.add(new ReusePrecondition(ReuseKnowledgeType.FLOW, degraded.flowId(), ReuseDecisionType.NEEDS_REVIEW,
                    degraded.semanticScore(), "confirmed flow quality is degraded: " + String.join(", ", degraded.contract().reuseQuality().reasons())));
            counters.review++;
        } else if (hasUnverifiedSemanticCandidate(relevantCandidates)) {
            preconditions.add(new ReusePrecondition(ReuseKnowledgeType.FLOW, "", ReuseDecisionType.NEEDS_REVIEW,
                    0.0d, unverifiedCandidateReason(input.semanticCandidates())));
            counters.review++;
        } else if (hasConfirmedCurrentFlow(requested, testCase, input)) {
            notes.add("Current-run confirmed flow exists, but it is not reused until a stable registry candidate is available");
        } else if (requested != FlowContractType.GENERIC) {
            missing.add(new ReuseMissingKnowledge(ReuseKnowledgeType.FLOW, requested.name(), ReuseDecisionType.DISCOVER,
                    "no stable confirmed flow matches the required operations and endpoints"));
            counters.discover++;
        }

        if (requiresMenu(testCase) && matches.stream().noneMatch(candidate -> containsAction(candidate.contract(), FlowActionType.OPEN_MENU))) {
            missing.add(new ReuseMissingKnowledge(ReuseKnowledgeType.COMPONENT, "user-menu", ReuseDecisionType.DISCOVER,
                    "user menu action requires a confirmed component-owned action sequence"));
            counters.discover++;
        }
        if (input.explainDecisions() && input.semanticCandidates() != null
                && !input.semanticCandidates().vectorUnavailableReason().isBlank()) {
            notes.add("semantic reuse unavailable: " + input.semanticCandidates().vectorUnavailableReason());
        }
        return new RequirementReusePlan(requirementId, testCase.pageName(), testCase.route(), preconditions, missing,
                List.copyOf(new LinkedHashSet<>(candidates)), notes);
    }

    private boolean hasUnverifiedSemanticCandidate(List<FlowSemanticCandidate> candidates) {
        return candidates.stream().anyMatch(candidate -> !candidate.graphConfirmed());
    }

    private String unverifiedCandidateReason(FlowSemanticCandidateBundle bundle) {
        if (bundle != null && bundle.notes() != null && !bundle.notes().isEmpty()) {
            String reason = bundle.notes().get(0);
            if (reason != null && !reason.isBlank()) return reason.trim();
        }
        return "semantic flow candidate was not confirmed by Neo4j exact lookup";
    }

    private boolean hasDegradedConfirmedCandidate(List<FlowSemanticCandidate> candidates) {
        return candidates.stream().anyMatch(FlowSemanticCandidate::confirmedButDegraded);
    }

    private FlowSemanticCandidate firstDegradedCandidate(List<FlowSemanticCandidate> candidates) {
        return candidates.stream().filter(FlowSemanticCandidate::confirmedButDegraded)
                .max(Comparator.comparingDouble(FlowSemanticCandidate::semanticScore)).orElseThrow();
    }

    private List<FlowSemanticCandidate> relevantSemanticCandidates(
            FlowContractType requested,
            CanonicalTestCase testCase,
            FlowSemanticCandidateBundle bundle
    ) {
        if (bundle == null) {
            return List.of();
        }
        return bundle.candidates().stream()
                .filter(candidate -> candidate.flowType() == requested)
                .filter(candidate -> endpointsMatch(candidate, testCase))
                .sorted(Comparator.comparingDouble(FlowSemanticCandidate::semanticScore).reversed())
                .toList();
    }

    private boolean endpointsMatch(FlowSemanticCandidate candidate, CanonicalTestCase testCase) {
        if (candidate.contract() != null) return endpointsMatch(candidate.contract(), testCase);
        return equalsAny(candidate.sourceRoute(), testCase.sourceRoute(), testCase.route())
                || equalsAny(candidate.targetRoute(), testCase.route(), testCase.sourceRoute());
    }

    private boolean hasConfirmedCurrentFlow(FlowContractType requested, CanonicalTestCase testCase, ReusePlannerInput input) {
        return input.currentRunFlows() != null && input.currentRunFlows().contracts().stream()
                .filter(FlowContract::confirmed)
                .anyMatch(contract -> contract.type() == requested && endpointsMatch(contract, testCase));
    }

    private boolean endpointsMatch(FlowContract contract, CanonicalTestCase testCase) {
        return equalsAny(contract.source().route(), testCase.sourceRoute(), testCase.route())
                || equalsAny(contract.target().route(), testCase.route(), testCase.sourceRoute())
                || equalsAny(contract.source().pageName(), testCase.sourcePageName(), testCase.pageName())
                || equalsAny(contract.target().pageName(), testCase.pageName(), testCase.sourcePageName());
    }

    private FlowContractType requestedType(CanonicalTestCase testCase) {
        Set<UiOperationKind> operations = new LinkedHashSet<>();
        testCase.operationIntents().forEach(intent -> {
            if (intent != null && intent.kind() != null) operations.add(intent.kind());
        });
        if (operations.contains(UiOperationKind.LOGOUT)) return FlowContractType.LOGOUT;
        if (operations.contains(UiOperationKind.AUTHENTICATE)) return FlowContractType.AUTHENTICATION;
        if (operations.contains(UiOperationKind.ENTER_TEXT) && operations.contains(UiOperationKind.SUBMIT_FORM)) return FlowContractType.FORM_COMPLETION;
        if (operations.contains(UiOperationKind.ENTER_TEXT)) return FlowContractType.FORM_ENTRY;
        if (operations.contains(UiOperationKind.SUBMIT_FORM)) return FlowContractType.FORM_SUBMISSION;
        if (operations.contains(UiOperationKind.SEARCH)) return FlowContractType.SEARCH;
        if (operations.contains(UiOperationKind.FILTER)) return FlowContractType.FILTER;
        if (operations.contains(UiOperationKind.SORT)) return FlowContractType.SORT;
        if (operations.contains(UiOperationKind.PAGINATE)) return FlowContractType.PAGINATION;
        if (operations.contains(UiOperationKind.OPEN_RECORD)) return FlowContractType.RECORD_OPEN;
        if (operations.contains(UiOperationKind.CREATE_RECORD)) return FlowContractType.RECORD_CREATE;
        if (operations.contains(UiOperationKind.EDIT_RECORD)) return FlowContractType.RECORD_EDIT;
        if (operations.contains(UiOperationKind.DELETE_RECORD)) return FlowContractType.RECORD_DELETE;
        return FlowContractType.GENERIC;
    }

    private boolean requiresMenu(CanonicalTestCase testCase) {
        String value = (testCase.title() + " " + String.join(" ", testCase.actions())).toLowerCase();
        return value.contains("menu") || value.contains("dropdown");
    }

    private boolean containsAction(FlowContract contract, FlowActionType action) {
        return contract != null && contract.steps().stream().anyMatch(step -> step.action() == action);
    }

    private boolean equalsAny(String value, String... candidates) {
        if (value == null || value.isBlank()) return false;
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && value.trim().equalsIgnoreCase(candidate.trim())) return true;
        }
        return false;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first.trim() : (second == null ? "" : second.trim());
    }

    private static final class Counters {
        private int stable;
        private int discover;
        private int review;
    }
}
