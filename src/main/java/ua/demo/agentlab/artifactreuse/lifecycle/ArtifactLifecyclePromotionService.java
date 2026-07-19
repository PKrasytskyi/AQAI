package ua.demo.agentlab.artifactreuse.lifecycle;

import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;
import ua.demo.agentlab.artifactreuse.model.ArtifactTarget;
import ua.demo.agentlab.artifactreuse.model.ArtifactTargetType;
import ua.demo.agentlab.artifactreuse.model.ArtifactType;
import ua.demo.agentlab.artifactreuse.model.QualityGateRecord;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistry;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteResult;
import ua.demo.agentlab.artifactreuse.store.FileBackedStableArtifactStore;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.ReviewSeverity;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArtifactLifecyclePromotionService {

    private static final String POM_SCHEMA_VERSION = "pom-contract-v1";

    private final ArtifactReuseRuntimeConfig config;
    private final ArtifactRegistry registry;
    private final FileBackedStableArtifactStore stableArtifactStore;

    public ArtifactLifecyclePromotionService(ArtifactReuseRuntimeConfig config, ArtifactRegistry registry) {
        this(config, registry, new FileBackedStableArtifactStore(config == null ? null : config.stableRoot()));
    }

    ArtifactLifecyclePromotionService(
            ArtifactReuseRuntimeConfig config,
            ArtifactRegistry registry,
            FileBackedStableArtifactStore stableArtifactStore
    ) {
        if (config == null || registry == null || stableArtifactStore == null) {
            throw new IllegalArgumentException("artifact lifecycle dependencies cannot be null");
        }
        this.config = config;
        this.registry = registry;
        this.stableArtifactStore = stableArtifactStore;
    }

    public ArtifactLifecycleResult promote(ArtifactLifecycleInput input) {
        if (input == null || input.contracts().isEmpty()) {
            return ArtifactLifecycleResult.empty();
        }
        if (!config.enabled()) {
            return skipped(input.contracts(), "artifact reuse is disabled");
        }

        List<ArtifactLifecycleEntry> entries = new ArrayList<>();
        for (PomContractSpec contract : input.contracts()) {
            entries.add(promote(contract, input));
        }
        int stable = (int) entries.stream().filter(ArtifactLifecycleEntry::reusable).count();
        return new ArtifactLifecycleResult(entries.size(), stable, entries.size() - stable, entries);
    }

    private ArtifactLifecycleResult skipped(List<PomContractSpec> contracts, String reason) {
        List<ArtifactLifecycleEntry> entries = contracts.stream()
                .map(contract -> new ArtifactLifecycleEntry(
                        contract.page().name(), "", "", ArtifactStatus.NEEDS_REVIEW,
                        false, 0.0d, reason, false, false))
                .toList();
        return new ArtifactLifecycleResult(entries.size(), 0, entries.size(), entries);
    }

    private ArtifactLifecycleEntry promote(PomContractSpec contract, ArtifactLifecycleInput input) {
        String pageName = contract.page().name();
        String prefix = "artifact.reuse." + fileStem(pageName) + ".";
        String fingerprint = value(input.artifacts(), prefix + "fingerprint");
        String stablePath = value(input.artifacts(), prefix + "stableWrite.path");
        if (stablePath.isBlank()) {
            // Reused contracts already have a validated stable-file path; they are not written again.
            stablePath = value(input.artifacts(), prefix + "file.path");
        }
        String decision = value(input.artifacts(), prefix + "decision");
        boolean writerPassed = hasGeneratedSource(pageName, input.generatedSources());
        boolean compilePassed = input.compileResult() != null && input.compileResult().isPassed();
        boolean reviewPassed = noCriticalReviewFinding(input.reviewReport());
        boolean smokePassed = input.smokeResult() != null && input.smokeResult().passed();
        String liveSmokeStatus = value(input.artifacts(), "generated.ui.live.smoke.status");
        boolean liveSmokeEnabled = Boolean.parseBoolean(value(input.artifacts(), "generated.ui.live.smoke.enabled"));
        boolean liveSmokePassedOrDisabled = !liveSmokeEnabled
                || GeneratedUiSmokeStatus.PASSED.name().equals(liveSmokeStatus);
        String generatedTestExecutionStatus = value(input.artifacts(), "generated.tests.execution.status");
        boolean generatedTestsPassedOrSkipped = "PASSED".equals(generatedTestExecutionStatus)
                || "SKIPPED".equals(generatedTestExecutionStatus);
        boolean stable = writerPassed && compilePassed && reviewPassed && smokePassed
                && liveSmokePassedOrDisabled && generatedTestsPassedOrSkipped;
        double qualityScore = qualityScore(writerPassed, compilePassed, reviewPassed, smokePassed,
                liveSmokePassedOrDisabled, generatedTestsPassedOrSkipped, input.reviewReport());
        ArtifactStatus status = stable ? ArtifactStatus.STABLE : ArtifactStatus.NEEDS_REVIEW;
        String reason = lifecycleReason(writerPassed, compilePassed, reviewPassed, smokePassed, liveSmokePassedOrDisabled,
                liveSmokeStatus, liveSmokeEnabled, generatedTestsPassedOrSkipped, generatedTestExecutionStatus);
        String artifactId = "pom-contract:" + targetId(contract) + ":" + fingerprint;

        if (fingerprint.isBlank() || stablePath.isBlank()) {
            return new ArtifactLifecycleEntry(pageName, artifactId, fingerprint, ArtifactStatus.NEEDS_REVIEW, false,
                    qualityScore, reason + "; artifact identity or stable path is missing", false, false);
        }

        if ("REUSE_STABLE".equals(decision) && stable) {
            ArtifactRegistryWriteResult registryResult = registry.register(new ArtifactRegistryWriteRequest(
                    artifactRecord(contract, fingerprint, stablePath, ArtifactStatus.STABLE, qualityScore, true, true),
                    ArtifactTarget.page(targetId(contract), pageName, contract.page().route(), contract.page().capability()),
                    input.runRecord(),
                    ArtifactRunRelation.REUSED,
                    qualityGates(artifactId, true, true, reviewPassed, smokePassed, liveSmokeStatus, liveSmokeEnabled,
                            generatedTestExecutionStatus, generatedTestsPassedOrSkipped)
            ));
            String reuseReason = "reused stable artifact passed current-run validation";
            if (!registryResult.success()) {
                reuseReason += "; Neo4j registry seed failed: " + registryResult.message();
            }
            return new ArtifactLifecycleEntry(pageName, artifactId, fingerprint, ArtifactStatus.STABLE, true,
                    qualityScore, reuseReason, registryResult.attempted(), registryResult.success());
        }

        ArtifactRegistryWriteResult registryResult = registry.register(new ArtifactRegistryWriteRequest(
                artifactRecord(contract, fingerprint, stablePath, status, qualityScore, writerPassed, compilePassed),
                ArtifactTarget.page(targetId(contract), pageName, contract.page().route(), contract.page().capability()),
                input.runRecord(),
                ArtifactRunRelation.PRODUCED,
                qualityGates(artifactId, writerPassed, compilePassed, reviewPassed, smokePassed, liveSmokeStatus,
                        liveSmokeEnabled, generatedTestExecutionStatus, generatedTestsPassedOrSkipped)
        ));
        String markerNote = "";
        if (stable) {
            var markerResult = stableArtifactStore.markPomContractValidated(java.nio.file.Path.of(stablePath));
            markerNote = markerResult.success() ? "" : "; local validation marker was not written: " + markerResult.message();
        }
        String registryNote = registryResult.success()
                ? ""
                : "; Neo4j artifact registry update failed: " + registryResult.message();
        return new ArtifactLifecycleEntry(pageName, artifactId, fingerprint, status, stable, qualityScore,
                reason + markerNote + registryNote,
                registryResult.attempted(), registryResult.success());
    }

    private ArtifactRecord artifactRecord(
            PomContractSpec contract,
            String fingerprint,
            String stablePath,
            ArtifactStatus status,
            double qualityScore,
            boolean writerPassed,
            boolean compilePassed
    ) {
        String now = Instant.now().toString();
        return new ArtifactRecord(
                "pom-contract:" + targetId(contract) + ":" + fingerprint,
                ArtifactType.POM_CONTRACT,
                ArtifactTargetType.PAGE,
                targetId(contract),
                fingerprint,
                contract.schemaVersion().isBlank() ? POM_SCHEMA_VERSION : contract.schemaVersion(),
                config.promptTemplateVersion(),
                "",
                0.0d,
                status,
                qualityScore,
                writerPassed,
                compilePassed,
                stablePath,
                now,
                now,
                0L
        );
    }

    private List<QualityGateRecord> qualityGates(
            String artifactId,
            boolean writerPassed,
            boolean compilePassed,
            boolean reviewPassed,
            boolean smokePassed,
            String liveSmokeStatus,
            boolean liveSmokeEnabled,
            String generatedTestExecutionStatus,
            boolean generatedTestsPassedOrSkipped
    ) {
        String now = Instant.now().toString();
        return List.of(
                gate(artifactId, "SCHEMA", true, "POM contract was parsed and rehydrated", now),
                gate(artifactId, "QUALITY", writerPassed, "POM contract quality gate and deterministic writer", now),
                gate(artifactId, "COMPILE", compilePassed, "generated POM compile validation", now),
                gate(artifactId, "REVIEW", reviewPassed, "generated POM review", now),
                gate(artifactId, "SMOKE", smokePassed, "generated POM smoke validation", now),
                new QualityGateRecord(artifactId + "-live-smoke", "LIVE_SMOKE",
                        liveSmokeEnabled ? (liveSmokeStatus.isBlank() ? "FAILED" : liveSmokeStatus) : "SKIPPED",
                        liveSmokeEnabled ? "live browser smoke status" : "live browser smoke disabled", 0, now),
                new QualityGateRecord(artifactId + "-generated-test-execution", "GENERATED_TEST_EXECUTION",
                        generatedTestExecutionStatus.isBlank() ? "MISSING" : generatedTestExecutionStatus,
                        "manifest-owned generated TestNG execution",
                        generatedTestsPassedOrSkipped ? 0 : 1, now)
        );
    }

    private QualityGateRecord gate(String artifactId, String type, boolean passed, String summary, String now) {
        return new QualityGateRecord(artifactId + "-" + type.toLowerCase(), type,
                passed ? "PASSED" : "FAILED", summary, passed ? 0 : 1, now);
    }

    private boolean hasGeneratedSource(String pageName, List<GeneratedSourceFile> sources) {
        return sources.stream().anyMatch(source -> pageName.equals(source.className()));
    }

    private boolean noCriticalReviewFinding(GeneratedCodeReviewReport report) {
        return report == null || report.findings() == null || report.findings().stream()
                .noneMatch(finding -> finding.severity() == ReviewSeverity.CRITICAL);
    }

    private double qualityScore(
            boolean writerPassed,
            boolean compilePassed,
            boolean reviewPassed,
            boolean smokePassed,
            boolean liveSmokePassedOrDisabled,
            boolean generatedTestsPassedOrSkipped,
            GeneratedCodeReviewReport reviewReport
    ) {
        double score = 0.0d;
        score += writerPassed ? 20.0d : 0.0d;
        score += compilePassed ? 30.0d : 0.0d;
        score += reviewPassed ? 20.0d : 0.0d;
        score += smokePassed ? 20.0d : 0.0d;
        score += liveSmokePassedOrDisabled ? 10.0d : 0.0d;
        if (reviewReport != null && reviewReport.findings() != null) {
            long warnings = reviewReport.findings().stream()
                    .filter(finding -> finding.severity() == ReviewSeverity.WARNING)
                    .count();
            score -= Math.min(10.0d, warnings * 2.0d);
        }
        if (!generatedTestsPassedOrSkipped) {
            score = Math.min(score, 85.0d);
        }
        return Math.max(0.0d, score);
    }

    private String lifecycleReason(
            boolean writerPassed,
            boolean compilePassed,
            boolean reviewPassed,
            boolean smokePassed,
            boolean liveSmokePassedOrDisabled,
            String liveSmokeStatus,
            boolean liveSmokeEnabled,
            boolean generatedTestsPassedOrSkipped,
            String generatedTestExecutionStatus
    ) {
        List<String> failures = new ArrayList<>();
        if (!writerPassed) {
            failures.add("writer did not produce the page source");
        }
        if (!compilePassed) {
            failures.add("compile validation did not pass");
        }
        if (!reviewPassed) {
            failures.add("review contains critical findings");
        }
        if (!smokePassed) {
            failures.add("generated smoke validation did not pass");
        }
        if (!liveSmokePassedOrDisabled) {
            failures.add("live smoke is enabled and status is " + (liveSmokeStatus.isBlank() ? "missing" : liveSmokeStatus));
        }
        if (!generatedTestsPassedOrSkipped) {
            failures.add("generated TestNG execution status is "
                    + (generatedTestExecutionStatus.isBlank() ? "missing" : generatedTestExecutionStatus));
        }
        return failures.isEmpty()
                ? "writer, compile, review, smoke, and generated test execution gates passed"
                : String.join("; ", failures);
    }

    private String targetId(PomContractSpec contract) {
        return contract.page().name().isBlank() ? contract.page().route() : contract.page().name();
    }

    private String fileStem(String value) {
        String safe = value == null ? "" : value.trim();
        return (safe.isBlank() ? "Page" : safe).replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private String value(Map<String, String> values, String key) {
        if (values == null || key == null) {
            return "";
        }
        String value = values.get(key);
        return value == null ? "" : value.trim();
    }

    public record ArtifactLifecycleInput(
            List<PomContractSpec> contracts,
            List<GeneratedSourceFile> generatedSources,
            GeneratedCodeValidationResult compileResult,
            GeneratedCodeReviewReport reviewReport,
            GeneratedUiSmokeResult smokeResult,
            Map<String, String> artifacts,
            RunRecord runRecord
    ) {
        public ArtifactLifecycleInput {
            contracts = contracts == null ? List.of() : List.copyOf(contracts);
            generatedSources = generatedSources == null ? List.of() : List.copyOf(generatedSources);
            artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
            runRecord = runRecord == null ? new RunRecord("", "", "", "", "", "", "", "") : runRecord;
        }
    }
}
