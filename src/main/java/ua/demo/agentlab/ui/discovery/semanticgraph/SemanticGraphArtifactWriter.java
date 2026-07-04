package ua.demo.agentlab.ui.discovery.semanticgraph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.semanticgraph.model.SemanticGraphModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SemanticGraphArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public SemanticGraphArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public SemanticGraphArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(SemanticGraphModel model) {
        if (model == null) {
            model = SemanticGraphModel.empty("semantic-graph:null");
        }
        try {
            Files.createDirectories(outputDirectory);
            Path outputFile = outputDirectory.resolve("semantic-graph.json");
            objectMapper.writeValue(outputFile.toFile(), model);
            return List.of(outputFile.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write semantic graph artifact to: " + outputDirectory, exception);
        }
    }
}
