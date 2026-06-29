package ua.demo.agentlab.ai.rag.service;

import ua.demo.agentlab.ai.rag.agent.GenerateTestAgent;
import ua.demo.agentlab.ai.rag.model.RagGenerationResult;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.rag.prompt.PromptBuilder;
import ua.demo.agentlab.templates.ProjectContext;

public class RagTestGenerationService {

    private final GenerateTestAgent generateTestAgent;

    public RagTestGenerationService(
            RagRetrievalService retrievalService,
            PromptBuilder promptBuilder,
            OpenAiResponseGenerationClient generationClient,
            ProjectContext projectContext
    ) {
        if (retrievalService == null || promptBuilder == null || generationClient == null || projectContext == null) {
            throw new IllegalArgumentException("RAG generation dependencies cannot be null");
        }
        this.generateTestAgent = new GenerateTestAgent(
                retrievalService.hybridContextRetriever(),
                promptBuilder,
                generationClient,
                projectContext
        );
    }

    public RagGenerationResult generate(String userRequest) {
        return generateTestAgent.generate(userRequest);
    }
}
