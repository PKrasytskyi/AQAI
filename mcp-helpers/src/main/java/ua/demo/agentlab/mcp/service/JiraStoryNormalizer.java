package ua.demo.agentlab.mcp.service;

import org.springframework.stereotype.Service;
import ua.demo.agentlab.mcp.model.CanonicalRequirementBundle;
import ua.demo.agentlab.mcp.model.JiraStoryInput;
import ua.demo.agentlab.mcp.model.RequirementItem;
import ua.demo.agentlab.mcp.model.SourceDescriptor;
import ua.demo.agentlab.mcp.model.SourceKind;

import java.util.ArrayList;
import java.util.List;

@Service
public class JiraStoryNormalizer {

    public CanonicalRequirementBundle normalize(JiraStoryInput input) {
        requireText(input.key(), "Jira key");
        requireText(input.summary(), "Jira summary");

        List<String> criteria = safeList(input.acceptanceCriteria()).stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();

        List<String> warnings = new ArrayList<>();
        if (criteria.isEmpty()) {
            warnings.add("The Jira story does not contain explicit acceptance criteria.");
        }

        RequirementItem requirement = new RequirementItem(
                input.key().trim(),
                input.summary().trim(),
                safe(input.description()),
                criteria,
                safeList(input.labels())
        );

        SourceDescriptor source = new SourceDescriptor(
                SourceKind.JIRA_STORY,
                input.key().trim(),
                input.summary().trim(),
                blankToNull(input.url())
        );

        return new CanonicalRequirementBundle(source, List.of(requirement), warnings);
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }
}
