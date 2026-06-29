package ua.demo.agentlab.orchestration.pipeline;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.assertions.agent.AssertionContractAgent;
import ua.demo.agentlab.ai.assertions.model.AssertionType;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult;
import ua.demo.agentlab.ai.ui.model.AiPageObjectSpec;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.policy.PolicyLoadingAgent;
import ua.demo.agentlab.policy.PolicyResolver;
import ua.demo.agentlab.policy.provider.DefaultGenerationPolicyProvider;
import ua.demo.agentlab.requirements.agent.RequirementReaderAgent;
import ua.demo.agentlab.requirements.model.RequirementDocument;
import ua.demo.agentlab.requirements.model.RequirementInput;
import ua.demo.agentlab.requirements.model.SourceType;
import ua.demo.agentlab.requirements.normalization.agent.RequirementNormalizationAgent;
import ua.demo.agentlab.requirements.normalization.model.NormalizedRequirementBundle;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.testcase.model.CanonicalTestCaseBundle;
import ua.demo.agentlab.ui.contract.AssertionIntent;
import ua.demo.agentlab.ui.contract.AssertionIntentKind;
import ua.demo.agentlab.ui.discovery.agent.UiPageMappingAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageMappingInput;
import ua.demo.agentlab.ui.discovery.agent.UiPageModelAgent;
import ua.demo.agentlab.ui.discovery.agent.UiPageModelInput;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;
import ua.demo.agentlab.ui.discovery.model.UiDiscoverySnapshot;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelArtifactWriter;
import ua.demo.agentlab.ui.discovery.pagemodel.PageModelBuilder;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.nio.file.Path;
import java.util.List;

public class PipelineAgentMigrationTest {

