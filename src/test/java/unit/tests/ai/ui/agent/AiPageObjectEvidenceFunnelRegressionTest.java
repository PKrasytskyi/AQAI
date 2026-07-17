package unit.tests.ai.ui.agent;

import org.testng.Assert;
import org.testng.annotations.Test;
import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.context.PromptUiEvidence;
import ua.demo.agentlab.ai.openai.OpenAiRuntimeConfig;
import ua.demo.agentlab.ai.ui.agent.AiPageObjectSpecAgent;
import ua.demo.agentlab.ai.ui.agent.AiPageObjectSpecInput;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationRequest;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectGenerationResult;
import ua.demo.agentlab.ai.ui.generation.AiPageObjectSpecGenerator;
import ua.demo.agentlab.ui.UiTestPlan;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelMetrics;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceFunnelReport;
import ua.demo.agentlab.ui.discovery.evidence.funnel.UiEvidenceRequirementResult;

import java.util.List;
import java.util.Map;

public class AiPageObjectEvidenceFunnelRegressionTest {

    @Test
    public void zeroBoundPagesProduceNoPromptsAndNoPomLlmCalls() {
        CountingGenerator generator = new CountingGenerator();
        AiPageObjectSpecAgent agent = new AiPageObjectSpecAgent(generator);
        UiEvidenceRequirementResult stoppedRequirement = new UiEvidenceRequirementResult(
                "REQ-001", "RECORD_LIST", "", "", 12, 0, 0, 0,
                false, false, false, List.of(), "DISCOVERY",
                "No current-run page matches target context", "Run targeted discovery for RECORD_LIST."
        );
        UiEvidenceFunnelReport funnel = new UiEvidenceFunnelReport(
                UiEvidenceFunnelReport.SCHEMA_VERSION,
                "run-1",
                true,
                false,
                new UiEvidenceFunnelMetrics(12, 0, 0, 4, 0, 0, 0),
                List.of(stoppedRequirement),
                List.of("requirements=1", "explicitStops=1")
        );
        AiContextPackage context = new AiContextPackage(
                "", null, null, null, null, null, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), PromptUiEvidence.empty("test")
        );
        AiPageObjectSpecInput input = new AiPageObjectSpecInput(
                null,
                new UiTestPlan("test", "", List.of("UnboundPage"), List.of()),
                context,
                funnel,
                List.of(),
                null,
                null,
                Map.of()
        );

        AiPageObjectGenerationResult result = agent.execute(input, null);

        Assert.assertEquals(generator.calls, 0);
        Assert.assertTrue(result.contracts().isEmpty());
        Assert.assertEquals(result.artifacts().get("openai.page.object.scoped.requests"), "0");
        Assert.assertEquals(result.artifacts().get("openai.page.object.llm.attempts"), "0");
    }

    private static final class CountingGenerator extends AiPageObjectSpecGenerator {
        private int calls;

        private CountingGenerator() {
            super(new DisabledOpenAiConfig());
        }

        @Override
        public AiPageObjectGenerationResult generate(AiPageObjectGenerationRequest request) {
            calls++;
            return AiPageObjectGenerationResult.empty();
        }
    }

    private static final class DisabledOpenAiConfig implements OpenAiRuntimeConfig {
        @Override public boolean enabled() { return false; }
        @Override public boolean strict() { return true; }
        @Override public String apiKey() { return ""; }
        @Override public String model() { return "test"; }
        @Override public String baseUrl() { return "https://example.invalid"; }
        @Override public boolean assistiveOnly() { return true; }
        @Override public int maxOutputTokens() { return 100; }
        @Override public boolean pageObjectLlmEnabled() { return true; }
    }
}
