package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.LiveTransitionDiscovery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LiveTransitionDiscoveryArtifactWriter {
    public String write(LiveTransitionDiscovery discovery) {
        try {
            Path path = Path.of("target", "discovery", "spa-live-transition-discovery.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), discovery);
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA live transition discovery", exception);
        }
    }
}
