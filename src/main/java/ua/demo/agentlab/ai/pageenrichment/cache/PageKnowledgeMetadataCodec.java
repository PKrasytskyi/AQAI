package ua.demo.agentlab.ai.pageenrichment.cache;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PageKnowledgeMetadataCodec {

    private static final String LIST_DELIMITER = "\u001F";
    private static final String ENTRY_DELIMITER = "\u001E";
    private static final String KEY_VALUE_DELIMITER = "\u001D";

    private PageKnowledgeMetadataCodec() {
    }

    public static String encodeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(PageKnowledgeMetadataCodec::escape)
                .distinct()
                .collect(java.util.stream.Collectors.joining(LIST_DELIMITER));
    }

    public static List<String> decodeList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(LIST_DELIMITER, -1))
                .map(PageKnowledgeMetadataCodec::unescape)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    public static String encodeFacts(Map<String, List<String>> facts) {
        if (facts == null || facts.isEmpty()) {
            return "";
        }
        return facts.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank())
                .map(entry -> escape(entry.getKey()) + KEY_VALUE_DELIMITER + encodeList(entry.getValue()))
                .collect(java.util.stream.Collectors.joining(ENTRY_DELIMITER));
    }

    public static Map<String, List<String>> decodeFacts(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        Map<String, List<String>> facts = new LinkedHashMap<>();
        for (String entry : value.split(ENTRY_DELIMITER, -1)) {
            if (entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(KEY_VALUE_DELIMITER, 2);
            if (parts.length == 0 || parts[0].isBlank()) {
                continue;
            }
            facts.put(unescape(parts[0]), parts.length > 1 ? decodeList(parts[1]) : List.of());
        }
        return Map.copyOf(facts);
    }

    private static String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace(LIST_DELIMITER, "\\u001F")
                .replace(ENTRY_DELIMITER, "\\u001E")
                .replace(KEY_VALUE_DELIMITER, "\\u001D");
    }

    private static String unescape(String value) {
        return value == null ? "" : value
                .replace("\\u001D", KEY_VALUE_DELIMITER)
                .replace("\\u001E", ENTRY_DELIMITER)
                .replace("\\u001F", LIST_DELIMITER)
                .replace("\\\\", "\\");
    }
}
