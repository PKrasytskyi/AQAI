package ua.demo.agentlab.artifactreuse.registry;

public record ArtifactRegistryWriteResult(
        boolean attempted,
        boolean success,
        String backend,
        int statements,
        String message
) {
    public static ArtifactRegistryWriteResult skipped(String backend, String message) {
        return new ArtifactRegistryWriteResult(false, false, backend, 0, message);
    }

    public static ArtifactRegistryWriteResult success(String backend, int statements, String message) {
        return new ArtifactRegistryWriteResult(true, true, backend, statements, message);
    }

    public static ArtifactRegistryWriteResult failed(String backend, int statements, String message) {
        return new ArtifactRegistryWriteResult(true, false, backend, statements, message);
    }
}
