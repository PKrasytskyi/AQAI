package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.artifactreuse.config.ArtifactReuseRuntimeConfig;
import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecyclePromotionService;
import ua.demo.agentlab.artifactreuse.lifecycle.ArtifactLifecycleResult;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactStatus;
import ua.demo.agentlab.artifactreuse.model.RunRecord;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactLookupResult;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistry;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteRequest;
import ua.demo.agentlab.artifactreuse.registry.ArtifactRegistryWriteResult;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedFileValidation;
import ua.demo.agentlab.validation.ValidationStatus;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult;
import ua.demo.agentlab.validation.smoke.GeneratedUiSmokeStatus;

import java.util.List;
import java.util.Map;

public class ArtifactLifecyclePromotionServiceTest {

    @Test
    public void promotesProducedArtifactOnlyAfterAllValidationGatesPass() {
        CapturingRegistry registry = new CapturingRegistry();
        ArtifactLifecycleResult result = service(registry).promote(input(
                passingCompile(),
                passingSmoke(),
                Map.of(
                        "artifact.reuse.LoginPage.fingerprint", "fingerprint-1",
                        "artifact.reuse.LoginPage.stableWrite.path", "target/stable/LoginPage.fingerprint-1.json",
                        "generated.ui.live.smoke.status", "PASSED"
                )
        ));

        Assert.assertEquals(result.stableArtifacts(), 1);
        Assert.assertEquals(result.entries().get(0).status(), ArtifactStatus.STABLE);
        Assert.assertTrue(result.entries().get(0).registryUpdated());
        Assert.assertNotNull(registry.request);
        Assert.assertEquals(registry.request.artifact().status(), ArtifactStatus.STABLE);
        Assert.assertTrue(registry.request.artifact().writerSucceeded());
        Assert.assertTrue(registry.request.artifact().compileSucceeded());
    }

    @Test
    public void keepsArtifactOutOfReuseWhenCompileDoesNotPass() {
        CapturingRegistry registry = new CapturingRegistry();
        GeneratedCodeValidationResult failedCompile = new GeneratedCodeValidationResult(
                ValidationStatus.FAILED, "compile failed", "error", List.of());
        ArtifactLifecycleResult result = service(registry).promote(input(
                failedCompile,
                passingSmoke(),
                Map.of(
                        "artifact.reuse.LoginPage.fingerprint", "fingerprint-1",
                        "artifact.reuse.LoginPage.stableWrite.path", "target/stable/LoginPage.fingerprint-1.json"
                )
        ));

        Assert.assertEquals(result.stableArtifacts(), 0);
        Assert.assertEquals(result.entries().get(0).status(), ArtifactStatus.NEEDS_REVIEW);
        Assert.assertFalse(result.entries().get(0).reusable());
        Assert.assertNotNull(registry.request);
        Assert.assertEquals(registry.request.artifact().status(), ArtifactStatus.NEEDS_REVIEW);
    }

    @Test
    public void preservesPreviouslyStableArtifactWhenCurrentRunReusesIt() {
        CapturingRegistry registry = new CapturingRegistry();
        ArtifactLifecycleResult result = service(registry).promote(input(
                passingCompile(),
                passingSmoke(),
                Map.of(
                        "artifact.reuse.LoginPage.fingerprint", "fingerprint-1",
                        "artifact.reuse.LoginPage.stableWrite.path", "target/stable/LoginPage.fingerprint-1.json",
                        "artifact.reuse.LoginPage.decision", "REUSE_STABLE"
                )
        ));

        Assert.assertEquals(result.entries().get(0).status(), ArtifactStatus.STABLE);
        Assert.assertTrue(result.entries().get(0).reusable());
        Assert.assertNotNull(registry.request,
                "A locally reused stable artifact must seed the registry after current-run validation passes");
        Assert.assertEquals(registry.request.runRelation(), ArtifactRunRelation.REUSED);
        Assert.assertEquals(registry.request.artifact().status(), ArtifactStatus.STABLE);
    }

