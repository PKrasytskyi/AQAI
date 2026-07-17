package ua.demo.agentlab.demo;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DemoManifestLoader {

    public DemoManifest load(Path manifestPath) {
        if (manifestPath == null || !Files.isRegularFile(manifestPath)) {
            throw new IllegalArgumentException("Demo manifest does not exist: " + manifestPath);
        }
        try {
            Object loaded = new Yaml().load(Files.readString(manifestPath, StandardCharsets.UTF_8));
            if (!(loaded instanceof Map<?, ?> root)) {
                throw new IllegalArgumentException("Demo manifest root must be a YAML object: " + manifestPath);
            }
            Map<?, ?> demo = map(root.get("demo"));
            Map<?, ?> inputs = map(root.get("inputs"));
            Map<?, ?> expected = map(root.get("expected"));
            Map<?, ?> runtime = map(root.get("runtime"));
            return new DemoManifest(
                    text(root.get("schemaVersion")),
                    text(demo.get("id")),
                    text(inputs.get("projectProfile")),
                    text(inputs.get("requirementFixture")),
                    strings(inputs.get("requiredEnvironment")),
                    strings(expected.get("pageCapabilities")),
                    strings(expected.get("scenarioIds")),
                    strings(expected.get("pomNames")),
                    text(expected.get("finalRoute")),
                    text(expected.get("finalState")),
                    text(runtime.get("databaseMode")),
                    text(runtime.get("aiMode")),
                    stringMap(root.get("schemas"))
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read demo manifest: " + manifestPath, exception);
        }
    }

    private Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> result ? result : Map.of();
    }

    private List<String> strings(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream().map(this::text).filter(item -> !item.isBlank()).toList();
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> values)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((key, item) -> {
            String normalizedKey = text(key);
            String normalizedValue = text(item);
            if (!normalizedKey.isBlank() && !normalizedValue.isBlank()) {
                result.put(normalizedKey, normalizedValue);
            }
        });
        return Map.copyOf(result);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
