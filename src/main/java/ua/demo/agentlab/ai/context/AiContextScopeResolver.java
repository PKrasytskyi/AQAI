package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.UiOperationKind;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.flow.model.CanonicalFlow;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AiContextScopeResolver {

    public AiContextScope resolveForUiPlan(AiContextPackage context, UiTestPlan baselinePlan) {
        List<UiTestScenario> baselineScenarios = baselinePlan == null ? List.of() : baselinePlan.scenarios();
        Set<String> pageNames = new LinkedHashSet<>();
        Set<String> routes = new LinkedHashSet<>();
        Set<String> scenarioIds = new LinkedHashSet<>();

        for (UiTestScenario scenario : baselineScenarios) {
            addScenarioScope(scenario, pageNames, routes, scenarioIds);
        }
        if (pageNames.isEmpty() && baselinePlan != null) {
            pageNames.addAll(baselinePlan.pageNames());
        }

        Set<String> requirementIds = resolveRequirementIds(context, routes, pageNames, baselineScenarios);
        return new AiContextScope(
                "ui-test-plan",
                "ui-plan",
                new ArrayList<>(pageNames),
                new ArrayList<>(routes),
                new ArrayList<>(scenarioIds),
                new ArrayList<>(requirementIds)
        );
    }

    public AiContextScope resolveForPage(AiContextPackage context, String pageName) {
        Set<String> pageNames = new LinkedHashSet<>();
        Set<String> routes = new LinkedHashSet<>();
        Set<String> scenarioIds = new LinkedHashSet<>();
        pageNames.add(normalize(pageName));

        List<UiTestScenario> matchingScenarios = new ArrayList<>();
        if (context.uiTestPlan() != null) {
            for (UiTestScenario scenario : context.uiTestPlan().scenarios()) {
                if (pageMatchesScenario(pageName, scenario)) {
                    matchingScenarios.add(scenario);
                    addIfPresent(scenarioIds, scenario.id());
                    addScenarioRoutesForPage(pageName, scenario, routes);
                }
            }
        }

        Set<String> requirementIds = resolveRequirementIds(context, routes, pageNames, matchingScenarios);
        return new AiContextScope(
                "page-object-spec",
                normalize(pageName),
                new ArrayList<>(pageNames),
                new ArrayList<>(routes),
                new ArrayList<>(scenarioIds),
                new ArrayList<>(requirementIds)
        );
    }

    public AiContextScope resolveForScenario(AiContextPackage context, UiTestScenario scenario) {
        Set<String> pageNames = new LinkedHashSet<>();
        Set<String> routes = new LinkedHashSet<>();
        Set<String> scenarioIds = new LinkedHashSet<>();
        addScenarioScope(scenario, pageNames, routes, scenarioIds);

        Set<String> requirementIds = resolveRequirementIds(context, routes, pageNames, List.of(scenario));
        return new AiContextScope(
                "ui-test-spec",
                normalize(scenario.id()),
                new ArrayList<>(pageNames),
                new ArrayList<>(routes),
                new ArrayList<>(scenarioIds),
                new ArrayList<>(requirementIds)
        );
    }

    private Set<String> resolveRequirementIds(
            AiContextPackage context,
            Set<String> routes,
            Set<String> pageNames,
            List<UiTestScenario> scenarios
    ) {
        Set<String> requirementIds = new LinkedHashSet<>();
        Set<String> sourceReferences = new LinkedHashSet<>();
        for (UiTestScenario scenario : scenarios) {
            if (scenario.sourceReference() != null && !scenario.sourceReference().isBlank()) {
                sourceReferences.addAll(List.of(scenario.sourceReference().split("\\s*\\|\\s*")));
            }
        }

        if (context.normalizedRequirementBundle() != null) {
            for (NormalizedRequirement requirement : context.normalizedRequirementBundle().requirements()) {
                String requirementSource = toSourceReference(requirement);
                if (sourceReferences.stream().anyMatch(requirementSource::contains)) {
                    requirementIds.add(requirement.id());
                }
            }
        }

        if (!requirementIds.isEmpty()) {
            return requirementIds;
        }

        if (context.canonicalPageFlowModel() != null) {
            for (CanonicalFlow flow : context.canonicalPageFlowModel().flows()) {
                if (matchesFlow(flow, routes, pageNames)) {
                    requirementIds.addAll(flow.sourceRequirementIds());
                }
            }
        }

        if (requirementIds.isEmpty() && context.normalizedRequirementBundle() != null) {
            for (NormalizedRequirement requirement : context.normalizedRequirementBundle().requirements()) {
                if (requirement.uiRelevant()) {
                    requirementIds.add(requirement.id());
                }
            }
        }
        return requirementIds;
    }

    private boolean matchesFlow(CanonicalFlow flow, Set<String> routes, Set<String> pageNames) {
        return PageReferenceMatcher.matchesScenarioPage(flow.sourcePageName(), flow.sourceRoute(), firstMatch(pageNames, flow.sourcePageName(), flow.sourceRoute()))
                || PageReferenceMatcher.matchesScenarioPage(flow.targetPageName(), flow.targetRoute(), firstMatch(pageNames, flow.targetPageName(), flow.targetRoute()))
                || routes.contains(normalize(flow.sourceRoute()))
                || routes.contains(normalize(flow.targetRoute()));
    }

    private String toSourceReference(NormalizedRequirement requirement) {
        if (requirement == null || requirement.sourceReference() == null) {
            return "";
        }
        if (requirement.sourceReference().startLine() > 0) {
            return "%s [L%d]".formatted(
                    normalize(requirement.sourceReference().source()),
                    requirement.sourceReference().startLine()
            );
        }
        return normalize(requirement.sourceReference().source());
    }

    private void addScenarioScope(
            UiTestScenario scenario,
            Set<String> pageNames,
            Set<String> routes,
            Set<String> scenarioIds
    ) {
        if (scenario == null) {
            return;
        }
        addIfPresent(pageNames, scenario.pageName());
        addIfPresent(pageNames, scenario.sourcePageName());
        addIfPresent(routes, scenario.route());
        addIfPresent(routes, scenario.sourceRoute());
        if (scenario.prerequisite() != null) {
            addIfPresent(pageNames, scenario.prerequisite().sourcePageName());
            addIfPresent(routes, scenario.prerequisite().sourceRoute());
        }
        addIfPresent(scenarioIds, scenario.id());
    }

    private void addScenarioRoutesForPage(String pageName, UiTestScenario scenario, Set<String> routes) {
        if (scenario == null) {
            return;
        }
        boolean targetPage = PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName);
        boolean sourcePage = PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), pageName);
        boolean prerequisitePage = scenario.prerequisite() != null
                && PageReferenceMatcher.matchesScenarioPage(
                scenario.prerequisite().sourcePageName(),
                scenario.prerequisite().sourceRoute(),
                pageName
        );
        boolean crossPageAuthentication = isCrossPageAuthenticationScenario(scenario);

        if (targetPage && (!crossPageAuthentication || targetOwnsAuthenticationScenario(pageName, scenario))) {
            addIfPresent(routes, scenario.route());
        }
        if (sourcePage && (!crossPageAuthentication || sourceOwnsAuthenticationScenario(pageName, scenario))) {
            addIfPresent(routes, scenario.sourceRoute());
        }
        if (prerequisitePage) {
            addIfPresent(routes, scenario.prerequisite().sourceRoute());
        }
    }

    private boolean pageMatchesScenario(String pageName, UiTestScenario scenario) {
        if (scenario == null || pageName == null || pageName.isBlank()) {
            return false;
        }

        boolean targetPage = PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName);
        boolean sourcePage = PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), pageName);
        boolean prerequisitePage = scenario.prerequisite() != null
                && PageReferenceMatcher.matchesScenarioPage(
                scenario.prerequisite().sourcePageName(),
                scenario.prerequisite().sourceRoute(),
                pageName
        );

        if (sourcePage && targetPage) {
            return true;
        }
        if (isCrossPageAuthenticationScenario(scenario)) {
            return sourcePage && sourceOwnsAuthenticationScenario(pageName, scenario)
                    || targetPage && targetOwnsAuthenticationScenario(pageName, scenario);
        }
        return targetPage || sourcePage || prerequisitePage;
    }

    private boolean isCrossPageAuthenticationScenario(UiTestScenario scenario) {
        boolean crossPage = !PageReferenceMatcher.matchesScenarioPage(
                scenario.sourcePageName(),
                scenario.sourceRoute(),
                scenario.pageName()
        );
        if (!crossPage) {
            return false;
        }
        String text = ownershipText(scenario);
        boolean authOperation = scenario.operationIntents().stream()
                .anyMatch(intent -> intent.kind() == UiOperationKind.AUTHENTICATE);
        return authOperation
                || text.contains("login")
                || text.contains("credential")
                || text.contains("authenticated")
                || text.contains("secure")
                || text.contains("welcome");
    }

    private boolean sourceOwnsAuthenticationScenario(String pageName, UiTestScenario scenario) {
        if (!isLoginPage(pageName)) {
            return true;
        }
        String text = ownershipText(scenario);
        if (text.contains("authenticated area")
                || text.contains("secure area")
                || text.contains("welcome message")
                || text.contains("authenticated welcome")) {
            return text.contains("valid credential")
                    || text.contains("can login")
                    || text.contains("logged with valid");
        }
        return text.contains("login page")
                || text.contains("login route")
                || text.contains("login button")
                || text.contains("username")
                || text.contains("password")
                || text.contains("credential")
                || text.contains("can login")
                || text.contains("logged with valid");
    }

    private boolean targetOwnsAuthenticationScenario(String pageName, UiTestScenario scenario) {
        if (isLoginPage(pageName)) {
            return sourceOwnsAuthenticationScenario(pageName, scenario);
        }
        String text = ownershipText(scenario);
        if (text.contains("login page")
                || text.contains("login route")
                || text.contains("login button")
                || text.contains("username")
                || text.contains("password")
                || text.equals("user can login with valid credentials")) {
            return false;
        }
        return text.contains("authenticated area")
                || text.contains("secure area")
                || text.contains("welcome")
                || text.contains("logout")
                || text.contains("user menu")
                || text.contains("sign out")
                || text.contains("redirected to the authenticated")
                || text.contains("logged with valid credentials");
    }

    private boolean isLoginPage(String pageName) {
        return PageReferenceMatcher.normalize(pageName).contains("login");
    }

    private String ownershipText(UiTestScenario scenario) {
        return String.join(" ",
                        scenario.title() == null ? "" : scenario.title(),
                        String.join(" ", scenario.assertions()))
                .toLowerCase(Locale.ROOT);
    }

    private String firstMatch(Set<String> pageNames, String pageName, String route) {
        for (String candidate : pageNames) {
            if (PageReferenceMatcher.matchesScenarioPage(pageName, route, candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private void addIfPresent(Set<String> target, String value) {
        String normalized = normalize(value);
        if (!normalized.isBlank()) {
            target.add(normalized);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
