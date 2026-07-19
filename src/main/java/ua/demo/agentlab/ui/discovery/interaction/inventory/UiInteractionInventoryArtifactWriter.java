package ua.demo.agentlab.ui.discovery.interaction.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UiInteractionInventoryArtifactWriter {

    private final ObjectMapper objectMapper;
    private final Path outputPath;

    public UiInteractionInventoryArtifactWriter() {
        this(Path.of("target", "discovery", "ui-interaction-inventory.json"));
    }

    public UiInteractionInventoryArtifactWriter(Path outputPath) {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.outputPath = outputPath == null
                ? Path.of("target", "discovery", "ui-interaction-inventory.json") : outputPath;
    }

    public String write(UiInteractionInventory inventory) {
        try {
            Files.createDirectories(outputPath.getParent());
            objectMapper.writeValue(outputPath.toFile(), inventory);
            return outputPath.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write UI interaction inventory artifact", exception);
        }
    }
}
