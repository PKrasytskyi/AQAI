package ua.demo.agentlab.mcp.model;

import java.util.List;

public record RequirementItem(
        String id,
        String title,
        String description,
        List<String> acceptanceCriteria,
        List<String> labels
) {
}
