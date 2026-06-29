package ua.demo.agentlab.mcp.model;

import java.util.List;

public record RunMavenTestRequest(
        List<String> goals,
        List<String> additionalArguments,
        int timeoutSeconds
) {
}
