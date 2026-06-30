package ua.demo.agentlab.api.agent;

import ua.demo.agentlab.api.discovery.ApiEndpointBundleMerger;
import ua.demo.agentlab.api.discovery.NetworkEndpointAdapter;
import ua.demo.agentlab.api.discovery.PropertiesApiEndpointSeedLoader;
import ua.demo.agentlab.api.generator.ApiTemplateSpecGenerator;
import ua.demo.agentlab.api.generator.RuleBasedCanonicalApiTestCaseGenerator;
import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.api.model.CanonicalApiTestCaseBundle;
import ua.demo.agentlab.api.quality.ApiQualityGate;
import ua.demo.agentlab.api.quality.ApiQualityReport;
import ua.demo.agentlab.api.spec.ApiGenerationSpec;
import ua.demo.agentlab.api.writer.ApiRestAssuredTestNgWriter;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApiGenerationAgent implements WorkflowAgent,
        PipelineAgent<ApiGenerationInput, ApiGenerationResult> {

    private final PropertiesApiEndpointSeedLoader seedLoader;
    private final NetworkEndpointAdapter networkEndpointAdapter;
    private final ApiEndpointBundleMerger endpointBundleMerger;
    private final RuleBasedCanonicalApiTestCaseGenerator testCaseGenerator;
    private final ApiTemplateSpecGenerator specGenerator;
    private final ApiQualityGate qualityGate;
    private final ApiRestAssuredTestNgWriter writer;
    private final StageOutputPublisher outputPublisher = new StageOutputPublisher();

    public ApiGenerationAgent() {
        this(
                new PropertiesApiEndpointSeedLoader(),
                new NetworkEndpointAdapter(),
                new ApiEndpointBundleMerger(),
                new RuleBasedCanonicalApiTestCaseGenerator(),
                new ApiTemplateSpecGenerator(),
                new ApiQualityGate(),
                new ApiRestAssuredTestNgWriter()
        );
    }

    ApiGenerationAgent(
            PropertiesApiEndpointSeedLoader seedLoader,
            NetworkEndpointAdapter networkEndpointAdapter,
            ApiEndpointBundleMerger endpointBundleMerger,
            RuleBasedCanonicalApiTestCaseGenerator testCaseGenerator,
            ApiTemplateSpecGenerator specGenerator,
            ApiQualityGate qualityGate,
            ApiRestAssuredTestNgWriter writer
    ) {
        this.seedLoader = seedLoader == null ? new PropertiesApiEndpointSeedLoader() : seedLoader;
        this.networkEndpointAdapter = networkEndpointAdapter == null ? new NetworkEndpointAdapter() : networkEndpointAdapter;
        this.endpointBundleMerger = endpointBundleMerger == null ? new ApiEndpointBundleMerger() : endpointBundleMerger;
        this.testCaseGenerator = testCaseGenerator == null ? new RuleBasedCanonicalApiTestCaseGenerator() : testCaseGenerator;
        this.specGenerator = specGenerator == null ? new ApiTemplateSpecGenerator() : specGenerator;
        this.qualityGate = qualityGate == null ? new ApiQualityGate() : qualityGate;
        this.writer = writer == null ? new ApiRestAssuredTestNgWriter() : writer;
    }

    @Override
    public String name() {
        return "api-generation-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.REQUIREMENT_INPUT);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.API_GENERATION_RESULT);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.REQUIREMENT_INPUT;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.API_GENERATION_RESULT;
    }

    @Override
    public boolean supports(ApiGenerationInput input, WorkflowRunEnvelope run) {
        return input != null
                && ((input.seededEndpoints() != null && !input.seededEndpoints().endpoints().isEmpty())
                || input.seleniumDiscoveryResult() != null);
    }

    @Override
    public ApiGenerationInput inputFrom(PipelineArtifactStore store, WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        return new ApiGenerationInput(
                seedLoader.load(),
                store == null
                        ? state.getSeleniumDiscoveryResult()
                        : (ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult)
                        store.get(WorkflowArtifact.SELENIUM_DISCOVERY_RESULT).orElse(state.getSeleniumDiscoveryResult())
        );
    }

    @Override
    public ApiGenerationResult execute(ApiGenerationInput input, WorkflowRunEnvelope run) {
        ApiEndpointBundle networkEndpoints = networkEndpointAdapter.adapt(input.seleniumDiscoveryResult());
        ApiEndpointBundle endpoints = endpointBundleMerger.merge(
                "api-discovery",
                List.of(input.seededEndpoints(), networkEndpoints)
        );
        CanonicalApiTestCaseBundle testCases = testCaseGenerator.generate(endpoints);
        ApiGenerationSpec generationSpec = specGenerator.generate(endpoints, testCases);
        ApiQualityReport specQualityReport = qualityGate.validate(endpoints, testCases, generationSpec);
        List<GeneratedSourceFile> sourceFiles = specQualityReport.hasBlockingIssues()
                ? List.of()
                : writer.write(generationSpec);
        ApiQualityReport qualityReport = qualityGate.validate(endpoints, testCases, generationSpec, sourceFiles);
        Map<String, String> artifacts = new LinkedHashMap<>();
        artifacts.put("api.endpoint.count", String.valueOf(endpoints.endpoints().size()));
        artifacts.put("api.canonical.test.case.count", String.valueOf(testCases.testCases().size()));
        artifacts.put("api.client.spec.count", String.valueOf(generationSpec.clientSpecs().size()));
        artifacts.put("api.dto.spec.count", String.valueOf(generationSpec.dtoSpecs().size()));
        artifacts.put("api.test.spec.count", String.valueOf(generationSpec.testSpecs().size()));
        artifacts.put("api.crud.spec.count", String.valueOf(generationSpec.crudScenarioSpecs().size()));
        artifacts.put("api.source.preview.count", String.valueOf(sourceFiles.size()));
        artifacts.put("api.quality.blocking.count", String.valueOf(qualityReport.blockingIssueCount()));
        artifacts.put("api.generation.completed", "true");
        return new ApiGenerationResult(endpoints, testCases, generationSpec, qualityReport, sourceFiles, artifacts);
    }

    @Override
    public void applyOutput(ApiGenerationResult output, WorkflowState state) {
        outputPublisher.publishApiGenerationResult(output, state);
    }
}
