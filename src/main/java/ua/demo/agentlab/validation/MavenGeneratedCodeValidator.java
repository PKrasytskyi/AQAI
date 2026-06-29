package ua.demo.agentlab.validation;

import ua.demo.agentlab.orchestration.WorkflowState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class MavenGeneratedCodeValidator implements GeneratedCodeValidator {

    private final String workingDirectory;

    public MavenGeneratedCodeValidator(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public GeneratedCodeValidationResult validate(WorkflowState state) {
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand());
        processBuilder.directory(new java.io.File(workingDirectory));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            ValidationStatus status = determineStatus(exitCode, output);

            List<GeneratedFileValidation> fileResults = state.getWrittenFiles().stream()
                    .map(path -> new GeneratedFileValidation(
                            path,
                            status,
                            switch (status) {
                                case PASSED -> "Compiled successfully";
                                case UNAVAILABLE -> "Validation could not complete because the local environment denied compiler access";
                                case FAILED -> "Compilation failed. See compiler output";
                            }
                    ))
                    .toList();

            String summary = switch (status) {
                case PASSED -> "Generated code compiled successfully";
                case UNAVAILABLE -> "Generated code validation is unavailable in the current local environment";
                case FAILED -> "Generated code compilation failed";
            };

            return new GeneratedCodeValidationResult(
                    status,
                    summary,
                    output,
                    fileResults
            );
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return new GeneratedCodeValidationResult(
                    ValidationStatus.FAILED,
                    "Generated code validation could not be completed",
                    exception.getMessage(),
                    state.getWrittenFiles().stream()
                            .map(path -> new GeneratedFileValidation(
                                    path,
                                    ValidationStatus.FAILED,
                                    "Validation process failed before compilation"
                            ))
                            .toList()
            );
        }
    }

    private List<String> buildCommand() {
        String mvnExecutable = isWindows() ? "mvn.cmd" : "mvn";

        return List.of(
                mvnExecutable,
                "--batch-mode",
                "-Duser.home=.",
                "-Dmaven.repo.local=.m2repo",
                "test-compile"
        );
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private ValidationStatus determineStatus(int exitCode, String output) {
        if (exitCode == 0) {
            return ValidationStatus.PASSED;
        }

        if (output != null && (output.contains("Access is denied.")
                || output.contains("AccessDeniedException"))) {
            return ValidationStatus.UNAVAILABLE;
        }

        return ValidationStatus.FAILED;
    }
}
