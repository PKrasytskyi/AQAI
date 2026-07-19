package ua.demo.agentlab.ui.discovery.spa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.spa.model.SpaEvidenceLifecycleResult;
import ua.demo.agentlab.ui.discovery.spa.model.SpaTargetedVerificationResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SpaTargetedVerificationArtifactWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path outputDirectory;

    public SpaTargetedVerificationArtifactWriter() {
        this(Path.of("target", "discovery"));
    }

    public SpaTargetedVerificationArtifactWriter(Path outputDirectory) {
        this.outputDirectory = outputDirectory == null ? Path.of("target", "discovery") : outputDirectory;
    }

    public List<String> write(SpaTargetedVerificationResult verification, SpaEvidenceLifecycleResult lifecycle) {
        try {
            Files.createDirectories(outputDirectory);
            Path verificationFile = outputDirectory.resolve("spa-targeted-verification.json");
            Path lifecycleFile = outputDirectory.resolve("spa-evidence-lifecycle.json");
            objectMapper.writeValue(verificationFile.toFile(), verification);
            objectMapper.writeValue(lifecycleFile.toFile(), lifecycle);
            return List.of(verificationFile.toAbsolutePath().toString(), lifecycleFile.toAbsolutePath().toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write SPA targeted verification artifacts", exception);
        }
    }
}
