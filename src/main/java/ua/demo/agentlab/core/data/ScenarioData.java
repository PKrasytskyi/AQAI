package ua.demo.agentlab.core.data;

import java.util.Map;
import java.util.Objects;

public record ScenarioData(Map<String, String> values) {

    public String required(String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required scenario data key: " + key);
        }
        return value;
    }

    public String optional(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public int requiredInt(String key) {
        return Integer.parseInt(required(key));
    }

    public static ScenarioData of(Map<String, String> values) {
        return new ScenarioData(Map.copyOf(Objects.requireNonNull(values)));
    }
}
