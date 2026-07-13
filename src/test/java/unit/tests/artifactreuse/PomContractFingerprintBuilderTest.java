package unit.tests.artifactreuse;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.PromptActionEvidence;
import ua.demo.agentlab.ai.context.PromptAssertionEvidence;
import ua.demo.agentlab.ai.context.PromptLocatorEvidence;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.artifactreuse.fingerprint.ArtifactFingerprint;
import ua.demo.agentlab.artifactreuse.fingerprint.PomContractFingerprintBuilder;
import ua.demo.agentlab.artifactreuse.fingerprint.PomContractFingerprintInput;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;

import java.util.List;
import java.util.Map;

public class PomContractFingerprintBuilderTest {

    @Test
    public void producesSameFingerprintForEquivalentEvidenceInDifferentOrder() {
        PomContractFingerprintBuilder builder = new PomContractFingerprintBuilder();

        ArtifactFingerprint first = builder.build(input(List.of(username(), password())));
        ArtifactFingerprint second = builder.build(input(List.of(password(), username())));

        Assert.assertFalse(first.value().isBlank());
        Assert.assertEquals(first.value(), second.value());
        Assert.assertFalse(first.canonicalSource().contains("runId"));
        Assert.assertFalse(first.canonicalSource().contains("createdAt"));
    }

    @Test
    public void changesFingerprintWhenExpectedRouteChanges() {
        PomContractFingerprintBuilder builder = new PomContractFingerprintBuilder();

        ArtifactFingerprint first = builder.build(input("/dashboard/index"));
        ArtifactFingerprint second = builder.build(input("/secure"));

        Assert.assertNotEquals(first.value(), second.value());
    }

    @Test
    public void doesNotChangeFingerprintForRequirementOrRetrievalMetadataOutsideThePomContract() {
        PomContractFingerprintBuilder builder = new PomContractFingerprintBuilder();

        ArtifactFingerprint first = builder.build(input(List.of(username(), password())));
        ArtifactFingerprint second = builder.build(new PomContractFingerprintInput(
                "LoginPage", "LoginPage", "/auth/login", "AUTHENTICATION",
                new PromptUiEvidence("LoginPage", "/auth/login", false, List.of(), List.of("REQ-999"),
                        List.of(new PromptActionEvidence("login", "AUTHENTICATE", "LoginPage", "requirement")),
                        List.of(new PromptAssertionEvidence("URL_CONTAINS", "/auth/login", "LoginPage", "requirement", 1.0d)),
                        List.of(password(), username()), List.of(), List.of(), List.of(), List.of(), List.of("changed"), 1.0d),
                Map.of("promptMode", "compact"), "pom-json-generation-v1", "pom-contract-v1", "gpt-5-mini",
                0.0d, "llm-pom-contract", "repository-code-rag"));

        Assert.assertEquals(first.value(), second.value());
    }

    @Test
    public void isolatesStablePomArtifactsByApplicationNamespace() {
        PomContractFingerprintBuilder builder = new PomContractFingerprintBuilder();

        ArtifactFingerprint first = builder.build(namespacedInput("orangehrm", "base-a"));
        ArtifactFingerprint second = builder.build(namespacedInput("the-internet", "base-b"));

        Assert.assertNotEquals(first.value(), second.value());
    }

    private PomContractFingerprintInput input(List<PromptLocatorEvidence> locators) {
        return new PomContractFingerprintInput(
                "LoginPage",
                "LoginPage",
                "/auth/login",
                "AUTHENTICATION",
                new PromptUiEvidence(
                        "LoginPage",
                        "/auth/login",
                        false,
                        List.of(),
                        List.of("REQ-001"),
                        List.of(new PromptActionEvidence("login", "AUTHENTICATE", "LoginPage", "requirement")),
                        List.of(new PromptAssertionEvidence("URL_CONTAINS", "/auth/login", "LoginPage", "requirement", 1.0d)),
                        locators,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("test"),
                        1.0d
                ),
                Map.of("promptMode", "compact"),
                "pom-json-generation-v1",
                "pom-contract-v1",
                "gpt-5-mini",
                0.0d,
                "llm-pom-contract",
                "stable-page-cache"
        );
    }

    private PomContractFingerprintInput input(String route) {
        return new PomContractFingerprintInput(
                "DashboardPage",
                "DashboardPage",
                route,
                "AUTHENTICATED_AREA",
                new PromptUiEvidence(
                        "DashboardPage",
                        route,
                        false,
                        List.of("LoginPage"),
                        List.of("REQ-017"),
                        List.of(),
                        List.of(new PromptAssertionEvidence("URL_CONTAINS", route, "DashboardPage", "requirement", 1.0d)),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("test"),
                        1.0d
                ),
                Map.of("promptMode", "compact"),
                "pom-json-generation-v1",
                "pom-contract-v1",
                "gpt-5-mini",
                0.0d,
                "llm-pom-contract",
                "stable-page-cache"
        );
    }

    private PromptLocatorEvidence username() {
        return new PromptLocatorEvidence("username", "username", "name", "username", "input", "", "",
                true, 0.88d, "LoginForm", "FORM", 1, 1, true, LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("mapper"));
    }

    private PomContractFingerprintInput namespacedInput(String appId, String baseUrlHash) {
        return new PomContractFingerprintInput(
                "LoginPage", "LoginPage", "/login", "AUTHENTICATION",
                new PromptUiEvidence("LoginPage", "/login", false, List.of(), List.of("REQ-1"),
                        List.of(new PromptActionEvidence("login", "AUTHENTICATE", "LoginPage", "requirement")),
                        List.of(new PromptAssertionEvidence("URL_CONTAINS", "/login", "LoginPage", "requirement", 1.0d)),
                        List.of(username(), password()), List.of(), List.of(), List.of(), List.of(), List.of(), 1.0d),
                Map.of("promptMode", "compact"), "pom-json-generation-v2", "pom-contract-v1", "gpt-5-mini",
                0.0d, "llm-pom-contract", "stable-page-cache", "deterministic-pom-java-writer-v1", null,
                appId, baseUrlHash);
    }

    private PromptLocatorEvidence password() {
        return new PromptLocatorEvidence("password", "password", "name", "password", "password", "", "",
                true, 0.88d, "LoginForm", "FORM", 1, 1, true, LocatorEvidenceType.CONFIRMED_LOCATOR, List.of("mapper"));
    }
}
