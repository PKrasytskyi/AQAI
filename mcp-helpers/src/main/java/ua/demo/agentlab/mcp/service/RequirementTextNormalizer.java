package ua.demo.agentlab.mcp.service;

import org.springframework.stereotype.Service;
import ua.demo.agentlab.mcp.model.CanonicalRequirementBundle;
import ua.demo.agentlab.mcp.model.RequirementItem;
import ua.demo.agentlab.mcp.model.SourceDescriptor;
import ua.demo.agentlab.mcp.model.SourceKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class RequirementTextNormalizer {

    public CanonicalRequirementBundle normalize(
            String text,
            String sourceId,
            String title,
            SourceKind sourceKind
    ) {
        String normalizedText = normalizeLineEndings(text);
        List<String> lines = normalizedText.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        List<String> criteria = lines.stream()
                .filter(this::isListItem)
                .map(this::removeListMarker)
                .toList();

        String description = lines.stream()
                .filter(line -> !isHeading(line))
                .filter(line -> !isListItem(line))
                .reduce((first, second) -> first + "\n" + second)
                .orElse(normalizedText);

        List<String> warnings = new ArrayList<>();
        if (criteria.isEmpty()) {
            warnings.add("No explicit acceptance criteria were detected.");
        }

        String resolvedTitle = firstNonBlank(
                title,
                lines.stream().filter(this::isHeading).map(this::removeHeadingMarker).findFirst().orElse(null),
                "Untitled requirement"
        );

        RequirementItem requirement = new RequirementItem(
                firstNonBlank(sourceId, "REQ-1"),
                resolvedTitle,
                description,
                criteria,
                List.of()
        );

        SourceDescriptor source = new SourceDescriptor(
                sourceKind == null ? SourceKind.RAW_TEXT : sourceKind,
                firstNonBlank(sourceId, "raw-text"),
                resolvedTitle,
                null
        );

        return new CanonicalRequirementBundle(source, List.of(requirement), warnings);
    }

    private String normalizeLineEndings(String text) {
        return text == null ? "" : text.trim().replace("\r\n", "\n").replace('\r', '\n');
    }

    private boolean isHeading(String line) {
        return line.startsWith("#");
    }

    private String removeHeadingMarker(String line) {
        return line.replaceFirst("^#+\\s*", "").trim();
    }

    private boolean isListItem(String line) {
        return line.matches("^[-*]\\s+.+") || line.matches("^\\d+[.)]\\s+.+");
    }

    private String removeListMarker(String line) {
        return line.replaceFirst("^([-*]|\\d+[.)])\\s+", "").trim();
    }

    private String firstNonBlank(String... values) {
        return Arrays.stream(values)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("");
    }
}
