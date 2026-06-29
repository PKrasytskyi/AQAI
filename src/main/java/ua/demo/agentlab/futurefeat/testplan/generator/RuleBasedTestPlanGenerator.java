package ua.demo.agentlab.futurefeat.testplan.generator;

import ua.demo.agentlab.futurefeat.testplan.model.FunctionalArea;
import ua.demo.agentlab.futurefeat.testplan.model.TestPlan;
import ua.demo.agentlab.futurefeat.testplan.model.TestPriority;
import ua.demo.agentlab.futurefeat.testplan.model.TestScenario;
import ua.demo.agentlab.futurefeat.testplan.model.TestType;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirement;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;

import ua.demo.agentlab.requirements.normalization.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RuleBasedTestPlanGenerator implements TestPlanGenerator {

    @Override
    public TestPlan generate(NormalizedRequirementBundle bundle, WorkflowState state) {

        List<FunctionalArea> areas = extractFunctionalAreas(bundle);
        List<TestScenario> scenarios = buildScenarios(bundle.requirements());
        List<String> assumptions = buildAssumptions(bundle);
        List<String> risks = buildRisks(bundle);

        return new TestPlan(
                state.getObjective(),
                bundle.source(),
                areas,
                scenarios,
                assumptions,
                risks
        );
    }

    private List<FunctionalArea> extractFunctionalAreas(NormalizedRequirementBundle bundle) {
        Map<String, FunctionalArea> uniqueAreas = new LinkedHashMap<>();

        for(NormalizedRequirement requirement : bundle.requirements()){
            for(String tag : requirement.tags()){
                String normalizedTag = normalizeTag(tag);
                uniqueAreas.putIfAbsent(
                        normalizedTag,
                        new FunctionalArea(
                                toAreaName(normalizedTag),
                                "Derived from normalized requirement tag"
                        )
                );
            }
        }

        if(uniqueAreas.isEmpty()){
            uniqueAreas.put("general",
                    new FunctionalArea("General Requirements", "Fallback area when no tags are present"));
        }

        return new ArrayList<>(uniqueAreas.values());
    }

    private List<TestScenario> buildScenarios(List<NormalizedRequirement> requirements) {
        List<TestScenario> scenarios = new ArrayList<>();
        int counter = 1;

        for (NormalizedRequirement requirement : requirements) {
            String idBase = "TP-" + String.format("%03d", counter++);
            scenarios.addAll(buildScenarioSet(idBase, requirement));
        }
        return scenarios;
    }

    private List<TestScenario> buildScenarioSet(String idBase, NormalizedRequirement requirement) {
        List<TestScenario> scenarios = new ArrayList<>();
        String sourceReference = toSourceReference(requirement.sourceReference());

        scenarios.add(new TestScenario(
                idBase + "-P",
                "Verify valid behavior for: " + requirement.title(),
                TestType.POSITIVE,
                TestPriority.HIGH,
                "Application is available and the user is on the target flow",
                List.of(
                        "Prepare valid input data",
                        "Execute the business action described in the requirement",
                        "Observe the system response"
                ),
                "System completes the expected behavior successfully",
                sourceReference
        ));

        scenarios.add(new TestScenario(
                idBase + "-N",
                "Verify invalid or restricted behavior for: " + requirement.title(),
                TestType.NEGATIVE,
                TestPriority.MEDIUM,
                "Application is available",
                List.of(
                        "Prepare invalid, incomplete, or restricted input",
                        "Execute the same business action",
                        "Observe validation or rejection behavior"
                ),
                "System rejects the action with correct validation or error handling",
                sourceReference
        ));

        scenarios.add(new TestScenario(
                idBase + "-E",
                "Verify boundary conditions for: " + requirement.title(),
                TestType.EDGE,
                TestPriority.MEDIUM,
                "Application is available",
                List.of(
                        "Prepare edge-case input data",
                        "Execute the same business action",
                        "Observe boundary behavior"
                ),
                "System handles the boundary case without crash or inconsistent state",
                sourceReference
        ));

        if(looksLikeApiRequirement(requirement)){
            scenarios.add(new TestScenario(
                    idBase + "-A",
                    "Verify API contract for: " + requirement.title(),
                    TestType.API,
                    TestPriority.HIGH,
                    "API is reachable and test credentials are available",
                    List.of(
                            "Prepare request data for the target endpoint",
                            "Send the API request",
                            "Validate response status, payload, structure, and key fields"
                    ),
                    "API responds according to the requirement and contract expectations",
                    sourceReference
            ));
        }

        return scenarios;
    }

    private List<String> buildAssumptions(NormalizedRequirementBundle bundle) {
        List<String> assumptions = new ArrayList<>(bundle.assumptions());

        if(assumptions.isEmpty()){
            assumptions.add("Normalization produced no explicit assumptions");
        }

        return assumptions;
    }

    private List<String> buildRisks(NormalizedRequirementBundle bundle) {
        List<String> risks = new ArrayList<>(bundle.risks());

        if (risks.isEmpty()) {
            risks.add("Normalized requirements may still omit edge cases or business constraints");
        }

        return risks;
    }

    private boolean looksLikeApiRequirement(NormalizedRequirement requirement){
        if(requirement.apiRelevant()){
            return true;
        }

        String lower = requirement.statement().toLowerCase(Locale.ROOT);
        return lower.contains("api") || lower.contains("endpoint")
                || lower.contains("request") || lower.contains("response")
                || lower.contains("status code") || lower.contains("payload")
                || lower.contains("token");
    }

    private String toSourceReference(SourceReference reference){
        if(reference == null){
            return "Unknown source reference";
        }

        if(reference.startLine() == reference.endLine()){
            return "%s [L%d]".formatted(reference.source(), reference.startLine());
        }

        return "%s [L%d-L%d]".formatted(
                reference.source(),
                reference.startLine(),
                reference.endLine()
        );
    }

    private String normalizeTag(String tag){
        if(tag == null || tag.isBlank()){
            return "general";
        }

        return tag.trim().toLowerCase();
    }

    private String toAreaName(String tag){
        return switch (tag) {
            case "auth" -> "Authentication";
            case "validation" -> "Validation";
            case "performance" -> "Performance";
            case "api" -> "API";
            case "ui" -> "UI";
            default -> capitalize(tag);
        };
    }

    private String capitalize(String value){
        if(value == null || value.isBlank()){
            return "General";
        }

        String normalized = value.trim().toLowerCase();
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
