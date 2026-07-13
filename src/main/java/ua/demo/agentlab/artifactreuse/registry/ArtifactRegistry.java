package ua.demo.agentlab.artifactreuse.registry;

public interface ArtifactRegistry {

    ArtifactRegistryWriteResult register(ArtifactRegistryWriteRequest request);

    default ArtifactLookupResult findStableArtifact(ArtifactLookupRequest request) {
        return ArtifactLookupResult.skipped("none", "artifact lookup is not implemented");
    }
}
