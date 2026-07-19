package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.fingerprint.ArtifactFingerprint;
import ua.demo.agentlab.artifactreuse.model.*;
import ua.demo.agentlab.artifactreuse.registry.*;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/** Persists POM artifact provenance in the registry without changing generation decisions. */
public final class PomArtifactRegistrationService {
    private final OpenAiRuntimeConfig runtimeConfig;
    private final ArtifactReuseRuntimeConfig reuseConfig;
    private final ArtifactRegistry registry;

    public PomArtifactRegistrationService(OpenAiRuntimeConfig runtimeConfig,
                                          ArtifactReuseRuntimeConfig reuseConfig,
                                          ArtifactRegistry registry) {
        this.runtimeConfig = runtimeConfig;
        this.reuseConfig = reuseConfig;
        this.registry = registry;
    }

    public ArtifactRegistryWriteResult register(Registration input) {
        ArtifactRecord existing = input.existing();
        boolean reused = input.relation() == ArtifactRunRelation.REUSED;
        String artifactId = existing != null && !existing.artifactId().isBlank()
                ? existing.artifactId() : input.targetId() + ":pom-contract:" + input.fingerprint().value();
        ArtifactRecord artifact = new ArtifactRecord(
                artifactId, ArtifactType.POM_CONTRACT, ArtifactTargetType.PAGE, input.targetId(),
                input.fingerprint().value(), input.contract().schemaVersion(), reuseConfig.promptTemplateVersion(),
                existing != null && !existing.model().isBlank() ? existing.model() : runtimeConfig.model(),
                existing == null ? 0.0d : existing.temperature(),
                reused ? ArtifactStatus.STABLE : ArtifactStatus.SCHEMA_VALIDATED,
                existing == null ? 0.0d : existing.qualityScore(), reused, reused,
                input.filePath() == null ? "" : input.filePath().toString(),
                existing == null ? Instant.now().toString() : existing.createdAt(), Instant.now().toString(),
                existing == null ? (reused ? 1L : 0L) : existing.reuseCount() + (reused ? 1L : 0L));
        String gateType = reused ? "REUSE" : "SCHEMA";
        return registry.register(new ArtifactRegistryWriteRequest(artifact,
                ArtifactTarget.page(input.targetId(), input.pageName(), input.route(), input.capability()),
                runRecord(input.request()), input.relation(), List.of(new QualityGateRecord(
                artifactId + "-" + gateType.toLowerCase(java.util.Locale.ROOT), gateType, "PASSED",
                reused ? "stable POM contract reused and queued for current-run validation"
                        : "pom-contract parsed and rehydrated",
                0, Instant.now().toString()))));
    }

    private RunRecord runRecord(AiPageObjectGenerationRequest request) {
        KnowledgeRunMetadata metadata = request == null || request.qualitySummaryInput() == null
                ? null : request.qualitySummaryInput().knowledgeRunMetadata();
        if (metadata == null) {
            return new RunRecord("", "", "", "", "", "", "", Instant.now().toString());
        }
        return new RunRecord(metadata.runId(), metadata.appId(), metadata.baseUrlHash(),
                metadata.requirementSetHash(), metadata.discoverySessionId(), metadata.schemaVersion(),
                metadata.createdAt(), metadata.sourceAgent());
    }

    public record Registration(String targetId, String pageName, String route, String capability,
                               ArtifactFingerprint fingerprint, PomContractSpec contract, Path filePath,
                               AiPageObjectGenerationRequest request, ArtifactRunRelation relation,
                               ArtifactRecord existing) {
    }
}
