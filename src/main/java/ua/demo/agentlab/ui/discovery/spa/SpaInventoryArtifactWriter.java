package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SpaInventoryBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpaInventoryArtifactWriter {

    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public SpaInventoryArtifactWriter() {
        this(Path.of("target", "discovery", "spa-inventory.json"));
    }

    public SpaInventoryArtifactWriter(Path outputPath) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.outputPath = outputPath == null ? Path.of("target", "discovery", "spa-inventory.json") : outputPath;
    }

    public String write(SpaInventoryBundle inventory) {
        try {
            Files.createDirectories(outputPath.getParent());
            objectMapper.writeValue(outputPath.toFile(), inventory);
            return outputPath.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA inventory artifact", exception);
        }
    }
}
