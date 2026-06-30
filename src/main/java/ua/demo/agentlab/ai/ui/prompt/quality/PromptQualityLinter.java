package ua.demo.agentlab.ai.ui.prompt.quality;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class PromptQualityLinter {

    private static final Pattern INLINE_BY_USAGE = Pattern.compile("elements\\.\\w+\\s*\\(\\s*By\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_WEBDRIVER_USAGE = Pattern.compile(
            "(driver\\.|\\.findElement\\s*\\(|new\\s+WebDriver|WebDriverWait|JavascriptExecutor|new\\s+Actions\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXTERNAL_LOCATOR = Pattern.compile(
            "(sameOrigin\\s*=\\s*false|external-origin|external-link-text-xpath|a\\[href=['\"]https?://)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WEAK_URL_ASSERTION = Pattern.compile("!\\s*getCurrentUrl\\s*\\(\\s*\\)\\.isBlank\\s*\\(");
    private static final Pattern STALE_LOCATOR = Pattern.compile(
            "(pASSWORD|(?i:id\\s*=\\s*login\\b[^\\n]*(password|passwordInput)"
                    + "|(password|passwordInput)[^\\n]*(id\\s*=\\s*login\\b|strategy=id,\\s*value=login\\b)"
                    + "|strategy=id,\\s*value=login\\b[^\\n]*(password|passwordInput)))"
    );

    public PromptQualityReport validate(
            String prompt,
            String pageName,
            AiContextPackage scopedContext,
            List<UiTestScenario> pageScenarios,
            AiPageObjectSpec baselineSpec
    ) {
        String safePrompt = prompt == null ? "" : prompt;
        String targetPage = pageName == null ? "" : pageName.trim();
        String targetRoute = resolveTargetRoute(scopedContext, pageScenarios, baselineSpec);
        List<String> requirementIds = pageScenarios == null
                ? List.of()
                : pageScenarios.stream().map(UiTestScenario::id).filter(id -> id != null && !id.isBlank()).toList();

        List<PromptQualityIssue> issues = new ArrayList<>();
        require(issues, containsAny(safePrompt, "pageName=" + targetPage, "\"pageName\": \"" + targetPage + "\""),
                "TARGET_PAGE_PRESENT", "Prompt must identify the target page", targetPage);
        require(issues, targetRoute.isBlank()
                        || containsAny(safePrompt, "route=" + targetRoute, "targetRoute: " + targetRoute,
                        "\"route\": \"" + targetRoute + "\""),
                "TARGET_ROUTE_PRESENT", "Prompt must identify the target route", targetRoute);
        require(issues, !requirementIds.isEmpty() && requirementIds.stream().allMatch(safePrompt::contains),
                "REQUIREMENT_IDS_PRESENT", "Prompt must include all scoped requirement ids", String.join(", ", requirementIds));
        require(issues, safePrompt.contains("Defined test cases:") && !requirementIds.isEmpty(),
                "CANONICAL_TEST_CASES_PRESENT", "Prompt must include canonical/defined test cases", "Defined test cases");
        require(issues, containsAny(safePrompt, "expectedValue=", "expectedValues:", "expected: [")
                        && !safePrompt.contains("expectedValue=null"),
                "EXPECTED_VALUES_PRESENT", "Prompt must include concrete expected values", "expectedValue/expectedValues");
        require(issues, containsAny(safePrompt, "requiredLocators=", "stableLocators=", "Allowed locators:", "\"locators\":"),
                "ALLOWED_LOCATORS_PRESENT", "Prompt must include allowed locator evidence or required locators",
                "requiredLocators/stableLocators");
        require(issues, containsAny(safePrompt, "Allowed locators:"),
                "PROMPT_UI_EVIDENCE_PRESENT", "Prompt must include PromptUiEvidence allowed locator section",
                "Prompt UI evidence");
        require(issues, safePrompt.contains("forbiddenMethods="),
                "FORBIDDEN_METHODS_PRESENT", "Prompt must include forbidden methods", "forbiddenMethods");
        require(issues, containsAny(safePrompt, "Baseline page object spec:", "Available inherited public BasePage methods"),
                "BASELINE_API_PRESENT", "Prompt must include baseline API or inherited BasePage API", "baseline/BasePage API");

        forbid(issues, EXTERNAL_LOCATOR, safePrompt,
                "NO_EXTERNAL_ORIGIN_NAVIGATION", "Prompt must not include external-origin locator/navigation evidence");
        forbid(issues, RAW_WEBDRIVER_USAGE, safePrompt,
                "NO_RAW_WEBDRIVER_USAGE", "Prompt must not include raw WebDriver usage");
        forbid(issues, INLINE_BY_USAGE, safePrompt,
                "NO_UNDECLARED_INLINE_LOCATOR_USAGE", "Prompt must not include inline By selectors in page methods");
        forbid(issues, STALE_LOCATOR, allowedLocatorEvidence(safePrompt, baselineSpec),
                "NO_STALE_LOCATOR_CANDIDATES", "Prompt must not include known stale locator candidates");
        forbid(issues, WEAK_URL_ASSERTION, baselineMethodBodies(baselineSpec),
                "NO_WEAK_URL_NONBLANK_ASSERTIONS", "Prompt must not include weak URL nonblank assertions");

        issues.addAll(validateContext(targetPage, targetRoute, scopedContext));
        issues.addAll(validatePromptEvidence(scopedContext));

        return new PromptQualityReport(
                "page-object-spec",
                targetPage,
                targetRoute,
                requirementIds,
                issues
        );
    }

    private List<PromptQualityIssue> validatePromptEvidence(AiContextPackage scopedContext) {
        if (scopedContext == null || scopedContext.promptUiEvidence() == null) {
            return List.of();
        }
        List<PromptQualityIssue> issues = new ArrayList<>();
        scopedContext.promptUiEvidence().requiredLocators().forEach(locator -> {
            if (!locator.sameOrigin()) {
                issues.add(new PromptQualityIssue(
                        PromptQualitySeverity.BLOCKER,
                        "NO_EXTERNAL_ORIGIN_PROMPT_LOCATORS",
                        "PromptUiEvidence allowed locators must be same-origin",
                        locator.elementName() + " " + locator.strategy() + "=" + locator.value()
                ));
            }
            if (locator.stabilityScore() < 0.75d) {
                issues.add(new PromptQualityIssue(
                        PromptQualitySeverity.BLOCKER,
                        "NO_LOW_CONFIDENCE_PROMPT_LOCATORS",
                        "PromptUiEvidence allowed locators must be promoted/stable",
                        locator.elementName() + " score=" + locator.stabilityScore()
                ));
            }
        });
        return issues;
    }

    private List<PromptQualityIssue> validateContext(String targetPage, String targetRoute, AiContextPackage scopedContext) {
        if (scopedContext == null || scopedContext.pageModelEnrichments() == null) {
            return List.of();
        }
        List<PromptQualityIssue> issues = new ArrayList<>();
        for (PageModelEnrichmentRecord record : scopedContext.pageModelEnrichments()) {
            boolean pageMatches = PageReferenceMatcher.matchesScenarioPage(record.pageName(), record.route(), targetPage);
            boolean routeMatches = targetRoute.isBlank() || PageReferenceMatcher.routeMatches(record.route(), targetRoute);
            if (!pageMatches && !routeMatches) {
                issues.add(new PromptQualityIssue(
                        PromptQualitySeverity.BLOCKER,
                        "NO_CROSS_ROUTE_ENRICHMENT_EVIDENCE",
                        "Prompt context contains enrichment evidence for a different page/route",
                        record.pageName() + " route=" + record.route()
                ));
            }
            if (record.stableLocators().stream().anyMatch(value -> EXTERNAL_LOCATOR.matcher(value).find())) {
                issues.add(new PromptQualityIssue(
                        PromptQualitySeverity.BLOCKER,
                        "NO_EXTERNAL_ORIGIN_ENRICHMENT_LOCATORS",
                        "Prompt context contains external-origin locator evidence",
                        record.pageName() + " route=" + record.route()
                ));
            }
        }
        return issues;
    }

    private String resolveTargetRoute(
            AiContextPackage scopedContext,
            List<UiTestScenario> pageScenarios,
            AiPageObjectSpec baselineSpec
    ) {
        if (baselineSpec != null && baselineSpec.route() != null && !baselineSpec.route().isBlank()) {
            return baselineSpec.route();
        }
        if (scopedContext != null
                && scopedContext.promptUiEvidence() != null
                && !scopedContext.promptUiEvidence().targetRoute().isBlank()) {
            return scopedContext.promptUiEvidence().targetRoute();
        }
        if (scopedContext != null && scopedContext.mappedUiKnowledge() != null) {
            var route = scopedContext.mappedUiKnowledge().pages().stream()
                    .map(page -> page.urlPattern().isBlank() ? page.url() : page.urlPattern())
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst();
            if (route.isPresent()) {
                return route.get();
            }
        }
        if (pageScenarios != null) {
            return pageScenarios.stream()
                    .map(UiTestScenario::route)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    private void require(
            List<PromptQualityIssue> issues,
            boolean condition,
            String ruleId,
            String message,
            String evidence
    ) {
        if (!condition) {
            issues.add(new PromptQualityIssue(PromptQualitySeverity.BLOCKER, ruleId, message, evidence));
        }
    }

    private void forbid(
            List<PromptQualityIssue> issues,
            Pattern pattern,
            String prompt,
            String ruleId,
            String message
    ) {
        var matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            issues.add(new PromptQualityIssue(
                    PromptQualitySeverity.BLOCKER,
                    ruleId,
                    message,
                    snippet(prompt, matcher.start(), matcher.end())
            ));
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String allowedLocatorEvidence(String prompt, AiPageObjectSpec baselineSpec) {
        StringBuilder builder = new StringBuilder();
        if (baselineSpec != null) {
            builder.append(baselineSpec.locators()).append(System.lineSeparator());
        }
        if (prompt == null || prompt.isBlank()) {
            return builder.toString();
        }
        for (String line : prompt.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- forbiddenLocators=") || trimmed.startsWith("- forbiddenMethods=")) {
                continue;
            }
            if (trimmed.startsWith("- requiredLocators=")
                    || trimmed.startsWith("- stableLocators=")
                    || trimmed.startsWith("- ") && trimmed.contains(" | strategy=") && trimmed.contains(" | value=")
                    || trimmed.contains("\"locators\"")
                    || trimmed.contains("\"value\": \"<stable locator value")) {
                builder.append(trimmed).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }

    private String baselineMethodBodies(AiPageObjectSpec baselineSpec) {
        if (baselineSpec == null || baselineSpec.methods() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        baselineSpec.methods().forEach(method -> builder
                .append(method.methodName())
                .append(" ")
                .append(method.body())
                .append(System.lineSeparator()));
        return builder.toString();
    }

    private String snippet(String text, int start, int end) {
        int safeStart = Math.max(0, start - 80);
        int safeEnd = Math.min(text.length(), end + 80);
        return text.substring(safeStart, safeEnd).replaceAll("\\s+", " ").trim();
    }
}
