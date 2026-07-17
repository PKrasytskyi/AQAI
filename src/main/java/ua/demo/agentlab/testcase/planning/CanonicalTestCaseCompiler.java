package ua.demo.agentlab.testcase.planning;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.UiScenarioPrerequisite;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.UiOperationIntent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

class CanonicalTestCaseCompiler {

    CanonicalTestCase compile(ScenarioCandidate scenario, ScenarioQualityReport qualityReport) {
        NormalizedRequirement requirement = scenario.primaryRequirement().requirement();
        List<UiOperationIntent> operationIntents = scenario.steps().stream()
                .map(step -> new UiOperationIntent(step.kind(), step.ownerPage(), step.dataKey()))
                .toList();
        List<AssertionIntent> assertionIntents = scenario.assertions().stream()
                .map(assertion -> new AssertionIntent(assertion.intentKind(), assertion.target(), assertion.expectedValue()))
                .toList();
        List<String> targetPages = targetPages(scenario);
        List<String> actions = scenario.steps().stream().map(this::actionText).distinct().toList();
        List<String> assertions = scenario.assertions().stream()
                .map(ScenarioAssertionCandidate::expectedValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        List<String> llmSteps = llmSteps(requirement, actions, assertions, qualityReport);
        return new CanonicalTestCase(
                requirement.id(),
                requirement.title(),
                requirementIds(scenario),
                llmSteps,
                operationIntents,
                assertionIntents,
                targetPages,
                new UiScenarioPrerequisite(
                        scenario.sourcePageName(),
                        scenario.sourceRoute(),
                        scenario.steps().stream().anyMatch(ScenarioStepCandidate::setup),
                        scenario.steps().stream().filter(ScenarioStepCandidate::setup).map(this::actionText).toList()
                ),
                "flow-" + requirement.id(),
                scenario.primaryRequirement().intent().name(),
                scenario.sourcePageName(),
                scenario.targetPageName(),
                scenario.sourceRoute(),
                scenario.targetRoute(),
                "Application is available",
                UiAssertionProfile.BASIC,
                actions,
                assertions,
                List.<LocatorHint>of(),
                sourceReference(requirement.sourceReference())
        );
    }

    private List<String> requirementIds(ScenarioCandidate scenario) {
        Set<String> ids = new LinkedHashSet<>();
        ids.add(scenario.primaryRequirement().requirement().id());
        ids.addAll(scenario.supportingRequirementIds());
        return List.copyOf(ids);
    }

    private List<String> targetPages(ScenarioCandidate scenario) {
        Set<String> pages = new LinkedHashSet<>();
        if (!scenario.sourcePageName().isBlank()) {
            pages.add(scenario.sourcePageName());
        }
        if (!scenario.targetPageName().isBlank()) {
            pages.add(scenario.targetPageName());
        }
        scenario.assertions().stream().map(ScenarioAssertionCandidate::ownerPage)
                .filter(value -> value != null && !value.isBlank())
                .forEach(pages::add);
        return List.copyOf(pages);
    }

    private List<String> llmSteps(
            NormalizedRequirement requirement,
            List<String> actions,
            List<String> assertions,
            ScenarioQualityReport qualityReport
    ) {
        java.util.ArrayList<String> steps = new java.util.ArrayList<>();
        steps.add("Requirement: " + requirement.title());
        actions.forEach(action -> steps.add("Action: " + action));
        assertions.forEach(assertion -> steps.add("Assert: " + assertion));
        if (qualityReport != null && !qualityReport.valid()) {
            qualityReport.issues().forEach(issue -> steps.add("Needs review: " + issue));
        }
        return List.copyOf(steps);
    }

    private String actionText(ScenarioStepCandidate step) {
        return switch (step.kind()) {
            case OPEN_PAGE -> "Open the target page (" + step.route() + ")";
            case AUTHENTICATE -> "Authenticate using the configured credentials";
            case ENTER_TEXT -> "Enter " + (step.dataKey() == null || step.dataKey().isBlank() ? "text" : step.dataKey() + " text");
            case SUBMIT_FORM -> "Submit the target form with valid data";
            case INSPECT_PAGE_CONTENT -> "Inspect the rendered content area";
            case LOGOUT -> "Sign out from the current session";
            default -> titleCase(step.kind().name());
        };
    }

    private String titleCase(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
    }

    private String sourceReference(SourceReference reference) {
        if (reference == null) {
            return "";
        }
        return reference.startLine() > 0 ? reference.source() + " [L" + reference.startLine() + "]" : reference.source();
    }
}
