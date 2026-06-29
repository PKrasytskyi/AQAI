package ua.demo.agentlab.mcp.model;

import java.util.List;

public record CanonicalRequirementBundle(
        SourceDescriptor source,
        List<RequirementItem> requirements,
        List<String> warnings
) {
}
