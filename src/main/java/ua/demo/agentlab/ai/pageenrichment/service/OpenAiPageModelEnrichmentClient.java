package ua.demo.agentlab.ai.pageenrichment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentInput;
import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaValidator;
import ua.demo.agentlab.ai.schema.LlmOutputSchemaVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiPageModelEnrichmentClient implements PageModelEnrichmentClient {

    private final PageModelEnrichmentClient baselineClient;
    private final OpenAiResponseGenerationClient generationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmOutputSchemaValidator schemaValidator = new LlmOutputSchemaValidator();
    private List<String> lastFailures = List.of();

    public OpenAiPageModelEnrichmentClient(RagRuntimeConfig config) {
        this(config, new RuleBasedPageModelEnrichmentClient());
    }

    OpenAiPageModelEnrichmentClient(RagRuntimeConfig config, PageModelEnrichmentClient baselineClient) {
        this.generationClient = new OpenAiResponseGenerationClient(config);
        this.baselineClient = baselineClient == null ? new RuleBasedPageModelEnrichmentClient() : baselineClient;
    }

    @Override
    public List<PageModelEnrichmentRecord> enrich(List<PageModelEnrichmentInput> inputs) {
        List<PageModelEnrichmentRecord> baseline = baselineClient.enrich(inputs);
        List<PageModelEnrichmentRecord> result = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (int index = 0; index < baseline.size(); index++) {
            PageModelEnrichmentRecord fallback = baseline.get(index);
            try {
                result.add(parse(generationClient.generate(prompt(inputs.get(index), fallback)), fallback, inputs.get(index)));
            } catch (Exception exception) {
                result.add(fallback);
                failures.add(fallback.pageId() + ": " + safeMessage(exception));
            }
        }
        lastFailures = List.copyOf(failures);
        return List.copyOf(result);
    }

    public List<String> lastFailures() {
        return lastFailures;
    }

    private String prompt(PageModelEnrichmentInput input, PageModelEnrichmentRecord baseline) throws Exception {
        return """
                # Goal
                Enrich exactly one discovered UI page model for a Selenium Page Object prompt.

                # Context
                This is mapper evidence for one page selected by current requirements. It is not repository source code.

                # Constraints
                1. Return JSON only.
                2. Do not invent elements, locators, actions, requirements, routes, or assertions.
                3. Keep all lists concise, with at most 5 items.
                4. Preserve pageId, pageName, and route exactly.
                5. Prefer mapper locator evidence; flag missing evidence as a risk.
                6. External navigation targets are outside the application boundary and must not appear as stable locators or supported actions.

                # Input
                %s

                # Expected Output
                Schema version: %s.
                {"schemaVersion":"%s","pageId":"%s","pageName":"%s","route":"%s","businessIntent":"","pageSummary":"","supportedActions":[],"stableLocators":[],"preconditions":[],"postconditions":[],"risks":[],"coverageGaps":[],"requirementTraceability":[],"confidenceScore":0.0}

                # Success Criteria
                The result helps build a focused Page Object API for only the supplied requirements.

                # Notes
                Enrichment is metadata only. Do not generate Java code or tests.
                """.formatted(objectMapper.writeValueAsString(Map.of("page", input, "baseline", baseline)),
                LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD,
                LlmOutputSchemaVersion.PAGE_MODEL_ENRICHMENT_RECORD,
                input.pageId(), input.pageName(), input.route());
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
