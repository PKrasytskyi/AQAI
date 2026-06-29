package ua.demo.agentlab.futurefeat.testplan.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.debug.AiRunArtifactWriter;
import ua.demo.agentlab.ai.openai.OpenAiClientFactory;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfigRagAdapter;
import ua.demo.agentlab.ai.openai.PropertiesOpenAiRuntimeConfig;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmptyOutputException;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.futurefeat.testplan.generator.RuleBasedTestPlanGenerator;
import ua.demo.agentlab.futurefeat.testplan.generator.TestPlanGenerator;
import ua.demo.agentlab.futurefeat.testplan.model.FunctionalArea;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.futurefeat.testplan.model.TestPriority;
import ua.demo.agentlab.futurefeat.testplan.model.TestScenario;
import ua.demo.agentlab.futurefeat.testplan.model.TestType;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class OpenAiTestPlanGenerator implements TestPlanGenerator, AutoCloseable {

    private static final int MAX_REQUIREMENTS_PER_REQUEST = 3;

    private final OpenAiRuntimeConfig runtimeConfig;
    private final TestPlanGenerator fallbackGenerator;
    private final OpenAiResponseGenerationClient generationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiRunArtifactWriter artifactWriter = new AiRunArtifactWriter();

    public OpenAiTestPlanGenerator() {
        this(new PropertiesOpenAiRuntimeConfig(), new RuleBasedTestPlanGenerator());
    }

    public OpenAiTestPlanGenerator(
            OpenAiRuntimeConfig runtimeConfig,
            TestPlanGenerator fallbackGenerator
    ) {
        if (runtimeConfig == null) {
            throw new IllegalArgumentException("runtimeConfig cannot be null");
        }
        if (fallbackGenerator == null) {
            throw new IllegalArgumentException("fallbackGenerator cannot be null");
        }
        this.runtimeConfig = runtimeConfig;
        this.fallbackGenerator = fallbackGenerator;
        this.generationClient = new OpenAiResponseGenerationClient(new OpenAiRuntimeConfigRagAdapter(runtimeConfig));
    }

    @Override
    public TestPlan generate(NormalizedRequirementBundle bundle, WorkflowState state) {
        TestPlan fallbackPlan = fallbackGenerator.generate(bundle, state);
        if (state != null) {
            state.addArtifact("openai.enabled", "true");
            state.addArtifact("openai.model", runtimeConfig.model());
            state.addArtifact("openai.base-url", runtimeConfig.baseUrl());
            state.addArtifact("openai.strict", String.valueOf(runtimeConfig.strict()));
            state.addArtifact("openai.mode", runtimeConfig.assistiveOnly() ? "assistive-only" : "full");
        }

        recordPromptOnly(bundle, state, fallbackPlan);
        if (state != null) {
            state.addArtifact("openai.status", "llm-disabled-enrichment-only");
            state.addArtifact("openai.test.plan.scenario.count", String.valueOf(fallbackPlan.scenarios().size()));
            state.addArtifact("openai.test.plan.chunked", String.valueOf(requiresChunking(bundle)));
            state.addFinding("OpenAI test plan generation is disabled; prompts were recorded and fallback planner used");
        }
        return fallbackPlan;
    }

    @Override
    public void close() {
    }

    private String buildPrompt(NormalizedRequirementBundle bundle, WorkflowState state, TestPlan fallbackPlan) {
        String objective = state == null || state.getObjective() == null || state.getObjective().isBlank()
                ? fallbackPlan.objective()
                : state.getObjective();

        StringBuilder requirementsBlock = new StringBuilder();
        for (NormalizedRequirement requirement : bundle.requirements()) {
            requirementsBlock.append("- id: ").append(nullSafe(requirement.id())).append(System.lineSeparator());
            requirementsBlock.append("  title: ").append(nullSafe(requirement.title())).append(System.lineSeparator());
            requirementsBlock.append("  statement: ").append(nullSafe(requirement.statement())).append(System.lineSeparator());
            requirementsBlock.append("  expectedResult: ").append(nullSafe(requirement.expectedResult())).append(System.lineSeparator());
            requirementsBlock.append("  uiRelevant: ").append(requirement.uiRelevant()).append(System.lineSeparator());
            requirementsBlock.append("  apiRelevant: ").append(requirement.apiRelevant()).append(System.lineSeparator());
            requirementsBlock.append("  tags: ").append(requirement.tags() == null ? List.of() : requirement.tags()).append(System.lineSeparator());
            requirementsBlock.append("  sourceReference: ").append(toSourceReference(requirement.sourceReference())).append(System.lineSeparator());
        }

        return """
                You are a senior QA architect and test planner.
                Generate a high-quality test plan from normalized requirements.

                Rules:
                - Return JSON only.
                - Do not wrap JSON in markdown fences.
                - Avoid duplicate scenarios.
                - Keep scenarios concrete and requirement-driven.
                - Keep the response compact and complete.
                - Prefer distinct positive, negative, edge, validation, and API scenarios only when justified by the requirements.
                - Generate only the scenario list for this request.
                - Do not generate functional areas, assumptions, or risks in the response.
                - Every scenario must include: id, title, type, priority, precondition, steps, expectedResult, sourceReference.
                - Valid type values: POSITIVE, NEGATIVE, EDGE, VALIDATION, API
                - Valid priority values: HIGH, MEDIUM, LOW
                - Keep 2-5 concise steps per scenario.
                - Keep the total number of scenarios small: usually 1 scenario per requirement, at most 2 when an API or negative case is clearly required.
                - sourceReference should point to the most relevant requirement source line or file reference.

                Objective:
                %s

                Source:
                %s

                Existing assumptions from normalization:
                %s

                Existing risks from normalization:
                %s

                Normalized requirements:
                %s
                Return JSON with this exact shape:
                {
                  "scenarios": [
                    {
                      "id": "TP-001-P",
                      "title": "Verify valid behavior for ...",
                      "type": "POSITIVE",
                      "priority": "HIGH",
                      "precondition": "...",
                      "steps": ["...", "..."],
                      "expectedResult": "...",
                      "sourceReference": "requirements/file.md [L3]"
                    }
                  ]
                }
                """.formatted(
                objective,
                bundle.source(),
                bundle.assumptions(),
                bundle.risks(),
                requirementsBlock
        );
    }

    private TestPlan parseGeneratedPlan(
            String rawResponse,
            NormalizedRequirementBundle bundle,
            WorkflowState state,
            TestPlan fallbackPlan
    ) throws IOException {
        String json = extractJsonObject(rawResponse);
        JsonNode root = objectMapper.readTree(json);

        List<TestScenario> scenarios = parseScenarios(root.path("scenarios"), fallbackPlan.scenarios(), bundle);
        if (scenarios.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no usable scenarios");
        }

        String objective = state != null && state.getObjective() != null && !state.getObjective().isBlank()
                ? state.getObjective()
                : fallbackPlan.objective();

        return new TestPlan(
                objective,
                bundle.source(),
                fallbackPlan.functionalAreas(),
                scenarios,
                fallbackPlan.assumptions(),
                fallbackPlan.risks()
        );
    }

    private List<FunctionalArea> parseFunctionalAreas(JsonNode node, List<FunctionalArea> fallbackAreas) {
        List<FunctionalArea> areas = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String name = text(item, "name");
                if (name.isBlank()) {
                    continue;
                }
                areas.add(new FunctionalArea(name, defaultIfBlank(text(item, "description"), "Generated by OpenAI")));
            }
        }
        return areas.isEmpty() ? fallbackAreas : deduplicateAreas(areas);
    }

    private List<TestScenario> parseScenarios(
            JsonNode node,
            List<TestScenario> fallbackScenarios,
            NormalizedRequirementBundle bundle
    ) {
        List<TestScenario> scenarios = new ArrayList<>();
        if (node != null && node.isArray()) {
            int index = 1;
            for (JsonNode item : node) {
                String title = text(item, "title");
                if (title.isBlank()) {
                    continue;
                }
                String id = defaultIfBlank(text(item, "id"), "AI-TP-" + String.format("%03d", index));
                List<String> steps = parseSteps(item.path("steps"));
                if (steps.isEmpty()) {
                    steps = List.of(
                            "Prepare the required test data",
                            "Execute the target business action",
                            "Observe the system response"
                    );
                }
                scenarios.add(new TestScenario(
                        id,
                        title,
                        parseType(text(item, "type"), title),
                        parsePriority(text(item, "priority")),
                        defaultIfBlank(text(item, "precondition"), "Application is available"),
                        steps,
                        defaultIfBlank(text(item, "expectedResult"), "System behaves according to the requirement"),
                        defaultIfBlank(text(item, "sourceReference"), defaultSourceReference(bundle))
                ));
                index++;
            }
        }

        List<TestScenario> deduplicated = deduplicateScenarios(scenarios);
        return deduplicated.isEmpty() ? fallbackScenarios : deduplicated;
    }

    private List<String> parseStringArray(JsonNode node, List<String> fallback) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item != null && item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText().trim());
                }
            }
        }
        return values.isEmpty() ? fallback : List.copyOf(values);
    }

    private List<String> parseSteps(JsonNode node) {
        List<String> steps = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item != null && item.isTextual() && !item.asText().isBlank()) {
                    steps.add(item.asText().trim());
                }
            }
        }
        return List.copyOf(steps);
    }

    private List<FunctionalArea> deduplicateAreas(List<FunctionalArea> areas) {
        List<FunctionalArea> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (FunctionalArea area : areas) {
            String key = area.name().trim().toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(area);
            }
        }
        return result;
    }

    private List<TestScenario> deduplicateScenarios(List<TestScenario> scenarios) {
        List<TestScenario> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (TestScenario scenario : scenarios) {
            String key = scenario.title().trim().toLowerCase(Locale.ROOT)
                    + "|" + scenario.type().name()
                    + "|" + scenario.expectedResult().trim().toLowerCase(Locale.ROOT);
            if (seen.add(key)) {
                result.add(scenario);
            }
        }
        return result;
    }

    private TestType parseType(String rawType, String title) {
        String normalized = nullSafe(rawType).toUpperCase(Locale.ROOT);
        try {
            if (!normalized.isBlank()) {
                return TestType.valueOf(normalized);
            }
        } catch (IllegalArgumentException ignored) {
        }

        String lowerTitle = nullSafe(title).toLowerCase(Locale.ROOT);
        if (containsAny(lowerTitle, "invalid", "negative", "restricted", "reject")) {
            return TestType.NEGATIVE;
        }
        if (containsAny(lowerTitle, "boundary", "edge")) {
            return TestType.EDGE;
        }
        if (containsAny(lowerTitle, "validation")) {
            return TestType.VALIDATION;
        }
        if (containsAny(lowerTitle, "api", "endpoint", "contract")) {
            return TestType.API;
        }
        return TestType.POSITIVE;
    }

    private TestPriority parsePriority(String rawPriority) {
        String normalized = nullSafe(rawPriority).toUpperCase(Locale.ROOT);
        try {
            if (!normalized.isBlank()) {
                return TestPriority.valueOf(normalized);
            }
        } catch (IllegalArgumentException ignored) {
        }
        return TestPriority.MEDIUM;
    }

    private String extractJsonObject(String rawResponse) {
        String text = nullSafe(rawResponse).trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }

        int start = text.indexOf('{');
        if (start < 0) {
            throw new IllegalStateException("OpenAI response does not contain a JSON object");
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
                continue;
            }

            if (current == '"') {
                inString = true;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, index + 1);
                }
            }
        }

        throw new IllegalStateException("OpenAI response contains incomplete JSON");
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || fieldName.isBlank()) {
            return "";
        }
        JsonNode field = node.path(fieldName);
        return field.isTextual() ? field.asText().trim() : "";
    }

    private String defaultSourceReference(NormalizedRequirementBundle bundle) {
        if (bundle.requirements() == null || bundle.requirements().isEmpty()) {
            return bundle.source();
        }
        return toSourceReference(bundle.requirements().get(0).sourceReference());
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
        return nullSafe(reference.source());
    }

    private boolean containsAny(String text, String... fragments) {
        String normalized = nullSafe(text).toLowerCase(Locale.ROOT);
        for (String fragment : fragments) {
            if (normalized.contains(fragment.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private boolean requiresChunking(NormalizedRequirementBundle bundle) {
        return bundle != null
                && bundle.requirements() != null
                && bundle.requirements().size() > MAX_REQUIREMENTS_PER_REQUEST;
    }

    private TestPlan generateSingleRequestTestPlan(
            NormalizedRequirementBundle bundle,
            WorkflowState state,
            TestPlan fallbackPlan,
            String artifactStage
    ) throws IOException {
        String prompt = buildPrompt(bundle, state, fallbackPlan);
        trackTextArtifact(state, artifactStage, "prompt.txt", prompt);
        trackTextArtifact(
                state,
                artifactStage,
                "raw-response-disabled.txt",
                "LLM test plan generation is disabled. Prompt was recorded for review only."
        );
        return fallbackPlan;
    }

    private void recordPromptOnly(NormalizedRequirementBundle bundle, WorkflowState state, TestPlan fallbackPlan) {
        if (requiresChunking(bundle)) {
            List<NormalizedRequirement> requirements = bundle.requirements();
            for (int start = 0, chunkIndex = 1; start < requirements.size(); start += MAX_REQUIREMENTS_PER_REQUEST, chunkIndex++) {
                int end = Math.min(start + MAX_REQUIREMENTS_PER_REQUEST, requirements.size());
                NormalizedRequirementBundle chunkBundle = new NormalizedRequirementBundle(
                        bundle.source(),
                        List.copyOf(requirements.subList(start, end)),
                        bundle.assumptions(),
                        bundle.risks()
                );
                TestPlan chunkFallbackPlan = fallbackGenerator.generate(chunkBundle, state);
                trackTextArtifact(state, "test-plan/chunk-%02d".formatted(chunkIndex), "prompt.txt",
                        buildPrompt(chunkBundle, state, chunkFallbackPlan));
            }
            return;
        }
        trackTextArtifact(state, "test-plan", "prompt.txt", buildPrompt(bundle, state, fallbackPlan));
    }

    private TestPlan generateChunkedTestPlan(NormalizedRequirementBundle bundle, WorkflowState state) throws IOException {
        List<TestPlan> partialPlans = new ArrayList<>();
        List<NormalizedRequirement> requirements = bundle.requirements();

        for (int start = 0, chunkIndex = 1; start < requirements.size(); start += MAX_REQUIREMENTS_PER_REQUEST, chunkIndex++) {
            int end = Math.min(start + MAX_REQUIREMENTS_PER_REQUEST, requirements.size());
            List<NormalizedRequirement> chunkRequirements = requirements.subList(start, end);
            NormalizedRequirementBundle chunkBundle = new NormalizedRequirementBundle(
                    bundle.source(),
                    List.copyOf(chunkRequirements),
                    bundle.assumptions(),
                    bundle.risks()
            );
            TestPlan chunkFallbackPlan = fallbackGenerator.generate(chunkBundle, state);
            partialPlans.add(generateSingleRequestTestPlan(
                    chunkBundle,
                    state,
                    chunkFallbackPlan,
                    "test-plan/chunk-%02d".formatted(chunkIndex)
            ));
        }

        TestPlan mergedPlan = mergePartialPlans(bundle, state, partialPlans);
        trackJsonArtifact(state, "test-plan", "merged-test-plan.json", mergedPlan);
        return mergedPlan;
    }

    private TestPlan mergePartialPlans(
            NormalizedRequirementBundle bundle,
            WorkflowState state,
            List<TestPlan> partialPlans
    ) {
        List<FunctionalArea> functionalAreas = new ArrayList<>();
        List<TestScenario> scenarios = new ArrayList<>();
        Set<String> assumptions = new LinkedHashSet<>();
        Set<String> risks = new LinkedHashSet<>();

        for (TestPlan partialPlan : partialPlans) {
            functionalAreas.addAll(partialPlan.functionalAreas());
            scenarios.addAll(partialPlan.scenarios());
            assumptions.addAll(partialPlan.assumptions());
            risks.addAll(partialPlan.risks());
        }

        List<FunctionalArea> deduplicatedAreas = deduplicateAreas(functionalAreas);
        List<TestScenario> deduplicatedScenarios = renumberScenarios(deduplicateScenarios(scenarios));
        String objective = state != null && state.getObjective() != null && !state.getObjective().isBlank()
                ? state.getObjective()
                : "Generated test plan";

        return new TestPlan(
                objective,
                bundle.source(),
                deduplicatedAreas,
                deduplicatedScenarios,
                List.copyOf(assumptions),
                List.copyOf(risks)
        );
    }

    private List<TestScenario> renumberScenarios(List<TestScenario> scenarios) {
        List<TestScenario> renumbered = new ArrayList<>();
        int index = 1;
        for (TestScenario scenario : scenarios) {
            String suffix = switch (scenario.type()) {
                case POSITIVE -> "P";
                case NEGATIVE -> "N";
                case EDGE -> "E";
                case VALIDATION -> "V";
                case API -> "A";
            };
            renumbered.add(new TestScenario(
                    "TP-%03d-%s".formatted(index, suffix),
                    scenario.title(),
                    scenario.type(),
                    scenario.priority(),
                    scenario.precondition(),
                    scenario.steps(),
                    scenario.expectedResult(),
                    scenario.sourceReference()
            ));
            index++;
        }
        return renumbered;
    }

    private void trackTextArtifact(WorkflowState state, String stage, String fileName, String content) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeText(stage, fileName, content).toString());
    }

    private void trackJsonArtifact(WorkflowState state, String stage, String fileName, Object payload) {
        if (state == null) {
            return;
        }
        state.addAiArtifactFile(artifactWriter.writeJson(stage, fileName, payload).toString());
    }

    private IllegalStateException strictFailure(String message, Exception cause) {
        return cause instanceof IllegalStateException illegalStateException
                ? new IllegalStateException(message + ": " + illegalStateException.getMessage(), illegalStateException)
                : new IllegalStateException(message, cause);
    }
}
