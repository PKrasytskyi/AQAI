package ua.demo.agentlab.artifactreuse.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public class FileBackedStableArtifactStore {

    private final Path root;
    private final ObjectMapper objectMapper;

    public FileBackedStableArtifactStore(String root) {
        this(Path.of(root == null || root.isBlank() ? "target/ai-run-history/stable" : root));
    }

    public FileBackedStableArtifactStore(Path root) {
        this(root, new ObjectMapper());
    }

    FileBackedStableArtifactStore(Path root, ObjectMapper objectMapper) {
        if (root == null || objectMapper == null) {
            throw new IllegalArgumentException("artifact store dependencies cannot be null");
        }
        this.root = root;
        this.objectMapper = objectMapper;
    }

    public StableArtifactLookup findPomContract(String pageName, String fingerprint) {
        Path path = pomContractPath(pageName, fingerprint);
        return readPomContract(path);
    }

    public StableArtifactLookup findPomContract(Path path) {
        if (path == null) {
            return StableArtifactLookup.miss("stable POM contract path is missing");
        }
        return readPomContract(path);
    }

    /** Reads only a contract explicitly promoted after writer, compile, review, and smoke gates. */
    public StableArtifactLookup findValidatedPomContract(String pageName, String fingerprint) {
        Path path = pomContractPath(pageName, fingerprint);
        if (!Files.isRegularFile(validationMarkerPath(path))) {
            return StableArtifactLookup.miss("stable POM validation marker is missing: " + validationMarkerPath(path));
        }
        return readPomContract(path);
    }

    public StableArtifactWriteResult markPomContractValidated(Path contractPath) {
        if (contractPath == null || !Files.isRegularFile(contractPath)) {
            return StableArtifactWriteResult.failed("cannot mark missing POM contract as validated");
        }
        Path marker = validationMarkerPath(contractPath);
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "STABLE", StandardCharsets.UTF_8);
            return StableArtifactWriteResult.success(marker.toAbsolutePath().normalize());
        } catch (Exception exception) {
            return StableArtifactWriteResult.failed("failed to mark stable POM contract: " + exception.getMessage());
        }
    }

    private StableArtifactLookup readPomContract(Path path) {
        if (!Files.isRegularFile(path)) {
            return StableArtifactLookup.miss("stable POM contract file not found: " + path);
        }
        try {
            PomContractSpec contract = objectMapper.readValue(path.toFile(), PomContractSpec.class);
            return StableArtifactLookup.hit(contract, path.toAbsolutePath().normalize());
        } catch (Exception exception) {
            return StableArtifactLookup.miss("failed to read stable POM contract: " + exception.getMessage());
        }
    }

    public StableArtifactWriteResult writePomContract(String pageName, String fingerprint, PomContractSpec contract) {
        if (contract == null) {
            return StableArtifactWriteResult.failed("cannot write null POM contract");
        }
        Path path = pomContractPath(pageName, fingerprint);
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), contract);
            return StableArtifactWriteResult.success(path.toAbsolutePath().normalize());
        } catch (Exception exception) {
            return StableArtifactWriteResult.failed("failed to write stable POM contract: " + exception.getMessage());
        }
    }

    public Path pomContractPath(String pageName, String fingerprint) {
        return root.resolve("pom-contracts")
                .resolve(fileStem(pageName) + "." + safe(fingerprint) + ".pom-contract.json")
                .normalize();
    }

    private Path validationMarkerPath(Path contractPath) {
        return Path.of(contractPath.toString() + ".stable").normalize();
    }

    private String fileStem(String pageName) {
        String stem = safe(pageName);
        if (stem.isBlank()) {
            stem = "Page";
        }
        return stem.replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