    @Test
    public void preservesPreviouslyStableArtifactWhenOnlyLookupFilePathIsAvailable() {
        CapturingRegistry registry = new CapturingRegistry();
        ArtifactLifecycleResult result = service(registry).promote(input(
                passingCompile(),
                passingSmoke(),
                Map.of(
                        "artifact.reuse.LoginPage.fingerprint", "fingerprint-1",
                        "artifact.reuse.LoginPage.file.path", "target/stable/LoginPage.fingerprint-1.json",
                        "artifact.reuse.LoginPage.decision", "REUSE_STABLE"
                )
        ));

        Assert.assertEquals(result.stableArtifacts(), 1);
        Assert.assertTrue(result.entries().get(0).reusable());
        Assert.assertEquals(registry.request.runRelation(), ArtifactRunRelation.REUSED);
    }

    @Test
    public void doesNotPromoteWhenConfiguredLiveSmokeWasSkipped() {
        CapturingRegistry registry = new CapturingRegistry();
        ArtifactLifecycleResult result = service(registry).promote(input(
                passingCompile(),
                passingSmoke(),
                Map.of(
                        "artifact.reuse.LoginPage.fingerprint", "fingerprint-1",
                        "artifact.reuse.LoginPage.stableWrite.path", "target/stable/LoginPage.fingerprint-1.json",
                        "generated.ui.live.smoke.enabled", "true",
                        "generated.ui.live.smoke.status", "SKIPPED"
                )
        ));

        Assert.assertEquals(result.entries().get(0).status(), ArtifactStatus.NEEDS_REVIEW);
        Assert.assertFalse(result.entries().get(0).reusable());
        Assert.assertTrue(result.entries().get(0).reason().contains("live smoke is enabled"));
    }

    private ArtifactLifecyclePromotionService service(CapturingRegistry registry) {
        return new ArtifactLifecyclePromotionService(new EnabledConfig(), registry);
    }

    private ArtifactLifecyclePromotionService.ArtifactLifecycleInput input(
            GeneratedCodeValidationResult compile,
            GeneratedUiSmokeResult smoke,
            Map<String, String> artifacts
    ) {
        return new ArtifactLifecyclePromotionService.ArtifactLifecycleInput(
                List.of(contract()),
                List.of(new GeneratedSourceFile("generated.pages", "LoginPage", "LoginPage.java", "class LoginPage {}")),
                compile,
                new GeneratedCodeReviewReport("review ok", 1, 0, List.of()),
                smoke,
                artifacts,
                new RunRecord("run-1", "app", "base", "requirements", "discovery", "knowledge-v1",
                        "2026-07-11T00:00:00Z", "test")
        );
    }

    private GeneratedCodeValidationResult passingCompile() {
        return new GeneratedCodeValidationResult(ValidationStatus.PASSED, "compile ok", "",
                List.of(new GeneratedFileValidation("LoginPage.java", ValidationStatus.PASSED, "ok")));
    }

    private GeneratedUiSmokeResult passingSmoke() {
        return new GeneratedUiSmokeResult(GeneratedUiSmokeStatus.PASSED, "smoke ok", 1, List.of());
    }

    private PomContractSpec contract() {
        return new PomContractSpec("pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION", "openLogin"),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private static final class EnabledConfig implements ArtifactReuseRuntimeConfig {
        @Override
        public boolean enabled() {
            return true;
        }

        @Override
        public boolean forceRefresh() {
            return false;
        }

        @Override
        public String stableRoot() {
            return "target/stable";
        }
    }

    private static final class CapturingRegistry implements ArtifactRegistry {
        private ArtifactRegistryWriteRequest request;

        @Override
        public ArtifactRegistryWriteResult register(ArtifactRegistryWriteRequest request) {
            this.request = request;
            return ArtifactRegistryWriteResult.success("test", 1, "ok");
        }

        @Override
        public ArtifactLookupResult findStableArtifact(ArtifactLookupRequest request) {
            return ArtifactLookupResult.miss("test", "not used");
        }
    }
}
