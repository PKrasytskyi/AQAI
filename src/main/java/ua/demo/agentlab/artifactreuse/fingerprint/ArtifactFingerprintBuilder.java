package ua.demo.agentlab.artifactreuse.fingerprint;

public interface ArtifactFingerprintBuilder<T> {

    ArtifactFingerprint build(T input);
}
