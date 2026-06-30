package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.api.agent.ApiGeneratedSourcePersistenceAgent;
import ua.demo.agentlab.api.agent.ApiGenerationAgent;
import ua.demo.agentlab.persistence.GeneratedFileWriter;

public class ApiModuleFactory {

    public ApiModule create(GeneratedFileWriter generatedFileWriter) {
        if (generatedFileWriter == null) {
            throw new IllegalArgumentException("generatedFileWriter cannot be null");
        }
        return new ApiModule(
                new ApiGenerationAgent(),
                new ApiGeneratedSourcePersistenceAgent(generatedFileWriter)
        );
    }
}
