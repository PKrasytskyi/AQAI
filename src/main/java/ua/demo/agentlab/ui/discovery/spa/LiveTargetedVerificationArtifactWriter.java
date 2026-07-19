package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LiveTargetedVerificationArtifactWriter {
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public String write(SpaLiveTargetedVerificationResult result) {
        try {
            Path directory = Path.of("target", "discovery");
            Files.createDirectories(directory);
            Path file = directory.resolve("spa-live-targeted-verification.json");
            mapper.writeValue(file.toFile(), result);
            return file.toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA live targeted verification artifact", exception);
        }
    }
}
