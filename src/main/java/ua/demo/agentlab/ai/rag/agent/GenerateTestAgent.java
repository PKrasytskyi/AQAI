package ua.demo.agentlab.ai.rag.agent;

import ua.demo.agentlab.ai.rag.model.RagGenerationResult;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.rag.prompt.PromptBuilder;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalRequest;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalResult;
import ua.demo.agentlab.ai.rag.retrieval.HybridContextRetriever;
import ua.demo.agentlab.templates.ProjectContext;

public class GenerateTestAgent {

    private final HybridContextRetriever hybridContextRetriever;
    private final PromptBuilder promptBuilder;
    private final OpenAiResponseGenerationClient generationClient;
    private final ProjectContext projectContext;

    public GenerateTestAgent(
            HybridContextRetriever hybridContextRetriever,
            PromptBuilder promptBuilder,
            OpenAiResponseGenerationClient generationClient,
            ProjectContext projectContext
    ) {
        if (hybridContextRetriever == null || promptBuilder == null || generationClient == null || projectContext == null) {
            throw new IllegalArgumentException("GenerateTestAgent dependencies cannot be null");
        }
        this.hybridContextRetriever = hybridContextRetriever;
        this.promptBuilder = promptBuilder;
        this.generationClient = generationClient;
        this.projectContext = projectContext;
    }

    public RagGenerationResult generate(String userRequest) {
        ContextRetrievalResult retrievalResult = hybridContextRetriever.retrieve(new ContextRetrievalRequest(userRequest, 6));
        String prompt = promptBuilder.build(userRequest, retrievalResult, projectContext);
        String generatedText = "LLM test generation is disabled. Prompt was built for review only; LLM is currently enabled only for knowledge enrichment.";
        return new RagGenerationResult(prompt, retrievalResult.contextChunks(), generatedText, retrievalResult.retrievalTrace());
    }
}
