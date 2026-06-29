package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.ArtifactTextSnippet;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.ReadAllureResultsRequest;
import ua.demo.agentlab.mcp.model.ReadAllureResultsResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class ReadAllureResultsTool implements McpTool<ReadAllureResultsRequest, ReadAllureResultsResult> {

    private final WorkspacePathResolver pathResolver;

    public ReadAllureResultsTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.READ_ALLURE_RESULTS;
    }

    @Override
    public Class<ReadAllureResultsRequest> inputType() {
        return ReadAllureResultsRequest.class;
    }

    @Override
    public ReadAllureResultsResult execute(ReadAllureResultsRequest input) {
        String directoryPath = input == null || input.directoryPath() == null || input.directoryPath().isBlank()
                ? "allure-results"
                : input.directoryPath();
        int maxFiles = input == null || input.maxFiles() <= 0 ? 20 : input.maxFiles();
        int maxCharsPerFile = input == null || input.maxCharsPerFile() <= 0 ? 4000 : input.maxCharsPerFile();

        try {
            Path absoluteDirectory = pathResolver.resolveFile(directoryPath);
            if (!Files.exists(absoluteDirectory) || !Files.isDirectory(absoluteDirectory)) {
                return new ReadAllureResultsResult(
                        McpExecutionStatus.NOT_FOUND,
                        "Allure results directory not found",
                        absoluteDirectory,
                        List.of()
                );
            }

            List<ArtifactTextSnippet> files = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(absoluteDirectory)) {
                stream.filter(Files::isRegularFile)
                        .sorted(Comparator.comparing(path -> absoluteDirectory.relativize(path).toString()))
                        .limit(maxFiles)
                        .forEach(path -> files.add(readSnippet(absoluteDirectory, path, maxCharsPerFile)));
            }

            return new ReadAllureResultsResult(
                    McpExecutionStatus.SUCCESS,
                    "Allure results read successfully",
                    absoluteDirectory,
                    files
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Allure results", exception);
        }
    }

    private ArtifactTextSnippet readSnippet(Path root, Path file, int maxCharsPerFile) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            boolean truncated = content.length() > maxCharsPerFile;
            String effectiveContent = truncated ? content.substring(0, maxCharsPerFile) : content;
            return new ArtifactTextSnippet(root.relativize(file).toString().replace('\\', '/'), effectiveContent, truncated);
        } catch (IOException exception) {
            return new ArtifactTextSnippet(
                    root.relativize(file).toString().replace('\\', '/'),
                    "Cannot read file: " + exception.getMessage(),
                    false
            );
        }
    }
}
