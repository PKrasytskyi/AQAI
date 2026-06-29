package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.ControllerRouteDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.ExistingTestDefinition;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstMethod;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstParseResult;
import ua.demo.agentlab.ai.rag.intelligence.model.JavaAstType;
import ua.demo.agentlab.ai.rag.intelligence.model.KnowledgeEnrichmentRecord;
import ua.demo.agentlab.ai.rag.intelligence.model.LayerComponentDefinition;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RuleBasedKnowledgeEnrichmentClient implements KnowledgeEnrichmentClient {

    private static final Pattern REQUIREMENT_PATTERN = Pattern.compile("\\bREQ[-_ ]?\\d+[A-Za-z]?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATOR_PATTERN = Pattern.compile(
            "By\\.(id|name|cssSelector|xpath|className|linkText|partialLinkText|tagName)\\s*\\(([^\\n;]+)\\)"
    );

    @Override
    public List<KnowledgeEnrichmentRecord> enrich(KnowledgeEnrichmentRequest request) {
        Map<String, SourceDocument> documentsByPath = request.documents().stream()
                .collect(Collectors.toMap(SourceDocument::relativePath, Function.identity(), (left, right) -> right));
        String allTestContent = request.documents().stream()
                .filter(document -> isTestPath(document.relativePath()))
                .map(SourceDocument::content)
                .collect(Collectors.joining(System.lineSeparator()));
        Map<String, LayerComponentDefinition> componentsByPath = request.layerComponents().stream()
                .collect(Collectors.toMap(LayerComponentDefinition::relativePath, Function.identity(), (left, right) -> right));
        Map<String, ExistingTestDefinition> testsByPath = request.existingTests().stream()
                .collect(Collectors.toMap(ExistingTestDefinition::relativePath, Function.identity(), (left, right) -> right));
        Map<String, List<ControllerRouteDefinition>> routesByPath = request.controllerRoutes().stream()
                .collect(Collectors.groupingBy(ControllerRouteDefinition::relativePath));

        List<KnowledgeEnrichmentRecord> records = new ArrayList<>();
        for (JavaAstParseResult astResult : request.javaAstResults()) {
            SourceDocument document = documentsByPath.get(astResult.relativePath());
            String source = document == null ? "" : document.content();
            LocatorAssessment locatorAssessment = assessLocators(source);
            for (JavaAstType type : astResult.types()) {
                records.add(buildClassRecord(
                        astResult,
                        type,
                        source,
                        locatorAssessment,
                        componentsByPath.get(astResult.relativePath()),
                        testsByPath.get(astResult.relativePath()),
                        routesByPath.getOrDefault(astResult.relativePath(), List.of()),
                        allTestContent
                ));
                for (JavaAstMethod method : type.methods()) {
                    records.add(buildMethodRecord(
                            astResult,
                            type,
                            method,
                            source,
                            locatorAssessment,
                            routesByPath.getOrDefault(astResult.relativePath(), List.of()),
                            allTestContent
                    ));
                }
            }
        }
        return records;
    }

    private KnowledgeEnrichmentRecord buildClassRecord(
            JavaAstParseResult astResult,
            JavaAstType type,
            String source,
            LocatorAssessment locatorAssessment,
            LayerComponentDefinition component,
            ExistingTestDefinition test,
            List<ControllerRouteDefinition> routes,
            String allTestContent
    ) {
        String classRole = classRole(type, astResult.relativePath(), component, test);
        List<String> dependencies = dependencies(astResult, type, List.of());
        List<String> tags = tags(classRole, type.name(), astResult.relativePath(), type.annotations());
        List<String> gaps = coverageGaps(type, allTestContent);
        return new KnowledgeEnrichmentRecord(
                astResult.relativePath() + "#" + type.name(),
                "class",
                astResult.relativePath(),
                type.name(),
                "",
                "%s class %s with %d method(s).".formatted(classRole, type.name(), type.methods().size()),
                businessIntentForClass(classRole, type.name()),
                businessMeaningForClass(classRole, type.name()),
                tags,
                dependencies,
                risks(type, locatorAssessment, gaps),
                locatorAssessment.stableLocators(),
                locatorAssessment.score(),
                preconditions(classRole, routes),
                postconditions(classRole, routes),
                gaps,
                failureClassifications(classRole, locatorAssessment, type.methods()),
                requirementTraceability(source, astResult.relativePath(), type.name(), "", routes),
                "rule-based"
        );
    }

    private KnowledgeEnrichmentRecord buildMethodRecord(
            JavaAstParseResult astResult,
            JavaAstType type,
            JavaAstMethod method,
            String source,
            LocatorAssessment locatorAssessment,
            List<ControllerRouteDefinition> routes,
            String allTestContent
    ) {
        String methodRole = methodRole(method);
        List<String> dependencies = dependencies(astResult, type, method.invocationTargets());
        List<String> gaps = methodCoverageGaps(type, method, allTestContent);
        return new KnowledgeEnrichmentRecord(
                astResult.relativePath() + "#" + type.name() + "." + method.name(),
                "method",
                astResult.relativePath(),
                type.name(),
                method.name(),
                "Method %s %s(%s) invokes %s.".formatted(
                        method.returnType(),
                        method.name(),
                        String.join(", ", method.parameterTypes()),
                        method.invocationTargets().isEmpty() ? "no parsed dependencies" : String.join(", ", method.invocationTargets())
                ),
                businessIntentForMethod(methodRole, method.name()),
                businessMeaningForMethod(methodRole, method.name()),
                tags(methodRole, method.name(), astResult.relativePath(), method.annotations()),
                dependencies,
                methodRisks(methodRole, locatorAssessment, gaps),
                locatorAssessment.stableLocators(),
                locatorAssessment.score(),
                methodPreconditions(methodRole, method),
                methodPostconditions(methodRole, method),
                gaps,
                failureClassifications(methodRole, locatorAssessment, List.of(method)),
                requirementTraceability(source, astResult.relativePath(), type.name(), method.name(), routes),
                "rule-based"
        );
    }

    private String classRole(
            JavaAstType type,
            String relativePath,
            LayerComponentDefinition component,
        ExistingTestDefinition test
    ) {
        String text = normalize(relativePath + " " + type.name() + " " + type.superClass() + " " + type.annotations());
        if (test != null || isTestPath(relativePath) || type.methods().stream().anyMatch(JavaAstMethod::testMethod)) {
            return "test";
        }
        if (text.contains("page") || text.contains("basepage")) {
            return "page-object";
        }
        if (component != null) {
            return component.componentType().name().toLowerCase(Locale.ROOT).replace('_', '-');
        }
        if (text.contains("controller")) {
            return "controller";
        }
        if (text.contains("client") || text.contains("api")) {
            return "api-client";
        }
        return "code-artifact";
    }

    private String methodRole(JavaAstMethod method) {
        String text = normalize(method.name() + " " + method.annotations() + " " + method.invocationTargets());
        if (method.testMethod() || text.contains("test")) {
            return "test-step";
        }
        if (startsWithAny(text, "is", "has", "should", "verify", "assert", "get")) {
            return "assertion-or-query";
        }
        if (containsAny(text, "login", "authenticate", "credential")) {
            return "authentication-action";
        }
        if (startsWithAny(text, "open", "navigate", "go")) {
            return "navigation-action";
        }
        if (containsAny(text, "click", "submit", "sendkeys", "clearandtype", "enter", "type")) {
            return "ui-action";
        }
        if (containsAny(text, "create", "add", "remove", "delete", "update")) {
            return "business-action";
        }
        return "method";
    }

    private String businessIntentForClass(String role, String className) {
        return switch (role) {
            case "page-object" -> "Expose reusable UI operations and assertions for " + className + ".";
            case "test" -> "Verify user-facing behavior through automated test scenarios.";
            case "controller" -> "Handle application requests and route them to business behavior.";
            case "api-client" -> "Communicate with an external or internal API boundary.";
            default -> "Support application or automation behavior in " + className + ".";
        };
    }

    private String businessMeaningForClass(String role, String className) {
        return switch (role) {
            case "page-object" -> "Represents a user-visible page or component used by UI tests.";
            case "test" -> "Documents expected product behavior as executable verification.";
            case "controller" -> "Connects product routes to backend use cases.";
            default -> "Provides implementation support for the project workflow.";
        };
    }

    private String businessIntentForMethod(String role, String methodName) {
        return switch (role) {
            case "authentication-action" -> "Authenticate a user or submit credentials.";
            case "navigation-action" -> "Move the user to a target page or route.";
            case "ui-action" -> "Perform a user interaction on the page.";
            case "assertion-or-query" -> "Expose observable state for assertions or downstream decisions.";
            case "test-step" -> "Execute and verify one automated scenario.";
            case "business-action" -> "Perform a domain operation requested by the scenario.";
            default -> "Support the behavior represented by " + methodName + ".";
        };
    }

    private String businessMeaningForMethod(String role, String methodName) {
        return switch (role) {
            case "authentication-action" -> "User signs in or attempts to access protected state.";
            case "navigation-action" -> "User reaches a page required by the scenario.";
            case "ui-action" -> "User manipulates a visible control.";
            case "assertion-or-query" -> "Automation reads evidence that the product state is correct.";
            case "test-step" -> "Executable coverage for a business requirement.";
            default -> "Technical method participating in automation or application flow.";
        };
    }

    private List<String> dependencies(JavaAstParseResult astResult, JavaAstType type, List<String> invocationTargets) {
        Set<String> dependencies = new LinkedHashSet<>();
        dependencies.addAll(astResult.imports());
        if (!type.superClass().isBlank()) {
            dependencies.add("extends " + type.superClass());
        }
        type.interfaces().forEach(value -> dependencies.add("implements " + value));
        invocationTargets.stream().filter(value -> !value.isBlank()).forEach(dependencies::add);
        return dependencies.stream().limit(30).toList();
    }

    private LocatorAssessment assessLocators(String source) {
        Matcher matcher = LOCATOR_PATTERN.matcher(source == null ? "" : source);
        List<String> stable = new ArrayList<>();
        double total = 0.0d;
        int count = 0;
        while (matcher.find()) {
            String strategy = matcher.group(1);
            String value = matcher.group(2);
            double score = scoreLocator(strategy, value);
            total += score;
            count++;
            if (score >= 0.70d) {
                stable.add("By." + strategy + "(" + value.trim() + ")");
            }
        }
        return new LocatorAssessment(count == 0 ? 1.0d : total / count, stable.stream().distinct().limit(20).toList());
    }

    private double scoreLocator(String strategy, String value) {
        String normalizedValue = normalize(value);
        return switch (strategy) {
            case "id" -> 0.95d;
            case "name" -> 0.85d;
            case "cssSelector" -> normalizedValue.contains(":nth-child") || normalizedValue.contains(">") ? 0.45d : 0.75d;
            case "xpath" -> normalizedValue.contains("/div[") || normalizedValue.contains("contains(") ? 0.40d : 0.60d;
            case "linkText" -> 0.70d;
            case "partialLinkText" -> 0.55d;
            case "className", "tagName" -> 0.50d;
            default -> 0.50d;
        };
    }

    private List<String> coverageGaps(JavaAstType type, String allTestContent) {
        if (!isPageObject(type)) {
            return List.of();
        }
        List<String> gaps = new ArrayList<>();
        String normalizedTests = normalize(allTestContent);
        for (JavaAstMethod method : type.methods()) {
            if (method.name().isBlank() || method.name().equals(type.name()) || normalize(method.name()).startsWith("get")) {
                continue;
            }
            if (!normalizedTests.contains(normalize(method.name()) + "(")) {
                gaps.add("No existing test reference found for page method " + type.name() + "." + method.name());
            }
        }
        return gaps.stream().limit(10).toList();
    }

    private List<String> methodCoverageGaps(JavaAstType type, JavaAstMethod method, String allTestContent) {
        if (!isPageObject(type) || method.name().isBlank()) {
            return List.of();
        }
        if (normalize(allTestContent).contains(normalize(method.name()) + "(")) {
            return List.of();
        }
        return List.of("No existing test reference found for page method " + type.name() + "." + method.name());
    }

    private boolean isPageObject(JavaAstType type) {
        return normalize(type.name() + " " + type.superClass()).contains("page");
    }

    private List<String> risks(JavaAstType type, LocatorAssessment locatorAssessment, List<String> gaps) {
        List<String> risks = new ArrayList<>();
        if (locatorAssessment.score() < 0.70d) {
            risks.add("Low locator stability can cause brittle UI tests.");
        }
        if (!gaps.isEmpty()) {
            risks.add("Some public page behavior appears uncovered by existing tests.");
        }
        if (type.methods().stream().anyMatch(method -> method.invocationTargets().stream().anyMatch(target -> normalize(target).contains("thread.sleep")))) {
            risks.add("Hard wait usage can cause timing-sensitive failures.");
        }
        return risks;
    }

    private List<String> methodRisks(String methodRole, LocatorAssessment locatorAssessment, List<String> gaps) {
        List<String> risks = new ArrayList<>();
        if (locatorAssessment.score() < 0.70d && containsAny(methodRole, "ui-action", "assertion")) {
            risks.add("Method relies on potentially unstable locators.");
        }
        if (!gaps.isEmpty()) {
            risks.add("Method is not referenced by parsed existing tests.");
        }
        return risks;
    }

    private List<String> failureClassifications(String role, LocatorAssessment locatorAssessment, List<JavaAstMethod> methods) {
        Set<String> failures = new LinkedHashSet<>();
        if (locatorAssessment.score() < 0.70d) {
            failures.add("LOCATOR_UNSTABLE");
        }
        if (containsAny(role, "ui-action", "page-object", "navigation", "authentication")) {
            failures.add("UI_INTERACTION_OR_WAIT");
        }
        if (containsAny(role, "test", "assertion")) {
            failures.add("ASSERTION_OR_TEST_DATA");
        }
        if (methods.stream().anyMatch(method -> method.invocationTargets().stream().anyMatch(target -> normalize(target).contains("api")))) {
            failures.add("API_DEPENDENCY");
        }
        return failures.isEmpty() ? List.of("UNKNOWN_OR_NOT_CLASSIFIED") : failures.stream().toList();
    }

    private List<String> preconditions(String role, List<ControllerRouteDefinition> routes) {
        if ("page-object".equals(role)) {
            return List.of("Browser session is available", "Page route can be opened from project profile");
        }
        if ("controller".equals(role) || !routes.isEmpty()) {
            return routes.stream().map(route -> route.httpMethod() + " " + route.fullPath() + " is called").toList();
        }
        return List.of();
    }

    private List<String> postconditions(String role, List<ControllerRouteDefinition> routes) {
        if ("page-object".equals(role)) {
            return List.of("Page exposes observable state or actions for generated tests");
        }
        if ("controller".equals(role) || !routes.isEmpty()) {
            return routes.stream().map(route -> "Response produced for " + route.httpMethod() + " " + route.fullPath()).toList();
        }
        return List.of();
    }

    private List<String> methodPreconditions(String role, JavaAstMethod method) {
        if (containsAny(role, "ui-action", "assertion", "navigation", "authentication")) {
            return List.of("Page object is initialized", "Browser is on the expected page or route");
        }
        if (method.parameterTypes().isEmpty()) {
            return List.of();
        }
        return List.of("Required method parameters are provided: " + method.parameterTypes());
    }

    private List<String> methodPostconditions(String role, JavaAstMethod method) {
        if ("assertion-or-query".equals(role)) {
            return List.of("Returns observable state for test assertions");
        }
        if (containsAny(role, "ui-action", "navigation", "authentication")) {
            return List.of("UI state changes or navigation result can be asserted");
        }
        return List.of();
    }

    private List<String> requirementTraceability(
            String source,
            String relativePath,
            String className,
            String methodName,
            List<ControllerRouteDefinition> routes
    ) {
        Set<String> trace = new LinkedHashSet<>();
        Matcher matcher = REQUIREMENT_PATTERN.matcher(source == null ? "" : source);
        while (matcher.find()) {
            trace.add("Requirement " + matcher.group().replace(" ", "-") + " -> " + className
                    + (methodName.isBlank() ? "" : "." + methodName));
        }
        routes.forEach(route -> trace.add("Route " + route.httpMethod() + " " + route.fullPath() + " -> " + relativePath));
        if (trace.isEmpty() && isTestPath(relativePath)) {
            trace.add("Test artifact -> " + className + (methodName.isBlank() ? "" : "." + methodName));
        }
        return trace.stream().limit(20).toList();
    }

    private List<String> tags(String role, String name, String relativePath, List<String> annotations) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(role);
        if (isTestPath(relativePath)) {
            tags.add("test");
        }
        if (normalize(name).contains("page")) {
            tags.add("page-object");
        }
        annotations.stream().map(this::normalize).filter(value -> !value.isBlank()).forEach(tags::add);
        return tags.stream().limit(20).toList();
    }

    private boolean isTestPath(String relativePath) {
        String normalized = normalize(relativePath);
        return normalized.contains("/test/") || normalized.contains("\\test\\") || normalized.endsWith("test.java")
                || normalized.endsWith("tests.java");
    }

    private boolean containsAny(String text, String... fragments) {
        String normalized = normalize(text);
        for (String fragment : fragments) {
            if (normalized.contains(normalize(fragment))) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String text, String... prefixes) {
        String normalized = normalize(text);
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('\\', '/');
    }

    private record LocatorAssessment(double score, List<String> stableLocators) {
    }
}
