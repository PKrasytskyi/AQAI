package ua.demo.agentlab.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class RuntimeProperties {

    private static final String DEFAULT_RESOURCE = "framework.properties";
    private static final String PROFILE_FILE_KEY = "project.profile.file";
    private static final String DB_STATUS_KEY = "KNOWLEDGE_DB_STATUS";

    private final Properties frameworkProperties = new Properties();
    private final Map<String, String> profileProperties;

    public RuntimeProperties() {
        this(DEFAULT_RESOURCE);
    }

    public RuntimeProperties(String resourceName) {
        loadFrameworkProperties(resourceName == null || resourceName.isBlank() ? DEFAULT_RESOURCE : resourceName);
        this.profileProperties = loadProfileProperties();
    }

    public RuntimeProperties(Properties properties) {
        if (properties != null) {
            this.frameworkProperties.putAll(properties);
        }
        this.profileProperties = loadProfileProperties();
    }

    public String readValue(String key, String fallback) {
        String value = firstNonBlank(
                readSystemProperty(key),
                readEnvironment(key),
                profileProperties.get(key),
                frameworkProperties.getProperty(key)
        );
        return resolvePlaceholders(value == null || value.isBlank() ? Objects.requireNonNull(fallback) : value.trim());
    }

    public String readOptional(String propertyKey, String environmentKey) {
        return resolvePlaceholders(firstNonBlank(
                readSystemProperty(propertyKey),
                readEnvironment(environmentKey),
                profileProperties.get(propertyKey),
                frameworkProperties.getProperty(propertyKey)
        ));
    }

    public boolean readBoolean(String key, String fallback) {
        return Boolean.parseBoolean(readValue(key, fallback));
    }

    public boolean readKnowledgeDbBoolean(String key, String fallback) {
        String override = firstNonBlank(
                readSystemProperty("knowledge.db.status"),
                readSystemProperty(DB_STATUS_KEY),
                readEnvironment(DB_STATUS_KEY)
        );
        if (override != null && !override.isBlank()) {
            return Boolean.parseBoolean(override.trim());
        }
        return readBoolean(key, fallback);
    }

    public String profileFile() {
        return readValue(PROFILE_FILE_KEY, "");
    }

    private void loadFrameworkProperties(String resourceName) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find runtime config resource: " + resourceName);
            }
            frameworkProperties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load runtime config resource: " + resourceName, exception);
        }
    }

    private Map<String, String> loadProfileProperties() {
        String profileFile = firstNonBlank(
                readSystemProperty(PROFILE_FILE_KEY),
                readEnvironment(PROFILE_FILE_KEY),
                frameworkProperties.getProperty(PROFILE_FILE_KEY)
        );
        if (profileFile == null || profileFile.isBlank()) {
            return Map.of();
        }
        String yaml = readProfileFile(resolvePlaceholders(profileFile.trim()));
        if (yaml.isBlank()) {
            return Map.of();
        }
        Object loaded = new Yaml().load(yaml);
        if (!(loaded instanceof Map<?, ?> root)) {
            return Map.of();
        }
        Map<String, String> flattened = new LinkedHashMap<>();
        flatten("", root, flattened);
        return Map.copyOf(toRuntimeProperties(flattened));
    }

    private String readProfileFile(String profileFile) {
        Path path = Path.of(profileFile);
        if (Files.isRegularFile(path)) {
            try {
                return Files.readString(path, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read project profile file: " + profileFile, exception);
            }
        }
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(profileFile)) {
            if (input == null) {
                throw new IllegalStateException("Cannot find project profile file: " + profileFile);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read project profile resource: " + profileFile, exception);
        }
    }

    private void flatten(String prefix, Map<?, ?> source, Map<String, String> target) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toString().trim();
            if (key.isBlank()) {
                continue;
            }
            String fullKey = prefix.isBlank() ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                flatten(fullKey, nested, target);
            } else if (value != null) {
                target.put(fullKey, value.toString().trim());
            }
        }
    }

    private Map<String, String> toRuntimeProperties(Map<String, String> flattened) {
        Map<String, String> result = new LinkedHashMap<>();
        put(result, "project.profile-id", flattened.get("project.id"));
        put(result, "project.name", flattened.get("project.name"));
        put(result, "project.base-url", flattened.get("project.baseUrl"));
        put(result, "project.requirements.file", flattened.get("requirements.file"));
        put(result, "project.route.home", flattened.get("routes.home"));
        put(result, "project.route.login", flattened.get("routes.login"));
        put(result, "project.route.registration", flattened.get("routes.registration"));
        put(result, "project.route.authenticated", flattened.get("routes.authenticated"));
        put(result, "project.route.recovery", flattened.get("routes.recovery"));
        put(result, "project.route.details", flattened.get("routes.details"));
        put(result, "project.route.form", flattened.get("routes.form"));
        put(result, "project.route.security", flattened.get("routes.security"));
        put(result, "project.route.catalog", flattened.get("routes.catalog"));
        put(result, "project.route.products", flattened.get("routes.products"));
        put(result, "project.route.cart", flattened.get("routes.cart"));
        put(result, "ui.base-url", flattened.get("ui.baseUrl"));
        put(result, "ui.browser", flattened.get("ui.browser"));
        put(result, "ui.headless", flattened.get("ui.headless"));
        put(result, "ui.timeout-seconds", flattened.get("ui.timeoutSeconds"));
        put(result, "discovery.auth.enabled", flattened.get("auth.enabled"));
        putEnvPlaceholder(result, "discovery.auth.username", flattened.get("auth.usernameEnv"));
        putEnvPlaceholder(result, "discovery.auth.password", flattened.get("auth.passwordEnv"));
        put(result, "discovery.auth.username-selector", flattened.get("auth.usernameSelector"));
        put(result, "discovery.auth.password-selector", flattened.get("auth.passwordSelector"));
        put(result, "discovery.auth.submit-selector", flattened.get("auth.submitSelector"));
        put(result, "openai.enabled", flattened.get("ai.openAiEnabled"));
        put(result, "ai.page-enrichment.llm.enabled", flattened.get("ai.pageEnrichmentLlmEnabled"));
        put(result, "ai.page-object.llm.enabled", flattened.get("ai.pageObjectLlmEnabled"));
        put(result, "ai.ui-test.llm.enabled", flattened.get("ai.uiTestLlmEnabled"));
        put(result, "rag.enabled", flattened.get("knowledge.ragEnabled"));
        put(result, "knowledge.graph.enabled", flattened.get("knowledge.graphEnabled"));
        put(result, "knowledge.vector.enabled", flattened.get("knowledge.vectorEnabled"));
        put(result, "artifact.reuse.enabled", flattened.get("artifactReuse.enabled"));
        put(result, "artifact.reuse.force-refresh", flattened.get("artifactReuse.forceRefresh"));
        put(result, "artifact.reuse.stable-root", flattened.get("artifactReuse.stableRoot"));
        put(result, "artifact.reuse.prompt-template-version", flattened.get("artifactReuse.promptTemplateVersion"));
        put(result, "artifact.reuse.flow-contract.enabled", flattened.get("artifactReuse.flowContractEnabled"));
        put(result, "artifact.reuse.pom-contract.enabled", flattened.get("artifactReuse.pomContractEnabled"));
        put(result, "artifact.reuse.test-data.enabled", flattened.get("artifactReuse.testDataEnabled"));
        put(result, "artifact.reuse.policy", flattened.get("artifactReuse.policy"));
        put(result, "artifact.reuse.explain-decisions", flattened.get("artifactReuse.explainDecisions"));
        put(result, "artifact.reuse.writer-version", flattened.get("artifactReuse.writerVersion"));
        put(result, "semantic.reuse.enabled", flattened.get("semantic.reuseEnabled"));
        put(result, "spa.inventory.enabled", flattened.get("spa.inventory.enabled"));
        put(result, "spa.inventory.max-components", flattened.get("spa.inventory.maxComponents"));
        put(result, "spa.discovery.mode", flattened.get("spa.discovery.mode"));
        put(result, "spa.targeted-verification.enabled", flattened.get("spa.targetedVerification.enabled"));
        put(result, "spa.evidence.min-confirmed-score", flattened.get("spa.evidence.minConfirmedScore"));
        put(result, "spa.evidence.promote-after-successes", flattened.get("spa.evidence.promoteAfterSuccesses"));
        put(result, "spa.evidence.demote-after-failures", flattened.get("spa.evidence.demoteAfterFailures"));
        put(result, "spa.live-verification.enabled", flattened.get("spa.liveVerification.enabled"));
        put(result, "spa.live-verification.execute-session-ending-actions", flattened.get("spa.liveVerification.executeSessionEndingActions"));
        put(result, "spa.live-verification.execute-safe-actions", flattened.get("spa.liveVerification.executeSafeActions"));
        put(result, "spa.live-verification.execute-data-actions", flattened.get("spa.liveVerification.executeDataActions"));
        put(result, "spa.evidence.retention.enabled", flattened.get("spa.evidence.retention.enabled"));
        put(result, "spa.evidence.retention.degraded-days", flattened.get("spa.evidence.retention.degradedDays"));
        put(result, "spa.evidence.retention.orphan-days", flattened.get("spa.evidence.retention.orphanDays"));
        put(result, "spa.evidence.retention.hard-delete", flattened.get("spa.evidence.retention.hardDelete"));
        return result;
    }

    private void put(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value.trim());
        }
    }

    private void putEnvPlaceholder(Map<String, String> target, String key, String envName) {
        if (envName != null && !envName.isBlank()) {
            target.put(key, "${" + envName.trim() + "}");
        }
    }

    private String resolvePlaceholders(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String resolved = value;
        for (int index = 0; index < 8; index++) {
            int start = resolved.indexOf("${");
            if (start < 0) {
                return resolved;
            }
            int end = resolved.indexOf('}', start);
            if (end < 0) {
                return resolved;
            }
            String key = resolved.substring(start + 2, end).trim();
            String replacement = readRawValue(key);
            resolved = resolved.substring(0, start)
                    + (replacement == null ? "" : replacement)
                    + resolved.substring(end + 1);
        }
        return resolved;
    }

    private String readRawValue(String key) {
        return firstNonBlank(
                readSystemProperty(key),
                readEnvironment(key),
                profileProperties == null ? null : profileProperties.get(key),
                frameworkProperties.getProperty(key)
        );
    }

    private String readSystemProperty(String key) {
        String value = System.getProperty(key);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String readEnvironment(String keyOrEnvName) {
        String normalized = keyOrEnvName.contains(".")
                ? keyOrEnvName.toUpperCase().replace('.', '_').replace('-', '_')
                : keyOrEnvName;
        String value = System.getenv(normalized);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
