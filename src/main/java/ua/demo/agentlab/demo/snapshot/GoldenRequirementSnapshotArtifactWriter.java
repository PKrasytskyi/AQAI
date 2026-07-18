package ua.demo.agentlab.demo.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the four reviewable BW-02 artifacts without updating source fixtures implicitly. */
public final class GoldenRequirementSnapshotArtifactWriter {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public void write(Path outputDirectory, GoldenRequirementSnapshot snapshot) {
        if (outputDirectory == null || snapshot == null) {
            throw new IllegalArgumentException("Output directory and snapshot are required");
        }
        try {
            Files.createDirectories(outputDirectory);
            write(outputDirectory.resolve("normalized-requirements.json"), snapshot.normalizedRequirementBundle());
            write(outputDirectory.resolve("structured-behavior-contracts.json"), snapshot.structuredBehaviorContracts());
            write(outputDirectory.resolve("canonical-test-cases.json"), snapshot.canonicalTestCaseBundle());
            write(outputDirectory.resolve("governance-requirements.json"), snapshot.governanceRequirementBundle());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write golden requirement snapshot artifacts", exception);
        }
    }

    private void write(Path path, Object value) throws IOException {
        objectMapper.writeValue(path.toFile(), value);
    }
}
