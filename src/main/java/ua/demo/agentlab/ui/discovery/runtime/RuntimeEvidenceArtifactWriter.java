package ua.demo.agentlab.ui.discovery.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RuntimeEvidenceArtifactWriter {

    private final Path outputDirectory;
    private final ObjectMapper objectMapper;

    public RuntimeEvidenceArtifactWriter() {
        this(Path.of("target", "discovery", "runtime"));
    }

    public RuntimeEvidenceArtifactWriter(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("outputDirectory cannot be null");
        }
        this.outputDirectory = outputDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> write(RuntimeEvidenceBundle bundle) {
        if (bundle == null) {
            bundle = RuntimeEvidenceBundle.empty();
        }
        try {
            Files.createDirectories(outputDirectory);
            List<String> writtenFiles = new ArrayList<>();
            writtenFiles.add(writeJson("runtime-evidence.json", bundle));
            writtenFiles.add(writeJson("runtime-network-evidence.json", bundle.semanticNetworkEvidence()));
            writtenFiles.add(writeJson("spa-state-transitions.json", bundle.stateTransitions()));
            return writtenFiles;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write runtime evidence artifacts to: " + outputDirectory, exception);
        }
    }

    private String writeJson(String fileName, Object value) throws IOException {
        Path outputFile = outputDirectory.resolve(fileName);
        objectMapper.writeValue(outputFile.toFile(), value);
        return outputFile.toString();
    }
}
