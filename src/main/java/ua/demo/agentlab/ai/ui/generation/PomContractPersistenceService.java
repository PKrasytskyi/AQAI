package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.artifactreuse.fingerprint.ArtifactFingerprint;
import ua.demo.agentlab.artifactreuse.store.FileBackedStableArtifactStore;
import ua.demo.agentlab.artifactreuse.store.StableArtifactWriteResult;

import java.util.ArrayList;
import java.util.List;

/** Writes current-run and stable contract artifacts after validation. */
public final class PomContractPersistenceService {
    private final AiPageObjectPromptArtifactWriter artifactWriter;
    private final FileBackedStableArtifactStore stableStore;

    public PomContractPersistenceService(AiPageObjectPromptArtifactWriter artifactWriter,
                                         FileBackedStableArtifactStore stableStore) {
        this.artifactWriter = artifactWriter;
        this.stableStore = stableStore;
    }

    public PomContractPersistenceResult persistGenerated(AiPageObjectPromptScope scope,
                                                          ArtifactFingerprint fingerprint,
                                                          PomContractSpec contract) {
        List<String> files = new ArrayList<>();
        files.add(artifactWriter.writeJson(scope.fileStem() + "-pom-contract.json", contract));
        StableArtifactWriteResult stable = stableStore.writePomContract(scope.pageName(), fingerprint.value(), contract);
        if (stable.success() && stable.path() != null) files.add(stable.path().toString());
        return new PomContractPersistenceResult(List.copyOf(files), stable);
    }

    public String persistReused(AiPageObjectPromptScope scope, PomContractSpec contract) {
        return artifactWriter.writeJson(scope.fileStem() + "-pom-contract.json", contract);
    }

    public record PomContractPersistenceResult(List<String> artifactFiles, StableArtifactWriteResult stableWrite) {}
}
