package ua.demo.agentlab.mcp.tool;

import ua.demo.agentlab.mcp.McpTool;
import ua.demo.agentlab.mcp.model.McpToolName;
import ua.demo.agentlab.mcp.model.RunMavenTestRequest;
import ua.demo.agentlab.mcp.model.RunMavenTestResult;
import ua.demo.agentlab.mcp.result.McpExecutionStatus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RunMavenTestTool implements McpTool<RunMavenTestRequest, RunMavenTestResult> {

    private final WorkspacePathResolver pathResolver;

    public RunMavenTestTool(Path workspaceRoot) {
        this.pathResolver = new WorkspacePathResolver(workspaceRoot);
    }

    @Override
    public McpToolName name() {
        return McpToolName.RUN_MAVEN_TEST;
    }

    @Override
    public Class<RunMavenTestRequest> inputType() {
        return RunMavenTestRequest.class;
    }

    @Override
    public RunMavenTestResult execute(RunMavenTestRequest input) {
        List<String> goals = input == null || input.goals() == null || input.goals().isEmpty()
                ? List.of("test")
                : input.goals();
        int timeoutSeconds = input == null || input.timeoutSeconds() <= 0 ? 300 : input.timeoutSeconds();
        List<String> command = new ArrayList<>();
        command.add("mvn");
        command.add("--batch-mode");
        command.add("-Duser.home=.");
        command.add("-Dmaven.repo.local=.m2repo");
        command.addAll(goals);
        if (input != null && input.additionalArguments() != null) {
            command.addAll(input.additionalArguments());
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(pathResolver.workspaceRoot().toFile());
        builder.redirectErrorStream(true);

        Instant startedAt = Instant.now();
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long durationMillis = Duration.between(startedAt, Instant.now()).toMillis();
            String output = readOutput(process.getInputStream());

            if (!finished) {
                process.destroyForcibly();
                return new RunMavenTestResult(
                        McpExecutionStatus.FAILED,
                        "Maven process timed out",
                        -1,
                        durationMillis,
                        output
                );
            }

            int exitCode = process.exitValue();
            return new RunMavenTestResult(
                    exitCode == 0 ? McpExecutionStatus.SUCCESS : McpExecutionStatus.FAILED,
                    exitCode == 0 ? "Maven command completed successfully" : "Maven command failed",
                    exitCode,
                    durationMillis,
                    output
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Maven execution", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to execute Maven command", exception);
        }
    }

    private String readOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
