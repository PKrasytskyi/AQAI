package ua.demo.agentlab.artifactreuse.fingerprint;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CanonicalArtifactHasher {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    public ArtifactFingerprint hash(Map<String, Object> canonicalInput) {
        try {
            String canonical = objectMapper.writeValueAsString(canonicalize(canonicalInput));
            return new ArtifactFingerprint("SHA-256", sha256(canonical), canonical);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to canonicalize artifact fingerprint input", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Object canonicalize(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return normalize(text);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .forEach(entry -> result.put(String.valueOf(entry.getKey()), canonicalize(entry.getValue())));
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> normalized = collection.stream()
                    .map(this::canonicalize)
                    .toList();
            if (normalized.stream().allMatch(item -> item instanceof String)) {
                return normalized.stream()
                        .map(String.class::cast)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();
            }
            return normalized.stream()
                    .sorted(Comparator.comparing(String::valueOf))
                    .toList();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Map) {
            return canonicalize((Map<String, Object>) value);
        }
        return normalize(String.valueOf(value));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to hash artifact fingerprint input", exception);
        }
    }
}
