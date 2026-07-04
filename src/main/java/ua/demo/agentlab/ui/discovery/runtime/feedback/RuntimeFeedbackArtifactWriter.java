package ua.demo.agentlab.ui.discovery.runtime.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RuntimeFeedbackArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public RuntimeFeedbackArtifactWriter() {
        this(Path.of("target", "discovery", "runtime"));
    }

    public RuntimeFeedbackArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(RuntimeFeedbackSummary summary) {
        if (summary == null) {
            summary = new RuntimeFeedbackSummary(0, 0, 0, 0, 0, 0, 0, 0.0d, 0.0d, List.of());
        }
        try {
            Files.createDirectories(outputDirectory);
            List<String> files = new ArrayList<>();
            Path summaryFile = outputDirectory.resolve("runtime-feedback-summary.json");
            objectMapper.writeValue(summaryFile.toFile(), summary);
            files.add(summaryFile.toString());
            if (!summary.issues().isEmpty()) {
                Path issuesFile = outputDirectory.resolve("runtime-feedback-issues.json");
                objectMapper.writeValue(issuesFile.toFile(), summary.issues());
                files.add(issuesFile.toString());
            }
            return files;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write runtime feedback artifacts to: " + outputDirectory, exception);
        }
    }
}
