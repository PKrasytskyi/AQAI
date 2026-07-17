package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SpaBehaviorExecutionBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StructuredBehaviorExecutionArtifactWriter {
    public String write(SpaBehaviorExecutionBundle result) {
        try {
            Path path = Path.of("target", "discovery", "spa-structured-behavior-execution.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), result);
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA structured behavior execution", exception);
        }
    }
}
