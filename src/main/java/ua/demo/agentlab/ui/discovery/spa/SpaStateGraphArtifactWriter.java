package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SpaStateGraph;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the compact, actionable state graph separately from raw discovery snapshots. */
public class SpaStateGraphArtifactWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String write(SpaStateGraph graph) {
        try {
            Path directory = Path.of("target", "discovery");
            Files.createDirectories(directory);
            Path path = directory.resolve("spa-state-graph.json");
            mapper.writeValue(path.toFile(), graph);
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA state graph artifact", exception);
        }
    }
}
