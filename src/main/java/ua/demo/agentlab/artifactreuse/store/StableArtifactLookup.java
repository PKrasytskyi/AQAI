package ua.demo.agentlab.artifactreuse.store;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;

import java.nio.file.Path;

public record StableArtifactLookup(
        boolean hit,
        PomContractSpec pomContract,
        Path path,
        String message
) {
    public StableArtifactLookup {
        message = message == null ? "" : message.trim();
    }

    public static StableArtifactLookup miss(String message) {
        return new StableArtifactLookup(false, null, null, message);
    }

    public static StableArtifactLookup hit(PomContractSpec contract, Path path) {
        return new StableArtifactLookup(true, contract, path, "stable artifact found");
    }
}
