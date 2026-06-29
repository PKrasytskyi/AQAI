package ua.demo.agentlab.ui.pageobject;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.List;

public record SharedPageObjectSpec(
        String pageName,
        String route,
        List<String> scenarioIds,
        List<UiTestScenario> scenarios,
        List<LocatorHint> locatorHints
) {
    public SharedPageObjectSpec {
        pageName = pageName == null ? "" : pageName.trim();
        route = route == null ? "" : route.trim();
        scenarioIds = scenarioIds == null ? List.of() : List.copyOf(scenarioIds);
        scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        locatorHints = locatorHints == null ? List.of() : List.copyOf(locatorHints);
    }

    public List<UiTestScenario> navigationScenarios() {
        return scenarios.stream()
                .filter(this::isNavigationScenario)
                .toList();
    }

    public List<UiTestScenario> assertionScenarios() {
        return scenarios.stream()
                .filter(this::isAssertionScenario)
                .toList();
    }

    private boolean isNavigationScenario(UiTestScenario scenario) {
        if (scenario == null) {
            return false;
        }
        String navigationPage = scenario.sourcePageName() == null || scenario.sourcePageName().isBlank()
                ? scenario.pageName()
                : scenario.sourcePageName();
        return PageReferenceMatcher.matchesScenarioPage(navigationPage, scenario.sourceRoute(), pageName)
                || PageReferenceMatcher.matchesScenarioPage(pageName, route, navigationPage);
    }

    private boolean isAssertionScenario(UiTestScenario scenario) {
        return scenario != null
                && (PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName)
                || PageReferenceMatcher.matchesScenarioPage(pageName, route, scenario.pageName()));
    }
}
