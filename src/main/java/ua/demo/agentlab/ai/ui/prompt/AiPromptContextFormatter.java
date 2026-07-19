package ua.demo.agentlab.ai.ui.prompt;

import ua.demo.agentlab.ai.assertions.model.AssertionContract;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiTestScenario;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;
import ua.demo.agentlab.ui.discovery.mapping.model.AssertionHint;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedForm;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageActionModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageElementModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFlowModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageFormModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageLocatorModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ai.context.CanonicalUiInteraction;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.context.UiKnowledgeGraphMatch;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiPromptContextFormatter {

    private static final Pattern ENTITY_DATA_PATTERN = Pattern.compile(
            "^\\s*([^,]+?)\\s*,\\s*Size\\s*=\\s*([^,]+?)\\s*,\\s*Color\\s*=\\s*([^,]+?)\\s+must\\b.*",
            Pattern.CASE_INSENSITIVE
    );

    public String summarize(AiContextPackage context) {
        return summarize(context, "");
    }

    public String summarize(AiContextPackage context, String requestedPageName) {
        StringBuilder builder = new StringBuilder();
        builder.append("Objective: ").append(context.objective()).append(System.lineSeparator());
        if (context.projectProfile() != null) {
            builder.append("Project: ").append(context.projectProfile().projectName())
                    .append(" | baseUrl=").append(context.projectProfile().baseUrl())
                    .append(System.lineSeparator());
            builder.append("Routes: ").append(summarizeConfiguredRoutes(context.projectProfile()))
                    .append(System.lineSeparator());
        }
        builder.append("Policy: ").append(summarizePolicy(context)).append(System.lineSeparator());
        builder.append("Template capabilities: ").append(context.templateCapabilities()).append(System.lineSeparator());
        builder.append(System.lineSeparator()).append("Defined test cases:").append(System.lineSeparator());
        builder.append(summarizeDefinedTestCases(context, requestedPageName));
        return builder.toString().strip();
    }

    public String summarizeCompactContext(AiContextPackage context) {
        if (context == null) {
            return "Objective: Generate one scoped Page Object contract";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Objective: ").append(context.objective()).append(System.lineSeparator());
        if (context.projectProfile() != null) {
            builder.append("Project: ").append(context.projectProfile().projectName())
                    .append(" | baseUrl=").append(context.projectProfile().baseUrl())
                    .append(System.lineSeparator());
            builder.append("Active routes: ").append(summarizeConfiguredRoutes(context.projectProfile()))
                    .append(System.lineSeparator());
        }
        builder.append("Policy: ").append(summarizePolicy(context));
        return builder.toString().strip();
    }

    public String summarizeRequirements(AiContextPackage context) {
        if (context.normalizedRequirementBundle() == null || context.normalizedRequirementBundle().requirements() == null) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (NormalizedRequirement requirement : context.normalizedRequirementBundle().requirements()) {
            builder.append("- ")
                    .append(requirement.id())
                    .append(" | title=").append(requirement.title())
                    .append(" | tags=").append(requirement.tags())
                    .append(" | ui=").append(requirement.uiRelevant())
                    .append(" | api=").append(requirement.apiRelevant())
                    .append(" | source=").append(toSourceReference(requirement.sourceReference()))
                    .append(System.lineSeparator());
            builder.append("  statement: ").append(requirement.statement()).append(System.lineSeparator());
            if (requirement.expectedResult() != null && !requirement.expectedResult().isBlank()) {
                builder.append("  expectedResult: ").append(requirement.expectedResult()).append(System.lineSeparator());
            }
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeFlows(AiContextPackage context) {
        if (context.canonicalPageFlowModel() == null || context.canonicalPageFlowModel().flows() == null) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        context.canonicalPageFlowModel().flows().stream().limit(12).forEach(flow -> {
            builder.append("- ")
                    .append(flow.flowId()).append(" | ")
                    .append(flow.flowName()).append(" | type=").append(flow.flowType())
                    .append(" | sourcePage=").append(flow.sourcePageName())
                    .append(" | targetPage=").append(flow.targetPageName())
                    .append(" | keywords=").append(flow.matchKeywords())
                    .append(System.lineSeparator());
        });
        return builder.toString().stripTrailing();
    }

    public String summarizeCanonicalInteractions(AiContextPackage context) {
        if (context.canonicalInteractionModel() == null || context.canonicalInteractionModel().interactions().isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        context.canonicalInteractionModel().interactions().stream().limit(16).forEach(interaction -> builder
                .append("- ")
                .append(interaction.pageName())
                .append(" | canonical=").append(interaction.canonicalName())
                .append(" | type=").append(interaction.interactionType())
                .append(" | subject=").append(interaction.subjectType())
                .append(" | targetType=").append(interaction.targetType())
                .append(" | sourceElement=").append(interaction.sourceElementName())
                .append(" | targetPageId=").append(interaction.targetPageId())
                .append(" | targetRoute=").append(interaction.targetRoute())
                .append(" | hints=").append(interaction.domainHints())
                .append(" | confidence=").append(String.format(Locale.ROOT, "%.2f", interaction.confidenceScore()))
                .append(System.lineSeparator()));
        return builder.toString().stripTrailing();
    }

    public String summarizeMappedPages(AiContextPackage context) {
        if (context.mappedUiKnowledge() == null || context.mappedUiKnowledge().pages() == null) {
            return "- none";
        }
        Set<String> relevantKeywords = buildRelevantKeywords(context);
        StringBuilder builder = new StringBuilder();
        for (MappedPage page : context.mappedUiKnowledge().pages()) {
            builder.append("- ").append(page.pageName())
                    .append(" | type=").append(page.pageType())
                    .append(" | route=").append(page.urlPattern())
                    .append(" | elements=").append(page.elements().size())
                    .append(" | forms=").append(page.forms().size())
                    .append(" | actions=").append(page.actions().size())
                    .append(System.lineSeparator());
            page.elements().stream()
                    .filter(element -> isRelevantElement(element, relevantKeywords))
                    .limit(8)
                    .forEach(element -> builder.append("  element: ")
                    .append(element.semanticName())
                    .append(" | type=").append(element.elementType())
                    .append(" | actions=").append(element.supportedActions())
                    .append(" | locator=").append(element.locatorCandidates().isEmpty() ? "none"
                            : element.locatorCandidates().get(0).strategy().wireName() + "=" + element.locatorCandidates().get(0).value())
                    .append(System.lineSeparator()));
            page.actions().stream()
                    .filter(action -> isRelevantAction(action, relevantKeywords))
                    .limit(8)
                    .forEach(action -> builder.append("  action: ")
                    .append(action.actionName())
                    .append(" | type=").append(action.actionType())
                    .append(" | target=").append(action.targetPageId())
                    .append(System.lineSeparator()));
            page.assertionHints().stream()
                    .filter(hint -> isRelevantAssertionHint(hint, relevantKeywords))
                    .limit(6)
                    .forEach(hint -> builder.append("  assertionHint: ")
                    .append(hint.hintType())
                    .append(" | target=").append(hint.target())
                    .append(System.lineSeparator()));
        }
        return builder.toString().stripTrailing();
    }

    public String summarizePromptUiEvidence(AiContextPackage context) {
        if (context == null || context.promptUiEvidence() == null) {
            return "- none";
        }
        PromptUiEvidence evidence = context.promptUiEvidence();
        StringBuilder builder = new StringBuilder();
        builder.append("- targetPage=").append(evidence.targetPage())
                .append(" | targetRoute=").append(evidence.targetRoute())
                .append(" | requiresAuthentication=").append(evidence.requiresAuthentication())
                .append(" | confidence=").append(String.format(Locale.ROOT, "%.2f", evidence.confidence()))
                .append(System.lineSeparator());
        builder.append("- prerequisitePages=").append(evidence.prerequisitePages()).append(System.lineSeparator());
        builder.append("- requirementIds=").append(evidence.requirementIds()).append(System.lineSeparator());
        builder.append("Required actions:").append(System.lineSeparator());
        if (evidence.requiredActions().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            evidence.requiredActions().stream().limit(10).forEach(action -> builder
                    .append("- ").append(action.name())
                    .append(" | type=").append(action.type())
                    .append(" | owner=").append(action.ownerPage())
                    .append(" | source=").append(action.sourceTrace())
                    .append(System.lineSeparator()));
        }
        builder.append("Required assertions:").append(System.lineSeparator());
        if (evidence.requiredAssertions().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            evidence.requiredAssertions().stream().limit(10).forEach(assertion -> builder
                    .append("- ").append(assertion.type())
                    .append(" | expectedValue=").append(assertion.expectedValue())
                    .append(" | owner=").append(assertion.ownerPage())
                    .append(" | confidence=").append(String.format(Locale.ROOT, "%.2f", assertion.confidence()))
                    .append(System.lineSeparator()));
        }
        builder.append("Allowed locators:").append(System.lineSeparator());
        if (evidence.requiredLocators().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            appendGroupedAllowedLocators(builder, evidence.requiredLocators().stream().limit(12).toList());
        }
        builder.append("Excluded evidence:").append(System.lineSeparator());
        if (evidence.excludedEvidence().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            evidence.excludedEvidence().stream().limit(8).forEach(excluded -> builder
                    .append("- type=").append(excluded.evidenceType())
                    .append(" | reason=").append(promptSafeExcludedReason(excluded.reason()))
                    .append(" | value=<redacted>")
                    .append(System.lineSeparator()));
        }
        return builder.toString().stripTrailing();
    }

    public String summarizePromptRequiredContract(
            AiContextPackage context,
            String requestedPageName,
            List<UiTestScenario> pageScenarios
    ) {
        if (context == null || context.promptUiEvidence() == null) {
            return "- none";
        }
        PromptUiEvidence evidence = context.promptUiEvidence();
        StringBuilder builder = new StringBuilder();
        builder.append("- targetPage=").append(evidence.targetPage())
                .append(" | targetRoute=").append(evidence.targetRoute())
                .append(" | requiresAuthentication=").append(evidence.requiresAuthentication())
                .append(" | confidence=").append(String.format(Locale.ROOT, "%.2f", evidence.confidence()))
                .append(System.lineSeparator());
        builder.append("- prerequisitePages=").append(evidence.prerequisitePages()).append(System.lineSeparator());
        builder.append("- requirementIds=").append(evidence.requirementIds()).append(System.lineSeparator());
        builder.append("Page-owned actions:").append(System.lineSeparator());
        if (evidence.requiredActions().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            evidence.requiredActions().forEach(action -> builder
                    .append("- ").append(action.name())
                    .append(System.lineSeparator()));
        }
        builder.append("Page-owned assertions:").append(System.lineSeparator());
        if (evidence.requiredAssertions().isEmpty()) {
            builder.append("- none").append(System.lineSeparator());
        } else {
            evidence.requiredAssertions().forEach(assertion -> builder
                    .append("- ").append(assertion.type())
                    .append(" | expectedValue=").append(assertion.expectedValue())
                    .append(" | confidence=").append(String.format(Locale.ROOT, "%.2f", assertion.confidence()))
                    .append(" | source=").append(assertion.sourceTrace())
                    .append(System.lineSeparator()));
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeScopedPomTestCases(
            AiContextPackage context,
            String requestedPageName,
            List<UiTestScenario> pageScenarios
    ) {
        if (pageScenarios == null || pageScenarios.isEmpty()) {
            return "- none";
        }
        Map<String, CanonicalTestCase> canonicalById = new LinkedHashMap<>();
        if (context != null && context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                canonicalById.put(testCase.id(), testCase);
            }
        }
        StringBuilder builder = new StringBuilder();
        pageScenarios.stream().limit(12).forEach(scenario -> {
            CanonicalTestCase testCase = canonicalById.get(scenario.id());
            if (testCase == null) {
                appendCompactScenario(builder, scenario, requestedPageName);
            } else {
                appendCompactCanonicalTestCase(builder, testCase, requestedPageName);
            }
        });
        return builder.toString().stripTrailing();
    }

    private void appendCompactCanonicalTestCase(
            StringBuilder builder,
            CanonicalTestCase testCase,
            String requestedPageName
    ) {
        String pageRole = pageRole(testCase, requestedPageName);
        builder.append("- ").append(testCase.id())
                .append(" | title=").append(testCase.title())
                .append(" | pageRole=").append(pageRole)
                .append(" | source=").append(testCase.sourcePageName()).append(" ").append(testCase.sourceRoute())
                .append(" | target=").append(testCase.pageName()).append(" ").append(testCase.route())
                .append(System.lineSeparator());
        builder.append("  operations=").append(testCase.operationIntents().stream()
                        .map(intent -> intent.kind().name())
                        .distinct()
                        .toList())
                .append(" | assertions=").append(testCase.assertionIntents().stream()
                        .map(intent -> intent.kind().name())
                        .distinct()
                        .toList())
                .append(System.lineSeparator());
        if ("source-action-owner".equals(pageRole) || "context-only".equals(pageRole)) {
            builder.append("  ownedExpectedValues=[] (target-page assertion values omitted)")
                    .append(System.lineSeparator());
        } else {
            builder.append("  expectedValues=").append(testCase.assertionIntents().stream()
                            .map(AssertionIntent::expectedValue)
                            .filter(value -> value != null && !value.isBlank())
                            .distinct()
                            .toList())
                    .append(System.lineSeparator());
        }
    }

    private void appendCompactScenario(
            StringBuilder builder,
            UiTestScenario scenario,
            String requestedPageName
    ) {
        builder.append("- ").append(scenario.id())
                .append(" | title=").append(scenario.title())
                .append(" | pageRole=").append(pageRole(scenario, requestedPageName))
                .append(" | source=").append(scenario.sourcePageName()).append(" ").append(scenario.sourceRoute())
                .append(" | target=").append(scenario.pageName()).append(" ").append(scenario.route())
                .append(System.lineSeparator());
        builder.append("  operations=").append(scenario.operationIntents().stream()
                        .map(intent -> intent.kind().name())
                        .distinct()
                        .toList())
                .append(" | assertions=").append(scenario.assertionIntents().stream()
                        .map(intent -> intent.kind().name())
                        .distinct()
                        .toList())
                .append(System.lineSeparator());
    }

    private String pageRole(CanonicalTestCase testCase, String requestedPageName) {
        boolean source = PageReferenceMatcher.matchesScenarioPage(
                testCase.sourcePageName(),
                testCase.sourceRoute(),
                requestedPageName
        );
        boolean target = PageReferenceMatcher.matchesScenarioPage(
                testCase.pageName(),
                testCase.route(),
                requestedPageName
        );
        if (source && target) {
            return "source-and-target";
        }
        if (source) {
            return "source-action-owner";
        }
        if (target) {
            return "target-assertion-owner";
        }
        return "context-only";
    }

    private String pageRole(UiTestScenario scenario, String requestedPageName) {
        boolean source = PageReferenceMatcher.matchesScenarioPage(
                scenario.sourcePageName(),
                scenario.sourceRoute(),
                requestedPageName
        );
        boolean target = PageReferenceMatcher.matchesScenarioPage(
                scenario.pageName(),
                scenario.route(),
                requestedPageName
        );
        if (source && target) {
            return "source-and-target";
        }
        if (source) {
            return "source-action-owner";
        }
        if (target) {
            return "target-assertion-owner";
        }
        return "context-only";
    }

    private List<ua.demo.agentlab.ai.context.PromptAssertionEvidence> summarizeScopedAssertions(
            AiContextPackage context,
            PromptUiEvidence evidence,
            String requestedPageName,
            Set<String> scopedIds
    ) {
        if (context != null && context.assertionContracts() != null && !context.assertionContracts().isEmpty()) {
            return context.assertionContracts().stream()
                    .filter(contract -> scopedIds.isEmpty() || scopedIds.contains(contract.testCaseId()))
                    .filter(contract -> belongsToRequestedPage(contract.ownerPage(), requestedPageName))
                    .map(contract -> new ua.demo.agentlab.ai.context.PromptAssertionEvidence(
                            contract.type().name(),
                            contract.expectedValue(),
                            contract.ownerPage(),
                            contract.sourceLine(),
                            contract.confidence()
                    ))
                    .limit(12)
                    .toList();
        }
        return evidence.requiredAssertions().stream()
                .filter(assertion -> belongsToRequestedPage(assertion.ownerPage(), requestedPageName))
                .filter(assertion -> belongsToScopedRequirements(assertion.sourceTrace(), scopedIds))
                .limit(12)
                .toList();
    }

    public String summarizeAllowedPromptLocators(AiContextPackage context) {
        if (context == null || context.promptUiEvidence() == null
                || context.promptUiEvidence().requiredLocators().isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        appendGroupedAllowedLocators(builder, context.promptUiEvidence().requiredLocators().stream().limit(16).toList());
        return builder.toString().stripTrailing();
    }

    private void appendGroupedAllowedLocators(
            StringBuilder builder,
            List<ua.demo.agentlab.ai.context.PromptLocatorEvidence> locators
    ) {
        Map<String, List<ua.demo.agentlab.ai.context.PromptLocatorEvidence>> byComponent = new LinkedHashMap<>();
        for (ua.demo.agentlab.ai.context.PromptLocatorEvidence locator : locators) {
            String component = locator.componentName().isBlank() ? "PageScope" : locator.componentName();
            byComponent.computeIfAbsent(component, ignored -> new java.util.ArrayList<>()).add(locator);
        }
        byComponent.forEach((component, componentLocators) -> {
            builder.append("component: ").append(component);
            String componentType = componentLocators.stream()
                    .map(ua.demo.agentlab.ai.context.PromptLocatorEvidence::componentType)
                    .filter(type -> type != null && !type.isBlank())
                    .findFirst()
                    .orElse("");
            if (!componentType.isBlank()) {
                builder.append(" | type=").append(componentType);
            }
            builder.append(System.lineSeparator());
            componentLocators.forEach(locator -> builder
                .append("- ").append(locator.fieldHint())
                .append(" | element=").append(locator.elementName())
                .append(" | strategy=").append(locator.strategy())
                .append(" | value=").append(locator.value())
                .append(" | role=").append(locator.role())
                .append(" | evidenceType=").append(locator.evidenceType())
                .append(" | sameOrigin=").append(locator.sameOrigin())
                .append(" | uniqueWithinComponent=").append(locator.uniqueWithinComponent())
                .append(" | globalCount=").append(locator.globalMatchCount())
                .append(" | scopedCount=").append(locator.scopedMatchCount())
                .append(" | score=").append(String.format(Locale.ROOT, "%.2f", locator.stabilityScore()))
                .append(System.lineSeparator()));
        });
    }

    private boolean belongsToRequestedPage(String ownerPage, String requestedPageName) {
        if (ownerPage == null || ownerPage.isBlank() || requestedPageName == null || requestedPageName.isBlank()) {
            return true;
        }
        return PageReferenceMatcher.matchesScenarioPage(ownerPage, "", requestedPageName);
    }

    private boolean belongsToScopedRequirements(String sourceTrace, Set<String> scopedIds) {
        if (scopedIds == null || scopedIds.isEmpty()) {
            return true;
        }
        String source = sourceTrace == null ? "" : sourceTrace;
        return scopedIds.stream().anyMatch(source::contains);
    }

    private String promptSafeExcludedReason(String reason) {
        String safe = reason == null ? "" : reason;
        return safe
                .replaceAll("(?i)external-origin", "origin-policy")
                .replaceAll("https?://\\S+", "<redacted-url>")
                .replaceAll("(?i)external-link-text-xpath", "origin-policy-locator");
    }

    public String summarizeRetrievalContext(AiContextPackage context) {
        if (context.retrievalContext() == null) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("query=").append(context.retrievalContext().query()).append(System.lineSeparator());
        builder.append("queryTerms=").append(context.retrievalContext().queryTerms()).append(System.lineSeparator());
        builder.append("vectorSource=").append(context.retrievalContext().vectorSource())
                .append(" | matches=").append(context.retrievalContext().vectorMatches().size())
                .append(System.lineSeparator());
        context.retrievalContext().vectorMatches().stream().limit(6).forEach(match -> builder
                .append("- vector | score=").append(String.format(Locale.ROOT, "%.3f", match.score()))
                .append(" | artifact=").append(match.metadata() == null ? "" : match.metadata().artifactName())
                .append(" | package=").append(match.metadata() == null ? "" : match.metadata().packageName())
                .append(" | tags=").append(match.metadata() == null ? List.of() : match.metadata().tags())
                .append(System.lineSeparator())
                .append("  text: ").append(truncate(match.text(), 220)).append(System.lineSeparator()));
        builder.append("graphSource=").append(context.retrievalContext().graphSource())
                .append(" | matches=").append(context.retrievalContext().graphMatches().size())
                .append(System.lineSeparator());
        context.retrievalContext().graphMatches().stream().limit(8).forEach(match -> appendGraphMatch(builder, match));
        if (!context.retrievalContext().notes().isEmpty()) {
            builder.append("notes=").append(context.retrievalContext().notes()).append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    public String summarizePageModelEnrichments(AiContextPackage context, String requestedPageName) {
        if (context == null || context.pageModelEnrichments().isEmpty()) {
            return "none";
        }
        List<PageModelEnrichmentRecord> records = context.pageModelEnrichments().stream()
                .filter(record -> ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher.matchesScenarioPage(
                        record.pageName(), record.route(), requestedPageName))
                .limit(1)
                .toList();
        if (records.isEmpty()) {
            return "none";
        }
        PageModelEnrichmentRecord record = records.get(0);
        return "- source=" + record.enrichmentSource()
                + " | confidence=" + record.confidenceScore()
                + "\n- businessIntent=" + record.businessIntent()
                + "\n- summary=" + record.pageSummary()
                + "\n- supportedActions=" + record.supportedActions()
                + "\n- stableLocators=" + record.stableLocators()
                + "\n- preconditions=" + record.preconditions()
                + "\n- postconditions=" + record.postconditions()
                + "\n- risks=" + record.risks()
                + "\n- coverageGaps=" + record.coverageGaps()
                + "\n- traceability=" + record.requirementTraceability();
    }

    public String summarizeStructuredRetrievalContext(AiContextPackage context) {
        if (context == null || context.retrievalContext() == null) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        var retrieval = context.retrievalContext();
        builder.append("retrievalMode=").append(retrieval.retrievalMode()).append(System.lineSeparator());
        builder.append("neo4jHit=").append(retrieval.neo4jHit())
                .append(" | qdrantHit=").append(retrieval.qdrantHit())
                .append(" | stableCacheUsed=").append(retrieval.stableCacheUsed())
                .append(System.lineSeparator());
        builder.append("staleEvidenceRejected=").append(retrieval.staleEvidenceRejected())
                .append(" | vectorUnavailableReason=")
                .append(retrieval.vectorUnavailableReason().isBlank() ? "none" : retrieval.vectorUnavailableReason())
                .append(System.lineSeparator());
        builder.append("Graph evidence count: ").append(retrieval.graphMatches().size()).append(System.lineSeparator());
        builder.append("Vector evidence count: ").append(retrieval.vectorMatches().size()).append(System.lineSeparator());
        builder.append("Raw retrieved text is intentionally excluded from POM prompts; retrieval is used only before prompt as ranking/enrichment metadata.");
        return builder.toString().stripTrailing();
    }

    public String summarizeRawPageObjectDiscoveryFacts(AiContextPackage context) {
        if (context == null) {
            return "- none";
        }
        boolean hasPageModels = context.pageModelBundle() != null && !context.pageModelBundle().pages().isEmpty();
        boolean hasMappedPages = context.mappedUiKnowledge() != null && !context.mappedUiKnowledge().pages().isEmpty();
        if (!hasPageModels && !hasMappedPages) {
            return "- none";
        }

        Set<String> relevantKeywords = buildRelevantKeywords(context);
        StringBuilder builder = new StringBuilder();
        builder.append("Use these discovered UI facts before baseline fallback. Prefer stable id/name/css locators marked here.")
                .append(System.lineSeparator());
        if (hasPageModels) {
            context.pageModelBundle().pages().stream()
                    .limit(8)
                    .forEach(page -> appendPageModelFacts(builder, page, relevantKeywords));
        }
        if (hasMappedPages) {
            appendMappedFormFacts(builder, context.mappedUiKnowledge().pages());
        }
        if (context.mappedUiKnowledge() != null && !context.mappedUiKnowledge().transitions().isEmpty()) {
            builder.append("Discovered transitions:").append(System.lineSeparator());
            context.mappedUiKnowledge().transitions().stream().limit(12).forEach(transition -> builder
                    .append("- from=").append(transition.fromPageId())
                    .append(" | action=").append(transition.actionId())
                    .append(" | type=").append(transition.actionType())
                    .append(" | to=").append(transition.toPageId())
                    .append(" | url=").append(transition.toUrl())
                    .append(System.lineSeparator()));
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeUiPlan(List<UiTestScenario> scenarios) {
        if (scenarios == null || scenarios.isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (UiTestScenario scenario : scenarios) {
            builder.append("- ").append(scenario.id())
                    .append(" | page=").append(scenario.pageName())
                    .append(" | sourcePage=").append(scenario.sourcePageName())
                    .append(" | route=").append(scenario.route())
                    .append(" | actions=").append(scenario.actions())
                    .append(" | assertions=").append(scenario.assertions())
                    .append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeCanonicalTestCases(CanonicalTestCaseBundle bundle) {
        if (bundle == null || bundle.testCases().isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (CanonicalTestCase testCase : bundle.testCases()) {
            builder.append("- ").append(testCase.id())
                    .append(" | page=").append(testCase.pageName())
                    .append(" | targetPages=").append(testCase.targetPages())
                    .append(" | requirementRefs=").append(testCase.requirementRefs())
                    .append(" | llmSteps=").append(testCase.llmSteps())
                    .append(System.lineSeparator());
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeDefinedTestCases(CanonicalTestCaseBundle bundle) {
        return summarizeDefinedTestCases(bundle, "");
    }

    public String summarizeDefinedTestCases(CanonicalTestCaseBundle bundle, String requestedPageName) {
        if (bundle == null || bundle.testCases().isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (CanonicalTestCase testCase : bundle.testCases()) {
            appendDefinedTestCase(builder, testCase, requestedPageName, List.of());
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeDefinedTestCases(AiContextPackage context, String requestedPageName) {
        if (context == null || context.canonicalTestCaseBundle() == null
                || context.canonicalTestCaseBundle().testCases().isEmpty()) {
            return "- none";
        }
        StringBuilder builder = new StringBuilder();
        for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
            appendDefinedTestCase(builder, testCase, requestedPageName, assertionContractsFor(context, testCase.id()));
        }
        return builder.toString().stripTrailing();
    }

    public String summarizeDefinedTestCase(AiContextPackage context, UiTestScenario scenario) {
        if (scenario == null) {
            return "- none";
        }
        if (context != null && context.canonicalTestCaseBundle() != null) {
            for (CanonicalTestCase testCase : context.canonicalTestCaseBundle().testCases()) {
                if (scenario.id().equals(testCase.id())) {
                    StringBuilder builder = new StringBuilder();
                    appendDefinedTestCase(builder, testCase, "", assertionContractsFor(context, testCase.id()));
                    return builder.toString().stripTrailing();
                }
            }
        }
        return summarizeUiScenarioAsDefinedTestCase(scenario);
    }

    public String summarizeSingleScenario(UiTestScenario scenario) {
        if (scenario == null) {
            return "- none";
        }
        return summarizeUiPlan(List.of(scenario));
    }

    public String summarizePageScenarioSlice(String pageName, List<UiTestScenario> scenarios) {
        List<UiTestScenario> pageScenarios = scenarios.stream()
                .filter(scenario -> PageReferenceMatcher.matchesScenarioPage(scenario.pageName(), scenario.route(), pageName)
                        || PageReferenceMatcher.matchesScenarioPage(scenario.sourcePageName(), scenario.sourceRoute(), pageName))
                .toList();
        return summarizeUiPlan(pageScenarios);
    }

    private void appendPageModelFacts(StringBuilder builder, PageModel page, Set<String> relevantKeywords) {
        builder.append("- pageModel=").append(page.pageId())
                .append(" | route=").append(page.route())
                .append(" | feature=").append(page.featureGuess())
                .append(" | title=").append(truncate(page.title(), 80))
                .append(System.lineSeparator());
        page.elements().stream()
                .filter(element -> isPomRelevantElement(element, relevantKeywords))
                .limit(12)
                .forEach(element -> appendPageElementFact(builder, element));
        page.forms().stream()
                .filter(form -> !form.fieldElementIds().isEmpty() || !form.submitElementIds().isEmpty())
                .limit(4)
                .forEach(form -> appendPageFormFact(builder, form));
        page.flows().stream()
                .filter(PageFlowModel::success)
                .limit(4)
                .forEach(flow -> builder.append("  flow: action=").append(flow.actionLabel())
                        .append(" | type=").append(flow.actionType())
                        .append(" | to=").append(flow.toPageId())
                        .append(" | url=").append(flow.toUrl())
                        .append(System.lineSeparator()));
    }

    private void appendPageElementFact(StringBuilder builder, PageElementModel element) {
        builder.append("  element: id=").append(element.elementId())
                .append(" | tech=").append(element.technicalType())
                .append(" | semantic=").append(element.semanticType())
                .append(" | tag=").append(element.tag());
        if (!element.inputType().isBlank()) {
            builder.append(" | input=").append(element.inputType());
        }
        if (!element.text().isBlank()) {
            builder.append(" | text=").append(truncate(element.text(), 90));
        }
        if (!element.href().isBlank()) {
            builder.append(" | href=").append(element.href());
        }
        builder.append(" | visible=").append(element.visible())
                .append(" | enabled=").append(element.enabled())
                .append(" | locator=").append(formatLocator(element.bestLocator()));
        if (!element.actions().isEmpty()) {
            builder.append(" | actions=").append(element.actions().stream()
                    .map(PageActionModel::actionType)
                    .filter(action -> action != null && !action.isBlank())
                    .distinct()
                    .toList());
        }
        builder.append(System.lineSeparator());
    }

    private void appendPageFormFact(StringBuilder builder, PageFormModel form) {
        builder.append("  form: id=").append(form.formId())
                .append(" | name=").append(form.formName())
                .append(" | action=").append(form.action())
                .append(" | fields=").append(form.fieldElementIds())
                .append(" | submits=").append(form.submitElementIds())
                .append(System.lineSeparator());
    }

    private void appendMappedFormFacts(StringBuilder builder, List<MappedPage> pages) {
        builder.append("Mapped form details:").append(System.lineSeparator());
        pages.stream()
                .filter(page -> page.forms() != null && !page.forms().isEmpty())
                .limit(8)
                .forEach(page -> page.forms().stream()
                        .filter(this::isPomRelevantForm)
                        .limit(4)
                        .forEach(form -> appendMappedFormFact(builder, page, form)));
    }

    private void appendMappedFormFact(StringBuilder builder, MappedPage page, MappedForm form) {
        builder.append("- page=").append(page.pageId())
                .append(" | form=").append(form.formName())
                .append(" | action=").append(form.action())
                .append(" | submits=").append(form.submitActionIds())
                .append(System.lineSeparator());
        form.fields().stream().limit(8).forEach(field -> builder
                .append("  field: ").append(field.fieldName())
                .append(" | type=").append(field.fieldType())
                .append(" | label=").append(field.label())
                .append(" | locator=").append(formatLocator(firstLocator(field)))
                .append(System.lineSeparator()));
    }

    private void appendDefinedTestCase(
            StringBuilder builder,
            CanonicalTestCase testCase,
            String requestedPageName,
            List<AssertionContract> assertionContracts
    ) {
        builder.append("- ").append(testCase.id())
                .append(" | title=").append(testCase.title())
                .append(" | sourcePage=").append(testCase.sourcePageName())
                .append(" | page=").append(testCase.pageName())
                .append(" | route=").append(testCase.route())
                .append(" | targetPages=").append(testCase.targetPages())
                .append(" | precondition=").append(testCase.precondition())
                .append(" | source=").append(testCase.sourceReference())
                .append(System.lineSeparator());
        appendPageOwnership(builder, testCase, requestedPageName);
        builder.append("  operations: ").append(testCase.operationIntents()).append(System.lineSeparator());
        builder.append("  assertions: ").append(testCase.assertionIntents()).append(System.lineSeparator());
        builder.append("  assertionContracts: ").append(formatAssertionContracts(assertionContracts)).append(System.lineSeparator());
        builder.append("  expectedValues: ").append(testCase.assertionIntents().stream()
                .map(AssertionIntent::expectedValue)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList()).append(System.lineSeparator());
        builder.append("  actions: ").append(testCase.actions()).append(System.lineSeparator());
        builder.append("  expected: ").append(testCase.assertions()).append(System.lineSeparator());
        appendDerivedScenarioData(builder, testCase.title());
        if (shouldShowLocatorHints(testCase, requestedPageName)) {
            builder.append("  locatorHints: ").append(testCase.locatorHints()).append(System.lineSeparator());
        } else {
            builder.append("  locatorHints: [] (source-page locators omitted for requested target page)")
                    .append(System.lineSeparator());
        }
    }

    private List<AssertionContract> assertionContractsFor(AiContextPackage context, String testCaseId) {
        if (context == null || context.assertionContracts().isEmpty()) {
            return List.of();
        }
        return context.assertionContracts().stream()
                .filter(contract -> contract.testCaseId().equals(testCaseId))
                .toList();
    }

    private List<String> formatAssertionContracts(List<AssertionContract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return List.of();
        }
        return contracts.stream()
                .map(contract -> "AssertionContract{"
                        + "requirementId=" + contract.requirementId()
                        + ", testCaseId=" + contract.testCaseId()
                        + ", type=" + contract.type()
                        + ", expectedValue=" + contract.expectedValue()
                        + ", ownerPage=" + contract.ownerPage()
                        + ", route=" + contract.route()
                        + ", sourceLine=" + contract.sourceLine()
                        + ", confidence=" + String.format(Locale.ROOT, "%.2f", contract.confidence())
                        + ", source=" + contract.source()
                        + "}")
                .toList();
    }

    private void appendPageOwnership(StringBuilder builder, CanonicalTestCase testCase, String requestedPageName) {
        if (requestedPageName == null || requestedPageName.isBlank()) {
            return;
        }
        boolean sourcePage = PageReferenceMatcher.matchesScenarioPage(
                testCase.sourcePageName(),
                testCase.sourceRoute(),
                requestedPageName
        );
        boolean targetPage = PageReferenceMatcher.matchesScenarioPage(
                testCase.pageName(),
                testCase.route(),
                requestedPageName
        );
        if (sourcePage) {
            builder.append("  pageOwnership: source/action page; action locators and action methods may belong here.")
                    .append(System.lineSeparator());
        } else if (targetPage) {
            builder.append("  pageOwnership: target/result page; prerequisite/source actions belong to ")
                    .append(testCase.sourcePageName())
                    .append(", this page should expose state/assertion methods.")
                    .append(System.lineSeparator());
        }
    }

    private boolean shouldShowLocatorHints(CanonicalTestCase testCase, String requestedPageName) {
        if (requestedPageName == null || requestedPageName.isBlank()) {
            return true;
        }
        return PageReferenceMatcher.matchesScenarioPage(
                testCase.sourcePageName(),
                testCase.sourceRoute(),
                requestedPageName
        );
    }

    private String summarizeUiScenarioAsDefinedTestCase(UiTestScenario scenario) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(scenario.id())
                .append(" | title=").append(scenario.title())
                .append(" | sourcePage=").append(scenario.sourcePageName())
                .append(" | page=").append(scenario.pageName())
                .append(" | route=").append(scenario.route())
                .append(" | precondition=").append(scenario.precondition())
                .append(" | source=").append(scenario.sourceReference())
                .append(System.lineSeparator());
        builder.append("  operations: ").append(scenario.operationIntents()).append(System.lineSeparator());
        builder.append("  assertions: ").append(scenario.assertionIntents()).append(System.lineSeparator());
        builder.append("  actions: ").append(scenario.actions()).append(System.lineSeparator());
        builder.append("  expected: ").append(scenario.assertions()).append(System.lineSeparator());
        appendDerivedScenarioData(builder, scenario.title());
        builder.append("  locatorHints: ").append(scenario.locatorHints()).append(System.lineSeparator());
        return builder.toString().stripTrailing();
    }

    private void appendDerivedScenarioData(StringBuilder builder, String title) {
        Matcher matcher = ENTITY_DATA_PATTERN.matcher(title == null ? "" : title);
        if (!matcher.matches()) {
            return;
        }
        builder.append("  scenarioData: entityKey=")
                .append(matcher.group(1).trim())
                .append(", size=")
                .append(matcher.group(2).trim())
                .append(", color=")
                .append(matcher.group(3).trim())
                .append(System.lineSeparator());
    }

    private String summarizePolicy(AiContextPackage context) {
        if (context.generationPolicy() == null) {
            return "none";
        }
        return context.generationPolicy().policyId()
                + ", aiSuggestions=" + context.generationPolicy().allowAiSuggestions()
                + ", reviewGate=" + context.generationPolicy().requireReviewGate()
                + ", compileGate=" + context.generationPolicy().requireCompileGate();
    }

    private String summarizeConfiguredRoutes(ua.demo.agentlab.config.ProjectProfile profile) {
        if (profile == null) {
            return "none configured";
        }
        List<String> routes = new ua.demo.agentlab.ui.catalog.ConfirmedPageSourceResolver()
                .resolve(profile)
                .allPages()
                .stream()
                .map(page -> page.capability().name() + "=" + page.route())
                .distinct()
                .toList();
        return routes.isEmpty() ? "none configured" : String.join(", ", routes);
    }

    private String toSourceReference(SourceReference reference) {
        if (reference == null) {
            return "";
        }
        if (reference.startLine() > 0 && reference.endLine() > 0) {
            if (reference.startLine() == reference.endLine()) {
                return "%s [L%d]".formatted(reference.source(), reference.startLine());
            }
            return "%s [L%d-L%d]".formatted(reference.source(), reference.startLine(), reference.endLine());
        }
        return reference.source();
    }

    private Set<String> buildRelevantKeywords(AiContextPackage context) {
        Set<String> keywords = new LinkedHashSet<>();
        if (context.normalizedRequirementBundle() != null) {
            for (NormalizedRequirement requirement : context.normalizedRequirementBundle().requirements()) {
                addTokens(keywords, requirement.title());
                addTokens(keywords, requirement.statement());
                if (requirement.tags() != null) {
                    requirement.tags().forEach(tag -> addTokens(keywords, tag));
                }
            }
        }
        if (context.uiTestPlan() != null) {
            for (UiTestScenario scenario : context.uiTestPlan().scenarios()) {
                addTokens(keywords, scenario.title());
                scenario.actions().forEach(action -> addTokens(keywords, action));
                scenario.assertions().forEach(assertion -> addTokens(keywords, assertion));
                scenario.locatorHints().forEach(locatorHint -> addTokens(keywords, locatorHint.elementName()));
            }
        }
        return keywords;
    }

    private boolean isRelevantElement(MappedElement element, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                safe(element.semanticName()),
                safe(element.text()),
                safe(element.elementType()),
                String.join(" ", element.supportedActions()));
        return matchesAnyKeyword(haystack, keywords);
    }

    private boolean isRelevantAction(MappedAction action, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }
        String haystack = String.join(" ",
                safe(action.actionName()),
                safe(action.actionType()),
                safe(action.targetPageId()));
        return matchesAnyKeyword(haystack, keywords);
    }

    private boolean isRelevantAssertionHint(AssertionHint hint, Set<String> keywords) {
        if (keywords.isEmpty()) {
            return true;
        }
        String haystack = safe(hint.hintType()) + " " + safe(hint.target());
        return matchesAnyKeyword(haystack, keywords);
    }

    private boolean isPomRelevantElement(PageElementModel element, Set<String> relevantKeywords) {
        if (element == null) {
            return false;
        }
        if (!element.visible() && element.actions().isEmpty()) {
            return false;
        }
        String haystack = String.join(" ",
                safe(element.elementId()),
                safe(element.technicalType()),
                safe(element.semanticType()),
                safe(element.text()),
                safe(element.id()),
                safe(element.name()),
                safe(element.href()),
                element.actions().stream().map(PageActionModel::actionType).toList().toString());
        String semantic = safe(element.semanticType()).toUpperCase(Locale.ROOT);
        String technical = safe(element.technicalType()).toUpperCase(Locale.ROOT);
        boolean structurallyImportant = !element.actions().isEmpty()
                || Set.of("DETAILS", "RECORD", "COLLECTION", "CONTAINER", "SEARCH", "DROPDOWN", "INPUT")
                .contains(semantic)
                || Set.of("BUTTON", "DROPDOWN", "FORM").contains(technical);
        return structurallyImportant || matchesAnyKeyword(haystack, relevantKeywords);
    }

    private boolean isPomRelevantForm(MappedForm form) {
        if (form == null) {
            return false;
        }
        String haystack = String.join(" ",
                safe(form.formId()),
                safe(form.formName()),
                safe(form.action()),
                form.submitActionIds().toString(),
                form.fields().stream().map(MappedField::fieldName).toList().toString());
        String normalized = haystack.toLowerCase(Locale.ROOT);
        return normalized.contains("container")
                || normalized.contains("record")
                || normalized.contains("item")
                || normalized.contains("search")
                || normalized.contains("login")
                || normalized.contains("register")
                || !form.fields().isEmpty();
    }

    private String formatLocator(PageLocatorModel locator) {
        if (locator == null || locator.strategy().isBlank() || locator.value().isBlank()) {
            return "none";
        }
        return locator.strategy() + "=" + locator.value()
                + " | unique=" + locator.unique()
                + " | score=" + String.format(Locale.ROOT, "%.2f", locator.score());
    }

    private String formatLocator(LocatorCandidate locator) {
        if (locator == null || locator.strategy().isBlank() || locator.value().isBlank()) {
            return "none";
        }
        String origin = locator.originHost().isBlank() ? "same-origin" : locator.originHost();
        return locator.strategy().wireName() + "=" + locator.value()
                + " | stability=" + String.format(Locale.ROOT, "%.2f", locator.stabilityScore())
                + " | sameOrigin=" + locator.sameOrigin()
                + " | origin=" + origin
                + (locator.risks().isEmpty() ? "" : " | risks=" + locator.risks());
    }

    private LocatorCandidate firstLocator(MappedField field) {
        if (field == null || field.locatorCandidates().isEmpty()) {
            return null;
        }
        return field.locatorCandidates().get(0);
    }

    private boolean matchesAnyKeyword(String text, Set<String> keywords) {
        String normalizedText = safe(text).toLowerCase(Locale.ROOT);
        return keywords.stream().anyMatch(normalizedText::contains);
    }

    private void addTokens(Set<String> keywords, String text) {
        Stream.of(safe(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/ ]", " ").split("\\s+"))
                .filter(token -> token.length() >= 4 || token.contains("/"))
                .forEach(keywords::add);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void appendGraphMatch(StringBuilder builder, UiKnowledgeGraphMatch match) {
        builder.append("- graph | nodeId=").append(match.nodeId())
                .append(" | type=").append(match.nodeType())
                .append(" | name=").append(match.name())
                .append(" | pageId=").append(match.pageId())
                .append(" | relation=").append(match.relationType())
                .append(" | relevance=").append(String.format(Locale.ROOT, "%.3f", match.score()))
                .append(System.lineSeparator());
    }

    private String truncate(String value, int maxLength) {
        String normalized = safe(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
