package ua.demo.agentlab.artifactreuse.flow;

import ua.demo.agentlab.artifactreuse.fingerprint.CanonicalArtifactHasher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Produces a stable identity for reusable flow behaviour, independent of requirement wording. */
public class FlowContractFingerprintBuilder {

    private final CanonicalArtifactHasher hasher;

    public FlowContractFingerprintBuilder() {
        this(new CanonicalArtifactHasher());
    }

    FlowContractFingerprintBuilder(CanonicalArtifactHasher hasher) {
        if (hasher == null) {
            throw new IllegalArgumentException("canonical hasher cannot be null");
        }
        this.hasher = hasher;
    }

    public String build(
            String schemaVersion,
            FlowContractType type,
            FlowEndpoint source,
            FlowEndpoint target,
            List<FlowState> requiresStates,
            List<FlowState> producesStates,
            List<FlowContractStep> steps,
            List<String> assertions
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("schemaVersion", schemaVersion);
        input.put("type", type == null ? "GENERIC" : type.name());
        input.put("source", endpoint(source));
        input.put("target", endpoint(target));
        input.put("requiresStates", requiresStates == null ? List.of() : requiresStates.stream().map(Enum::name).toList());
        input.put("producesStates", producesStates == null ? List.of() : producesStates.stream().map(Enum::name).toList());
        input.put("steps", steps == null ? List.of() : steps.stream().map(step -> Map.of(
                "action", step.action().name(), "ownerPage", step.ownerPage(), "route", step.route(),
                "dataKey", step.dataKey(), "setup", step.setup())).toList());
        input.put("assertions", assertions == null ? List.of() : assertions);
        return hasher.hash(input).value();
    }

    private Map<String, Object> endpoint(FlowEndpoint endpoint) {
        if (endpoint == null) return Map.of();
        return Map.of("pageName", endpoint.pageName(), "route", endpoint.route());
    }
}
