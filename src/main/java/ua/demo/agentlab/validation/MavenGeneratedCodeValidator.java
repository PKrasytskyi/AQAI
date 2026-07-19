package ua.demo.agentlab.validation;

import ua.demo.agentlab.orchestration.WorkflowState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class MavenGeneratedCodeValidator implements GeneratedCodeValidator {

    private final String workingDirectory;

    public MavenGeneratedCodeValidator(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public GeneratedCodeValidationResult validate(List<String> writtenFiles) {
        List<String> paths = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
        if (paths.isEmpty()) {
            return new GeneratedCodeValidationResult(
                    ValidationStatus.FAILED,
                    "Generated code validation has no current-run manifest sources",
                    "No manifest-owned source paths were supplied",
                    List.of()
            );
        }
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(paths));
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

            List<GeneratedFileValidation> fileResults = paths.stream()
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
                    paths.stream()
                            .map(path -> new GeneratedFileValidation(
                                    path,
                                    ValidationStatus.FAILED,
                                    "Validation process failed before compilation"
                            ))
                            .toList()
            );
        }
    }

    private List<String> buildCommand(List<String> manifestPaths) {
        String mvnExecutable = isWindows() ? "mvn.cmd" : "mvn";
        Path sourceRoot = commonSourceRoot(manifestPaths);

        return List.of(
                mvnExecutable,
                "--batch-mode",
                "-Duser.home=.",
                "-Dmaven.repo.local=.m2repo",
                "-Dagentlab.testSourceDirectory=" + sourceRoot,
                "test-compile"
        );
    }

    public Path commonSourceRoot(List<String> manifestPaths) {
        List<Path> parents = manifestPaths == null ? List.of() : manifestPaths.stream()
                .map(Path::of)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .map(Path::getParent)
                .toList();
        if (parents.isEmpty() || parents.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Manifest source paths must have parent directories");
        }
        Path common = parents.get(0);
        for (int index = 1; index < parents.size(); index++) {
            common = commonAncestor(common, parents.get(index));
        }
        Path testSourceRoot = Path.of(workingDirectory, "src", "test", "java")
                .toAbsolutePath()
                .normalize();
        if (!common.startsWith(testSourceRoot) || common.equals(testSourceRoot)) {
            throw new IllegalArgumentException(
                    "Manifest sources must share a project-specific namespace below " + testSourceRoot
            );
        }
        return common;
    }

    private Path commonAncestor(Path left, Path right) {
        Path current = left;
        while (current != null && !right.startsWith(current)) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalArgumentException("Manifest source paths do not share a common directory");
        }
        return current;
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
