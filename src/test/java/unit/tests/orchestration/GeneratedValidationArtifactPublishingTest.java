package unit.tests.orchestration;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.ui.agent.PomContractPageObjectWriterAgent;
import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckSpec;
import ua.demo.agentlab.ai.ui.contract.PomCheckType;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomLocatorSpec;
import ua.demo.agentlab.ai.ui.contract.PomPageSpec;
import ua.demo.agentlab.ai.ui.contract.PomStepAction;
import ua.demo.agentlab.ai.ui.contract.PomStepSpec;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.persistence.GeneratedUiSources;
import ua.demo.agentlab.persistence.LocalFilePersistenceAgent;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.review.GeneratedCodeReviewReport;
import ua.demo.agentlab.review.agent.GeneratedCodeReviewAgent;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.UiAssertionProfile;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.validation.GeneratedCodeValidationResult;
import ua.demo.agentlab.validation.GeneratedFileValidation;
import ua.demo.agentlab.validation.ValidationStatus;
import ua.demo.agentlab.validation.agent.GeneratedCodeCompileAgent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GeneratedValidationArtifactPublishingTest {

    @Test
    public void compileAndReviewAgentsPublishExplicitValidationArtifacts() {
        WorkflowState state = new WorkflowState("objective", new RequirementInput(SourceType.FILE, "requirements.md"));
        GeneratedCodeValidationResult compileResult = new GeneratedCodeValidationResult(
                ValidationStatus.PASSED,
                "compile passed",
                "",
                List.of(new GeneratedFileValidation("LoginPage.java", ValidationStatus.PASSED, "ok"))
        );
        GeneratedCodeCompileAgent compileAgent = new GeneratedCodeCompileAgent(paths -> compileResult);
        compileAgent.applyOutput(compileResult, state);

        GeneratedCodeReviewReport reviewReport = new GeneratedCodeReviewReport("review passed", 1, 0, List.of());
        GeneratedCodeReviewAgent reviewAgent = new GeneratedCodeReviewAgent(sources -> reviewReport);
        reviewAgent.applyOutput(reviewReport, state);

        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("target\\ai-run\\validation\\generated-code-compile-result.json")
                        || path.endsWith("target/ai-run/validation/generated-code-compile-result.json")));
        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("target\\ai-run\\validation\\generated-code-review-result.json")
                        || path.endsWith("target/ai-run/validation/generated-code-review-result.json")));
    }

    @Test
    public void filePersistenceAgentPublishesPersistedSourcesArtifact() {
        WorkflowState state = new WorkflowState("objective", new RequirementInput(SourceType.FILE, "requirements.md"));
        GeneratedSourceFile file = new GeneratedSourceFile(
                "ua.demo.agentlab.ui.generated.pages",
                "LoginPage",
                "src/test/java/ua/demo/agentlab/ui/generated/pages/LoginPage.java",
                "class LoginPage {}"
        );
        LocalFilePersistenceAgent agent = new LocalFilePersistenceAgent(sourceFile -> {
        });

        agent.applyOutput(List.of(file.relativePath()), state);

        Assert.assertTrue(state.getAiArtifactFiles().stream()
                .anyMatch(path -> path.endsWith("target\\ai-run\\validation\\persisted-generated-sources.json")
                        || path.endsWith("target/ai-run/validation/persisted-generated-sources.json")));
        Assert.assertEquals(state.getWrittenFiles(), List.of(file.relativePath()));
    }

    @Test
    public void pomWriterPublishesRequirementToGeneratedSourceTraceability() throws Exception {
        WorkflowState state = new WorkflowState("objective", new RequirementInput(SourceType.FILE, "requirements.md"));
        state.setCanonicalTestCaseBundle(new CanonicalTestCaseBundle(
                "requirements.md",
                "LoginPage",
                List.of("LoginPage"),
                List.of(new CanonicalTestCase(
                        "REQ-LOGIN",
                        "Login route is visible",
                        List.of("REQ-LOGIN"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("LoginPage"),
                        null,
                        "flow-login",
                        "AUTHENTICATION",
                        "LoginPage",
                        "LoginPage",
                        "/login",
                        "/login",
                        "Application is available",
                        UiAssertionProfile.BASIC,
                        List.of("Open login"),
                        List.of("Login route is visible"),
                        List.of(),
                        "requirements.md [L1]"
                ))
        ));
        PomContractSpec contract = loginContract();
        state.setPomContractSpecs(List.of(contract));
        List<GeneratedSourceFile> generated = new DeterministicPomJavaWriter("ua.demo.agentlab.ui.generated.pages")
                .write(List.of(contract));

        new PomContractPageObjectWriterAgent(new DeterministicPomJavaWriter("ua.demo.agentlab.ui.generated.pages"))
                .applyOutput(generated, state);

        String traceabilityPath = state.getAiArtifactFiles().stream()
                .filter(path -> path.endsWith("pom-source-traceability.json"))
                .findFirst()
                .orElseThrow();
        String json = Files.readString(Path.of(traceabilityPath));
        Assert.assertTrue(json.contains("\"requirementIds\""));
        Assert.assertTrue(json.contains("REQ-LOGIN"));
        Assert.assertTrue(json.contains("LoginPage.java"));
    }

    private PomContractSpec loginContract() {
        return new PomContractSpec(
                "pom-contract-v1",
                new PomPageSpec("LoginPage", "/login", "AUTHENTICATION", "openLogin"),
                List.of(new PomLocatorSpec(
                        "usernameInput",
                        "Username",
                        "name",
                        "username",
                        "input",
                        0.90d,
                        "CONFIRMED_LOCATOR",
                        true,
                        true,
                        List.of("test")
                )),
                List.of(),
                List.of(new PomActionSpec(
                        "enterUsername",
                        "ACTION",
                        List.of(new ua.demo.agentlab.ai.ui.model.AiMethodParameterSpec("String", "username")),
                        List.of(new PomStepSpec(PomStepAction.CLEAR_AND_TYPE, "usernameInput", "username", "", ""))
                )),
                List.of(new PomAssertionSpec(
                        "isUsernameInputVisible",
                        "boolean",
                        List.of(new PomCheckSpec(PomCheckType.VISIBLE, "usernameInput", "", "", "", "")),
                        "AND"
                )),
                List.of(),
                List.of()
        );
    }
}
