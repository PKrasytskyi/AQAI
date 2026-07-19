package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.TypedComponentFlowBundle;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TypedComponentFlowArtifactWriter {
    public String write(TypedComponentFlowBundle bundle) {
        try {
            Path path=Path.of("target","discovery","typed-component-flows.json");
            Files.createDirectories(path.getParent());
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(path.toFile(), bundle);
            return path.toAbsolutePath().toString();
        } catch (Exception exception) { throw new IllegalStateException("Failed to write typed component flow artifact", exception); }
    }
}
