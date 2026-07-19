package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.fingerprint.ArtifactFingerprint;
import ua.demo.agentlab.artifactreuse.model.ArtifactType;
import ua.demo.agentlab.artifactreuse.policy.*;
import ua.demo.agentlab.artifactreuse.registry.*;
import ua.demo.agentlab.artifactreuse.store.*;

import java.nio.file.Path;

/** Coordinates registry lookup, validated file fallback, and the reuse policy decision. */
public final class PomArtifactReuseCoordinator {
    private final ArtifactReuseRuntimeConfig config;
    private final ArtifactRegistry registry;
    private final FileBackedStableArtifactStore store;
    private final ArtifactReusePolicy policy;

    public PomArtifactReuseCoordinator(ArtifactReuseRuntimeConfig config, ArtifactRegistry registry,
                                       FileBackedStableArtifactStore store, ArtifactReusePolicy policy) {
        this.config = config;
        this.registry = registry;
        this.store = store;
        this.policy = policy;
    }

    public PomArtifactReuseEvaluation evaluate(AiPageObjectPromptScope scope, String targetId,
                                               ArtifactFingerprint fingerprint, String schemaVersion) {
        ArtifactLookupResult registryLookup = registry.findStableArtifact(new ArtifactLookupRequest(
                ArtifactType.POM_CONTRACT, targetId, fingerprint.value(), schemaVersion));
        StableArtifactLookup stable = stableLookup(registryLookup, scope, fingerprint);
        ArtifactReuseDecision decision = policy.decide(new ArtifactReusePolicyInput(
                config.enabled() && config.pomContractEnabled(), config.forceRefresh(),
                registryLookup.hit() || stable.hit(), stable.hit(),
                registryLookup.artifact() == null ? (stable.hit() ? "STABLE" : "")
                        : registryLookup.artifact().status().name(),
                schemaVersion, registryLookup.artifact() == null ? "" : registryLookup.artifact().schemaVersion()));
        return new PomArtifactReuseEvaluation(registryLookup, stable, decision);
    }

    private StableArtifactLookup stableLookup(ArtifactLookupResult registryLookup, AiPageObjectPromptScope scope,
                                               ArtifactFingerprint fingerprint) {
        if (registryLookup != null && registryLookup.hit() && registryLookup.artifact() != null
                && !registryLookup.artifact().filePath().isBlank()) {
            StableArtifactLookup byPath = store.findPomContract(Path.of(registryLookup.artifact().filePath()));
            if (byPath.hit()) return byPath;
        }
        return store.findValidatedPomContract(scope.pageName(), fingerprint.value());
    }

    public record PomArtifactReuseEvaluation(ArtifactLookupResult registryLookup,
                                              StableArtifactLookup stableLookup,
                                              ArtifactReuseDecision decision) {}
}
