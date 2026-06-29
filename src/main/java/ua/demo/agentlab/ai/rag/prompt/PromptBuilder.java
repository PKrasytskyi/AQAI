package ua.demo.agentlab.ai.rag.prompt;

import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalResult;
import ua.demo.agentlab.templates.ProjectContext;

public interface PromptBuilder {

    String build(String userRequest, ContextRetrievalResult retrievalResult, ProjectContext projectContext);
}
