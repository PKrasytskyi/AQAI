package ua.demo.agentlab.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

/** Adds terminal demo evidence to the compact run summary without reinterpreting UI evidence. */
public final class BuildWeekRunSummaryProjector {

    private final Path summaryPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public BuildWeekRunSummaryProjector() {
        this(Path.of("target", "ai-run", "run-summary.json"));
    }

    public BuildWeekRunSummaryProjector(Path summaryPath) {
        this.summaryPath = summaryPath;
    }

    public void project(BuildWeekDemoSummary summary) {
        if (summary == null) return;
        try {
            Files.createDirectories(summaryPath.toAbsolutePath().normalize().getParent());
            ObjectNode root = readRoot();
            root.set("buildWeekDemo", mapper.valueToTree(summary));
            root.put("generatedTestExecutionStatus", summary.generatedTestExecutionStatus());
            root.put("generatedTestsExecuted", summary.executedTests());
            root.put("generatedTestsPassed", summary.passedTests());
            if (!summary.passed()) {
                root.put("qualityScore", Math.min(root.path("qualityScore").asInt(85), 85));
                root.put("terminalFailure", "BUILD_WEEK_DEMO_ACCEPTANCE");
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(summaryPath.toFile(), root);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to project Build Week result into run summary", exception);
        }
    }

    private ObjectNode readRoot() throws Exception {
        if (!Files.isRegularFile(summaryPath)) return mapper.createObjectNode();
        JsonNode existing = mapper.readTree(summaryPath.toFile());
        return existing instanceof ObjectNode object ? object : mapper.createObjectNode();
    }
}
