package ua.demo.agentlab.persistence;

import ua.demo.agentlab.config.ProjectProfile;

public record GeneratedSourcePersistenceInput(
        ProjectProfile projectProfile,
        GeneratedUiSources sources
) {
    public GeneratedSourcePersistenceInput {
        if (projectProfile == null) {
            throw new IllegalArgumentException("projectProfile cannot be null");
        }
        sources = sources == null ? new GeneratedUiSources(null, null) : sources;
    }
}
