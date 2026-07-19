package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SourceStateBindingBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceStateBindingArtifactWriter {
    public String write(SourceStateBindingBundle bindings) {
        try {
            Path path = Path.of("target", "discovery", "spa-source-state-bindings.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), bindings);
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA source state bindings", exception);
        }
    }
}
