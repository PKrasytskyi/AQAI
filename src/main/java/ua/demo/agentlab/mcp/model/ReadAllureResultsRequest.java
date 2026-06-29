package ua.demo.agentlab.mcp.model;

public record ReadAllureResultsRequest(
        String directoryPath,
        int maxFiles,
        int maxCharsPerFile
) {
}
