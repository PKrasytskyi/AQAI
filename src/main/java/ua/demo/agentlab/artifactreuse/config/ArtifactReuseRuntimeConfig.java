package ua.demo.agentlab.artifactreuse.config;

public interface ArtifactReuseRuntimeConfig {

    boolean enabled();

    boolean forceRefresh();

    String stableRoot();

    default boolean flowContractEnabled() {
        return false;
    }

    default boolean pomContractEnabled() {
        return true;
    }

    default boolean testDataEnabled() {
        return false;
    }

    default boolean semanticReuseEnabled() {
        return false;
    }

    default boolean explainDecisions() {
        return true;
    }

    default String policy() {
        return "strict";
    }

    default String writerVersion() {
        return "deterministic-pom-java-writer-v1";
    }

    default String promptTemplateVersion() {
        return "pom-json-generation-v1";
    }
}