    @Test
    public void requirementReaderAgentExposesTypedPipelineContract() {
        RequirementInput input = new RequirementInput(SourceType.FILE, "requirements.md");
        RequirementReaderAgent agent = new RequirementReaderAgent(List.of(new ua.demo.agentlab.requirements.source.RequirementSource() {
            @Override
            public boolean supports(RequirementInput candidate) {
                return candidate == input;
            }

            @Override
            public RequirementDocument load(RequirementInput candidate) {
                return new RequirementDocument(candidate.location(), "REQ-001");
            }
        }));

        RequirementDocument output = agent.execute(
                input,
                new WorkflowRunEnvelope("objective", input, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.REQUIREMENT_INPUT);
        Assert.assertEquals(agent.output(), WorkflowArtifact.REQUIREMENT_DOCUMENT);
        Assert.assertEquals(output.source(), "requirements.md");
    }

    @Test
    public void policyLoadingAgentExposesTypedPipelineContract() {
        PolicyLoadingAgent agent = new PolicyLoadingAgent(
                new PolicyResolver(List.of(new DefaultGenerationPolicyProvider()))
        );

        var policy = agent.execute(
                new RequirementInput(SourceType.FILE, "requirements.md"),
                new WorkflowRunEnvelope("objective", null, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.REQUIREMENT_INPUT);
        Assert.assertEquals(agent.output(), WorkflowArtifact.GENERATION_POLICY);
        Assert.assertEquals(policy.policyId(), "default-selenium-testng");
    }

    @Test
    public void requirementNormalizationAgentExposesTypedPipelineContract() {
        RequirementNormalizationAgent agent = new RequirementNormalizationAgent(document ->
                new NormalizedRequirementBundle(document.source(), List.of(), List.of(), List.of()));

        NormalizedRequirementBundle output = agent.execute(
                new RequirementDocument("requirements.md", "REQ-001"),
                new WorkflowRunEnvelope("objective", null, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.REQUIREMENT_DOCUMENT);
        Assert.assertEquals(agent.output(), WorkflowArtifact.NORMALIZED_REQUIREMENT_BUNDLE);
        Assert.assertEquals(output.source(), "requirements.md");
    }

    @Test
    public void assertionContractAgentExposesTypedPipelineContract() {
        AssertionContractAgent agent = new AssertionContractAgent();
        CanonicalTestCase testCase = new CanonicalTestCase(
                "REQ-001",
                "Login page is visible",
                List.of("REQ-001"),
                List.of(),
                List.of(),
                List.of(new AssertionIntent(AssertionIntentKind.PAGE_VISIBLE, "Login form is visible")),
                List.of("LoginPage"),
                null,
                "flow-1",
                "OPEN_PAGE",
                "HomePage",
                "LoginPage",
                "/",
                "/login",
                "Application is available",
                null,
                List.of("Open login page"),
                List.of("Login form is visible"),
                List.of(),
                "requirements.md [L1]"
        );

        var contracts = agent.execute(
                new CanonicalTestCaseBundle("requirements.md", "LoginPage", List.of("LoginPage"), List.of(testCase)),
                new WorkflowRunEnvelope("objective", null, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.CANONICAL_TEST_CASE_BUNDLE);
        Assert.assertEquals(agent.output(), WorkflowArtifact.ASSERTION_CONTRACTS);
        Assert.assertEquals(contracts.size(), 1);
        Assert.assertEquals(contracts.get(0).type(), AssertionType.ELEMENT_VISIBLE);
        Assert.assertEquals(contracts.get(0).expectedValue(), "Login form is visible");
    }

    @Test
    public void uiPageModelAgentExposesTypedPipelineContract() {
        UiPageModelAgent agent = new UiPageModelAgent(
                new PageModelBuilder(),
                new PageModelArtifactWriter(Path.of("target", "test-page-model-artifacts"))
        );
        UiDiscoverySnapshot snapshot = new UiDiscoverySnapshot("profile", "Project", "test", List.of(), List.of());

        PageModelBundle output = agent.execute(
                new UiPageModelInput(snapshot, new SeleniumDiscoveryResult("", List.of(), List.of())),
                new WorkflowRunEnvelope("objective", null, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.UI_DISCOVERY_SNAPSHOT);
        Assert.assertEquals(agent.output(), WorkflowArtifact.PAGE_MODEL_BUNDLE);
        Assert.assertTrue(output.pages().isEmpty());
    }

    @Test
    public void uiPageMappingAgentExposesTypedPipelineContract() {
        MappedUiKnowledge expected = new MappedUiKnowledge(List.of(), List.of(), List.of(), List.of(), List.of());
        UiPageMappingAgent agent = new UiPageMappingAgent((snapshot, selenium, pageModelBundle) -> expected);
        UiDiscoverySnapshot snapshot = new UiDiscoverySnapshot("profile", "Project", "test", List.of(), List.of());
        PageModelBundle pageModelBundle = new PageModelBundle(List.of());

        MappedUiKnowledge output = agent.execute(
                new UiPageMappingInput(null, null, snapshot, new SeleniumDiscoveryResult("", List.of(), List.of()), pageModelBundle),
                new WorkflowRunEnvelope("objective", null, null, null, null, false, "")
        );

        Assert.assertEquals(agent.input(), WorkflowArtifact.PAGE_MODEL_BUNDLE);
        Assert.assertEquals(agent.output(), WorkflowArtifact.MAPPED_UI_KNOWLEDGE);
        Assert.assertSame(output, expected);
    }

    @Test
    public void artifactStoreAcceptsTypedPageObjectGenerationResultForLegacySpecArtifact() {
        WorkflowState state = new WorkflowState(
                "objective",
                new RequirementInput(SourceType.FILE, "requirements.md")
        );
        AiPageObjectSpec spec = new AiPageObjectSpec("LoginPage", "/auth/login", "openLogin", List.of(), List.of());
        AiPageObjectGenerationResult result = new AiPageObjectGenerationResult(
                List.of(spec),
                List.of(),
                java.util.Map.of(),
                List.of()
        );

        PipelineArtifactStore.from(state).put(WorkflowArtifact.AI_PAGE_OBJECT_SPECS, result);

        Assert.assertEquals(state.getAiPageObjectSpecs(), List.of(spec));
    }
}
