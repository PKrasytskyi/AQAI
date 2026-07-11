package ua.demo.agentlab.ai.pageenrichment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentFailure;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.runtime.skill.RuntimeSkillPromptLoader;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiPageModelEnrichmentClient implements PageModelEnrichmentClient {

    private final PageModelEnrichmentClient baselineClient;
    private final OpenAiResponseGenerationClient generationClient;
    private final RuntimeSkillPromptLoader skillPromptLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();
    private List<String> lastFailures = List.of();
    private List<PageModelEnrichmentFailure> lastFailureDetails = List.of();
    private int lastAttempts;
    private int lastSuccesses;
    private int lastPromptChars;
    private int lastResponseChars;
    private int lastActualInputTokens;
    private int lastActualOutputTokens;
    private int lastActualTotalTokens;

    public OpenAiPageModelEnrichmentClient(RagRuntimeConfig config) {
        this(config, new RuleBasedPageModelEnrichmentClient());
    }

    OpenAiPageModelEnrichmentClient(RagRuntimeConfig config, PageModelEnrichmentClient baselineClient) {
        this(config, baselineClient, new RuntimeSkillPromptLoader());
    }

    OpenAiPageModelEnrichmentClient(
            RagRuntimeConfig config,
            PageModelEnrichmentClient baselineClient,
            RuntimeSkillPromptLoader skillPromptLoader
    ) {
        this.generationClient = new OpenAiResponseGenerationClient(config);
        this.baselineClient = baselineClient == null ? new RuleBasedPageModelEnrichmentClient() : baselineClient;
        this.skillPromptLoader = skillPromptLoader == null ? new RuntimeSkillPromptLoader() : skillPromptLoader;
    }

    @Override
    public List<PageModelEnrichmentRecord> enrich(List<PageModelEnrichmentInput> inputs) {
        List<PageModelEnrichmentRecord> baseline = baselineClient.enrich(inputs);
        List<PageModelEnrichmentRecord> result = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        List<PageModelEnrichmentFailure> failureDetails = new ArrayList<>();
        int attempts = 0;
        int successes = 0;
        int promptChars = 0;
        int responseChars = 0;
        int actualInputTokens = 0;
        int actualOutputTokens = 0;
        int actualTotalTokens = 0;
        for (int index = 0; index < baseline.size(); index++) {
            PageModelEnrichmentRecord fallback = baseline.get(index);
            String response = "";
            try {
                String prompt = prompt(inputs.get(index), fallback);
                attempts++;
                promptChars += prompt.length();
                response = generationClient.generate(prompt);
                responseChars += response == null ? 0 : response.length();
                var usage = generationClient.lastUsage();
                actualInputTokens += usage.inputTokens();
                actualOutputTokens += usage.outputTokens();
                actualTotalTokens += usage.totalTokens();
                result.add(parse(response, fallback, inputs.get(index)));
                successes++;
            } catch (Exception exception) {
                result.add(fallback);
                failures.add(fallback.pageId() + ": " + safeMessage(exception));
                failureDetails.add(new PageModelEnrichmentFailure(
                        fallback.pageId(),
                        fallback.pageName(),
                        fallback.route(),
                        safeMessage(exception),
                        response
                ));
            }
        }
        lastFailures = List.copyOf(failures);
        lastFailureDetails = List.copyOf(failureDetails);
        lastAttempts = attempts;
        lastSuccesses = successes;
        lastPromptChars = promptChars;
        lastResponseChars = responseChars;
        lastActualInputTokens = actualInputTokens;
        lastActualOutputTokens = actualOutputTokens;
        lastActualTotalTokens = actualTotalTokens;
        return List.copyOf(result);
    }

    public List<String> lastFailures() {
        return lastFailures;
    }

    public List<PageModelEnrichmentFailure> lastFailureDetails() {
        return lastFailureDetails;
    }

    public int lastAttempts() {
        return lastAttempts;
    }

    public int lastSuccesses() {
        return lastSuccesses;
    }

    public int lastPromptChars() {
        return lastPromptChars;
    }

    public int lastResponseChars() {
        return lastResponseChars;
    }

    public int lastActualInputTokens() {
        return lastActualInputTokens;
    }

    public int lastActualOutputTokens() {
        return lastActualOutputTokens;
    }

    public int lastActualTotalTokens() {
        return lastActualTotalTokens;
    }

    private String prompt(PageModelEnrichmentInput input, PageModelEnrichmentRecord baseline) throws Exception {
        return """
                # Runtime Skill Contract
                %s

                # Context
                This is already page-owned mapper evidence selected by current requirements. It is not repository source code.

                # Constraints
                1. Return JSON only.
                2. Do not invent elements, locators, actions, requirements, routes, or assertions.
                3. Keep all lists concise, with at most 5 items.
                4. Preserve pageId, pageName, and route exactly.
                5. Use only ownedRequirementIds, ownedActions, ownedAssertions, allowedLocators, semanticComponents, runtimeEvidence, and knownGaps from input.
                6. Prefer allowedLocators; if evidence is missing, report a risk or coverage gap instead of inventing it.
                7. External navigation targets are outside the application boundary and must not appear as stable locators or supported actions.

                # Input
                %s

                # Expected Output
                Schema version: %s.
                {"schemaVersion":"%s","pageId":"%s","pageName":"%s","route":"%s","businessIntent":"","pageSummary":"","supportedActions":[],"stableLocators":[],"preconditions":[],"postconditions":[],"risks":[],"coverageGaps":[],"requirementTraceability":[],"confidenceScore":0.0}

                # Success Criteria
                The result helps build a focused Page Object API for only the supplied requirements.

                # Notes
                Enrichment is metadata only. Do not generate Java code or tests.
                """.formatted(skillPromptLoader.promptBlock("page-enrichment"),
                objectMapper.writeValueAsString(promptInput(input)),
                LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD,
                LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD,
                input.pageId(), input.pageName(), input.route());
    }

    private Map<String, Object> promptInput(PageModelEnrichmentInput input) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("pageId", input.pageId());
        page.put("pageName", input.pageName());
        page.put("route", input.route());
        page.put("capability", input.capability());
        page.put("title", input.title());
        payload.put("page", page);
        payload.put("ownedRequirementIds", input.requirementRefs());
        payload.put("ownedActions", input.requirementActions().isEmpty() ? input.actions() : input.requirementActions());
        payload.put("ownedAssertions", input.requirementAssertions());
        payload.put("semanticComponents", input.semanticComponents());
        payload.put("allowedLocators", input.stableLocators());
        payload.put("runtimeEvidence", input.runtimeEvidence());
        payload.put("preconditions", input.preconditions());
        payload.put("knownGaps", input.knownGaps());
        return payload;
    }

    private PageModelEnrichmentRecord parse(
            String response,
            PageModelEnrichmentRecord fallback,
            PageModelEnrichmentInput input
    ) throws Exception {
        JsonNode node = objectMapper.readTree(stripFences(response));
        schemaValidator.throwIfInvalid(schemaValidator.validatePageModelEnrichmentRecord(node));
        if (!fallback.pageId().equals(text(node, "pageId", fallback.pageId()))) {
            return fallback;
        }
        return new PageModelEnrichmentRecord(
                fallback.pageId(), fallback.pageName(), fallback.route(),
                text(node, "businessIntent", fallback.businessIntent()),
                text(node, "pageSummary", fallback.pageSummary()),
                list(node, "supportedActions", fallback.supportedActions()),
                allowedList(node, "stableLocators", fallback.stableLocators(), input.stableLocators()),
                list(node, "preconditions", fallback.preconditions()),
                list(node, "postconditions", fallback.postconditions()),
                list(node, "risks", fallback.risks()),
                list(node, "coverageGaps", fallback.coverageGaps()),
                allowedList(node, "requirementTraceability", fallback.requirementTraceability(), input.requirementRefs()),
                fallback.actionsByRequirement(),
                fallback.postconditionsByRequirement(),
                node.path("confidenceScore").isNumber() ? node.path("confidenceScore").asDouble() : fallback.confidenceScore(),
                "openai"
        );
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private List<String> list(JsonNode node, String field, List<String> fallback) {
        if (!node.path(field).isArray()) {
            return fallback;
        }
        List<String> values = new ArrayList<>();
        node.path(field).forEach(item -> {
            String value = item.asText("").trim();
            if (!value.isBlank()) {
                values.add(value);
            }
        });
        return values.isEmpty() ? fallback : values;
    }

    private List<String> allowedList(
            JsonNode node,
            String field,
            List<String> fallback,
            List<String> allowedValues
    ) {
        List<String> values = list(node, field, List.of());
        if (values.isEmpty()) {
            return fallback;
        }
        List<String> allowed = allowedValues == null ? List.of() : allowedValues;
        List<String> filtered = values.stream().filter(allowed::contains).toList();
        return filtered.isEmpty() ? fallback : filtered;
    }

    private String stripFences(String response) {
        String value = response == null ? "" : response.trim();
        return value.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    private String safeMessage(Exception exception) {
        String message = exception == null ? "unknown failure" : exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message.trim();
    }
}
