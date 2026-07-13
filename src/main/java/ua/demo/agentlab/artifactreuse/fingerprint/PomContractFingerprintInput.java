package ua.demo.agentlab.artifactreuse.fingerprint;

import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

import java.util.Map;

public record PomContractFingerprintInput(
        String targetPageId,
        String targetPageName,
        String route,
        String capability,
        PromptUiEvidence promptUiEvidence,
        Map<String, Object> promptMetadata,
        String promptTemplateVersion,
        String pomContractSchemaVersion,
        String modelName,
        double temperature,
        String generationMode,
        String retrievalMode,
        String writerVersion,
        PromptReadyPomScope promptScope,
        String appId,
        String baseUrlHash
) {
    public PomContractFingerprintInput(
            String targetPageId,
            String targetPageName,
            String route,
            String capability,
            PromptUiEvidence promptUiEvidence,
            Map<String, Object> promptMetadata,
            String promptTemplateVersion,
            String pomContractSchemaVersion,
            String modelName,
            double temperature,
            String generationMode,
            String retrievalMode
    ) {
        this(targetPageId, targetPageName, route, capability, promptUiEvidence, promptMetadata,
                promptTemplateVersion, pomContractSchemaVersion, modelName, temperature, generationMode,
                retrievalMode, "deterministic-pom-java-writer-v1", null, "", "");
    }

    public PomContractFingerprintInput(
            String targetPageId,
            String targetPageName,
            String route,
            String capability,
            PromptUiEvidence promptUiEvidence,
            Map<String, Object> promptMetadata,
            String promptTemplateVersion,
            String pomContractSchemaVersion,
            String modelName,
            double temperature,
            String generationMode,
            String retrievalMode,
            String writerVersion
    ) {
        this(targetPageId, targetPageName, route, capability, promptUiEvidence, promptMetadata,
                promptTemplateVersion, pomContractSchemaVersion, modelName, temperature, generationMode,
                retrievalMode, writerVersion, null, "", "");
    }

    public PomContractFingerprintInput {
        targetPageId = safe(targetPageId);
        targetPageName = safe(targetPageName);
        route = safe(route);
        capability = safe(capability);
        promptMetadata = promptMetadata == null ? Map.of() : Map.copyOf(promptMetadata);
        promptTemplateVersion = safe(promptTemplateVersion);
        pomContractSchemaVersion = safe(pomContractSchemaVersion);
        modelName = safe(modelName);
        temperature = Math.max(0.0d, temperature);
        generationMode = safe(generationMode);
        retrievalMode = safe(retrievalMode);
        writerVersion = safe(writerVersion);
        appId = safe(appId);
        baseUrlHash = safe(baseUrlHash);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
