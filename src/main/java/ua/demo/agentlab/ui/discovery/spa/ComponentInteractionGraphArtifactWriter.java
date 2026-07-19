package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.ComponentInteractionGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ComponentInteractionGraphArtifactWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path directory;

    public ComponentInteractionGraphArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public ComponentInteractionGraphArtifactWriter(Path directory) {
        this.directory = directory == null ? Path.of("target", "discovery") : directory;
    }

    public String write(ComponentInteractionGraph graph) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve("component-interaction-graph.json");
            mapper.writeValue(file.toFile(), graph);
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write component interaction graph artifact", exception);
        }
    }
}
