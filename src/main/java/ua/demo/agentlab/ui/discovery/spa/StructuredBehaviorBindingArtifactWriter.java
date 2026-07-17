package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.BoundSpaBehaviorContract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes the evidence binding boundary so a needs-review decision is inspectable without raw prompts. */
public final class StructuredBehaviorBindingArtifactWriter {
    public String write(List<BoundSpaBehaviorContract> bindings) {
        try {
            Path path = Path.of("target", "discovery", "spa-structured-behavior-bindings.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), bindings == null ? List.of() : bindings);
            return path.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA structured behavior bindings", exception);
        }
    }
}
