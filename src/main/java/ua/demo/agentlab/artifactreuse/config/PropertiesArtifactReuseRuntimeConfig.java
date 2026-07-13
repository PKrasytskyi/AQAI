package ua.demo.agentlab.artifactreuse.config;

import ua.demo.agentlab.config.RuntimeProperties;

public class PropertiesArtifactReuseRuntimeConfig implements ArtifactReuseRuntimeConfig {

    private final RuntimeProperties properties;

    public PropertiesArtifactReuseRuntimeConfig() {
        this("framework.properties");
    }

    public PropertiesArtifactReuseRuntimeConfig(String resourceName) {
        this.properties = new RuntimeProperties(resourceName);
    }

    @Override
    public boolean enabled() {
        return properties.readKnowledgeDbBoolean("artifact.reuse.enabled", "true");
    }

    @Override
    public boolean forceRefresh() {
        return properties.readBoolean("artifact.reuse.force-refresh", "false");
    }

    @Override
    public String stableRoot() {
        return properties.readValue("artifact.reuse.stable-root", "target/ai-run-history/stable");
    }

    @Override
    public boolean flowContractEnabled() {
        return properties.readKnowledgeDbBoolean("artifact.reuse.flow-contract.enabled", "false");
    }

    @Override
    public boolean pomContractEnabled() {
        return properties.readKnowledgeDbBoolean("artifact.reuse.pom-contract.enabled", "true");
    }

    @Override
    public boolean testDataEnabled() {
        return properties.readKnowledgeDbBoolean("artifact.reuse.test-data.enabled", "false");
    }

    @Override
    public boolean semanticReuseEnabled() {
        return properties.readKnowledgeDbBoolean("semantic.reuse.enabled", "false");
    }

    @Override
    public boolean explainDecisions() {
        return properties.readBoolean("artifact.reuse.explain-decisions", "true");
    }

    @Override
    public String policy() {
        return properties.readValue("artifact.reuse.policy", "strict");
    }

    @Override
    public String writerVersion() {
        return properties.readValue("artifact.reuse.writer-version", "deterministic-pom-java-writer-v1");
    }

    @Override
    public String promptTemplateVersion() {
        return properties.readValue("artifact.reuse.prompt-template-version", "pom-json-generation-v1");
    }
}
