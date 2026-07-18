package ua.demo.agentlab.demo.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Compares golden artifacts after removing run identity and ordering noise. */
public final class NormalizedArtifactRegressionGate {

    private static final Set<String> RUN_FIELDS = Set.of(
            "runId", "createdAt", "timestamp", "discoverySessionId", "elapsedMillis", "durationMillis");
    private final ObjectMapper mapper;

    public NormalizedArtifactRegressionGate() {
        this(new ObjectMapper());
    }

    NormalizedArtifactRegressionGate(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public RegressionResult compare(Path expected, Path actual) {
        if (expected == null || actual == null) {
            throw new IllegalArgumentException("Expected and actual artifact paths are required");
        }
        try {
            String expectedValue = normalize(expected);
            String actualValue = normalize(actual);
            return new RegressionResult(expectedValue.equals(actualValue), expected, actual,
                    expectedValue.equals(actualValue) ? "" : "Normalized artifact content changed");
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to compare normalized artifacts", exception);
        }
    }

    public String normalize(Path artifact) throws IOException {
        String content = Files.readString(artifact);
        if (!artifact.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json")) {
            return content.replace("\r\n", "\n").strip();
        }
        return mapper.writeValueAsString(canonicalize(mapper.readTree(content)));
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isValueNode()) return node;
        if (node.isObject()) {
            ObjectNode result = mapper.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.stream().filter(name -> !RUN_FIELDS.contains(name)).sorted()
                    .forEach(name -> result.set(name, canonicalize(node.get(name))));
            return result;
        }
        ArrayNode result = mapper.createArrayNode();
        node.elements().forEachRemaining(value -> result.add(canonicalize(value)));
        return result;
    }

    public record RegressionResult(boolean passed, Path expected, Path actual, String reason) {
    }
}
