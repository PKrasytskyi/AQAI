package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.TargetStateBindingBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TargetStateBindingArtifactWriter {
    private final SensitiveSpaArtifactSanitizer sanitizer = new SensitiveSpaArtifactSanitizer();

    public String write(TargetStateBindingBundle bindings) {
        try {
            Path path = Path.of("target", "discovery", "spa-target-state-bindings.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValue(path.toFile(), sanitizer.sanitize(bindings));
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA target state bindings", exception);
        }
    }
}
