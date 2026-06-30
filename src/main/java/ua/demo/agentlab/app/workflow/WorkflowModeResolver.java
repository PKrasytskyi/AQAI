package ua.demo.agentlab.app.workflow;

import ua.demo.agentlab.ai.openai.PropertiesOpenAiRuntimeConfig;

import java.util.Arrays;

public class WorkflowModeResolver {

    public WorkflowMode resolve(String[] args) {
        if (args != null && Arrays.stream(args).anyMatch("--deterministic"::equalsIgnoreCase)) {
            return WorkflowMode.DETERMINISTIC;
        }
        if (args != null && Arrays.stream(args).anyMatch("--ai"::equalsIgnoreCase)) {
            return WorkflowMode.AI_PROMPT;
        }
        PropertiesOpenAiRuntimeConfig config = new PropertiesOpenAiRuntimeConfig();
        return config.enabled() && config.apiKey() != null && !config.apiKey().isBlank()
                ? WorkflowMode.AI_PROMPT
                : WorkflowMode.DETERMINISTIC;
    }
}
