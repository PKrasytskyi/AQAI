package ua.demo.agentlab.ui.generator;

import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CanonicalTestCaseUiPlanGenerator implements UiTestPlanGenerator {

    @Override
    public UiTestPlan generate(WorkflowState state) {
        if (state == null || state.getCanonicalTestCaseBundle() == null) {
            return new UiTestPlan("unknown-source", "NoPages", List.of(), List.of());
        }

        CanonicalTestCaseBundle bundle = state.getCanonicalTestCaseBundle();
        List<UiTestScenario> scenarios = bundle.testCases().stream()
                .map(this::toUiScenario)
                .toList();

        return new UiTestPlan(
                bundle.source(),
                bundle.primaryPage(),
                resolvePageNames(bundle),
                scenarios
        );
    }

    private UiTestScenario toUiScenario(CanonicalTestCase testCase) {
        return new UiTestScenario(
                testCase.id(),
                testCase.title(),
                testCase.canonicalFlowId(),
                testCase.canonicalFlowType(),
                testCase.sourcePageName(),
                testCase.pageName(),
                testCase.sourceRoute(),
                testCase.route(),
                testCase.precondition(),
                testCase.prerequisiteFlow(),
                testCase.assertionProfile(),
                testCase.actions(),
                testCase.assertions(),
                testCase.operationIntents(),
                testCase.assertionIntents(),
                testCase.locatorHints(),
                testCase.sourceReference()
        );
    }

    private List<String> resolvePageNames(CanonicalTestCaseBundle bundle) {
        Set<String> pageNames = new LinkedHashSet<>();
        addPageName(pageNames, bundle.primaryPage());
        for (CanonicalTestCase testCase : bundle.testCases()) {
            addPageName(pageNames, testCase.sourcePageName());
            addPageName(pageNames, testCase.pageName());
            if (testCase.prerequisiteFlow() != null) {
                addPageName(pageNames, testCase.prerequisiteFlow().sourcePageName());
            }
        }
        return pageNames.isEmpty() ? List.of(bundle.primaryPage()) : new ArrayList<>(pageNames);
    }

    private void addPageName(Set<String> pageNames, String pageName) {
        if (isPageReference(pageName)) {
            pageNames.add(pageName.trim());
        }
    }

    private boolean isPageReference(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        return !trimmed.startsWith("/")
                && !normalized.startsWith("http://")
                && !normalized.startsWith("https://")
                && !normalized.equals("entitykey")
                && !normalized.equals("datakey")
                && !normalized.equals("null");
    }
}
