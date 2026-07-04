package ua.demo.agentlab.ui.discovery.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.component.model.ComponentDiscoveryModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ComponentModelArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public ComponentModelArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public ComponentModelArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(ComponentDiscoveryModel model) {
        if (model == null) {
            return List.of();
        }
        try {
            Files.createDirectories(outputDirectory);
            Path outputFile = outputDirectory.resolve("component-model.json");
            objectMapper.writeValue(outputFile.toFile(), model);
            return List.of(outputFile.toAbsolutePath().normalize().toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write component model artifact to: " + outputDirectory, exception);
        }
    }
}
