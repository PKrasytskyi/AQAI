package ua.demo.agentlab.ui.discovery.evidence.funnel;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Loads project-specific acceptance metadata without leaking it into discovery rules. */
public final class SpaAcceptanceFixtureLoader {

    public SpaAcceptanceFixture load(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("SPA acceptance fixture does not exist: " + path);
        }
        try (InputStream input = Files.newInputStream(path)) {
            return parse(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read SPA acceptance fixture: " + path, exception);
        }
    }

    public SpaAcceptanceFixture loadResource(String resourcePath) {
        String normalized = resourcePath == null ? "" : resourcePath.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("resourcePath cannot be blank");
        }
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized)) {
            if (input == null) {
                throw new IllegalArgumentException("SPA acceptance fixture resource does not exist: " + normalized);
            }
            return parse(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot close SPA acceptance fixture resource: " + normalized, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private SpaAcceptanceFixture parse(InputStream input) {
        Object loaded = new Yaml().load(input);
        if (!(loaded instanceof Map<?, ?> raw)) {
            throw new IllegalArgumentException("SPA acceptance fixture must be a YAML object");
        }
        Map<String, Object> values = (Map<String, Object>) raw;
        return new SpaAcceptanceFixture(
                text(values.get("fixtureId")),
                text(values.get("projectProfile")),
                text(values.get("requirementsFile")),
                capabilities(values.get("flowCapabilities")),
                capabilities(values.get("supportedCapabilityContracts"))
        );
    }

    private List<SpaAcceptanceCapability> capabilities(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .map(String::valueOf)
                .map(item -> SpaAcceptanceCapability.from(item)
                        .orElseThrow(() -> new IllegalArgumentException("Unsupported SPA acceptance capability: " + item)))
                .toList();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
