package ua.demo.agentlab.mcp.model;

import java.util.List;

public record JiraStoryInput(
        String key,
        String summary,
        String description,
        List<String> acceptanceCriteria,
        List<String> labels,
        String url
) {
}
