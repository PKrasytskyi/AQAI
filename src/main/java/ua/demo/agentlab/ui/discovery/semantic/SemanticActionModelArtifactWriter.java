package ua.demo.agentlab.ui.discovery.semantic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.semantic.model.SemanticActionModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SemanticActionModelArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public SemanticActionModelArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public SemanticActionModelArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(SemanticActionModel model) {
        if (model == null) {
            return List.of();
        }
        try {
            Files.createDirectories(outputDirectory);
            Path outputFile = outputDirectory.resolve("semantic-action-model.json");
            objectMapper.writeValue(outputFile.toFile(), model);
            return List.of(outputFile.toAbsolutePath().normalize().toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write semantic action model artifact to: " + outputDirectory, exception);
        }
    }
}
