package ua.demo.agentlab.orchestration.pipeline;

import java.util.LinkedHashMap;
import java.util.Map;

public class ArtifactRefs {

    private final Map<String, String> values;

    public ArtifactRefs() {
        this(new LinkedHashMap<>());
    }

    private ArtifactRefs(Map<String, String> values) {
        this.values = values;
    }

    public static ArtifactRefs from(Map<String, String> values) {
        return new ArtifactRefs(values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values));
    }

    public void put(String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        values.put(key, value);
    }

    public Map<String, String> asMap() {
        return values;
    }

    public Map<String, String> getValues() {
        return Map.copyOf(values);
    }

    public ArtifactRefs snapshot() {
        return new ArtifactRefs(new LinkedHashMap<>(values));
    }
}
