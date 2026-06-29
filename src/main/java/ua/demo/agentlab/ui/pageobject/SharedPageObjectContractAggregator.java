package ua.demo.agentlab.ui.pageobject;

import ua.demo.agentlab.ui.LocatorHint;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SharedPageObjectContractAggregator {

    public List<SharedPageObjectSpec> aggregate(UiTestPlan uiTestPlan) {
        if (uiTestPlan == null || uiTestPlan.scenarios() == null || uiTestPlan.scenarios().isEmpty()) {
            return List.of();
        }

        Map<String, Accumulator> accumulators = new LinkedHashMap<>();

        for (UiTestScenario scenario : uiTestPlan.scenarios()) {
            registerScenario(accumulators, scenario.pageName(), scenario.route(), scenario);
            if (scenario.sourcePageName() != null
                    && !scenario.sourcePageName().isBlank()
                    && !samePageReference(scenario.sourcePageName(), scenario.sourceRoute(), scenario.pageName(), scenario.route())) {
                registerScenario(accumulators, scenario.sourcePageName(), scenario.sourceRoute(), scenario);
            }
        }

        return accumulators.values().stream()
                .map(Accumulator::toSpec)
                .toList();
    }

    private void registerScenario(
            Map<String, Accumulator> accumulators,
            String pageName,
            String route,
            UiTestScenario scenario
    ) {
        if (pageName == null || pageName.isBlank() || scenario == null) {
            return;
        }

        accumulators.computeIfAbsent(accumulatorKey(pageName, route), key -> new Accumulator(pageName))
                .add(route, scenario);
    }

    private String accumulatorKey(String pageName, String route) {
        if (route != null && !route.isBlank()) {
            return route.trim().toLowerCase();
        }
        return pageName.trim();
    }

    private boolean samePageReference(String leftPageName, String leftRoute, String rightPageName, String rightRoute) {
        return ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher.matchesScenarioPage(leftPageName, leftRoute, rightPageName)
                || ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher.matchesScenarioPage(rightPageName, rightRoute, leftPageName)
                || (leftRoute != null && !leftRoute.isBlank() && leftRoute.equalsIgnoreCase(rightRoute));
    }

    private static final class Accumulator {
        private final String pageName;
        private String route;
        private final Set<String> scenarioIds = new LinkedHashSet<>();
        private final List<UiTestScenario> scenarios = new ArrayList<>();
        private final Map<String, LocatorHint> locatorHintsByKey = new LinkedHashMap<>();

        private Accumulator(String pageName) {
            this.pageName = pageName;
        }

        private void add(String candidateRoute, UiTestScenario scenario) {
            if (route == null || route.isBlank()) {
                route = candidateRoute;
            }
            if (scenarioIds.add(scenario.id())) {
                scenarios.add(scenario);
            }
            for (LocatorHint locatorHint : scenario.locatorHints()) {
                String key = locatorHint.elementName() + "|" + locatorHint.recommendedStrategy()
                        + "|" + locatorHint.recommendedValue();
                locatorHintsByKey.putIfAbsent(key, locatorHint);
            }
        }

        private SharedPageObjectSpec toSpec() {
            return new SharedPageObjectSpec(
                    pageName,
                    route,
                    new ArrayList<>(scenarioIds),
                    scenarios,
                    new ArrayList<>(locatorHintsByKey.values())
            );
        }
    }
}
