package ua.demo.agentlab.ai.ui.agent;

import ua.demo.agentlab.ai.ui.contract.DeterministicPomJavaWriter;
import ua.demo.agentlab.ai.ui.contract.PomActionSpec;
import ua.demo.agentlab.ai.ui.contract.PomAssertionSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomSourceMapBuilder;
import ua.demo.agentlab.orchestration.pipeline.AiArtifactPublisher;
import ua.demo.agentlab.orchestration.WorkflowAgent;
import ua.demo.agentlab.orchestration.WorkflowArtifact;
import ua.demo.agentlab.orchestration.WorkflowState;
import ua.demo.agentlab.orchestration.pipeline.PipelineAgent;
import ua.demo.agentlab.orchestration.pipeline.PipelineArtifactStore;
import ua.demo.agentlab.orchestration.pipeline.StageOutputPublisher;
import ua.demo.agentlab.orchestration.pipeline.WorkflowRunEnvelope;
import ua.demo.agentlab.testcase.model.CanonicalTestCase;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;
import ua.demo.agentlab.ui.discovery.identity.PageReferenceMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PomContractPageObjectWriterAgent implements WorkflowAgent,
        PipelineAgent<List<PomContractSpec>, List<GeneratedSourceFile>> {

    private final DeterministicPomJavaWriter writer;
    private final StageOutputPublisher publisher = new StageOutputPublisher();
    private final AiArtifactPublisher artifactPublisher = new AiArtifactPublisher();
    private final PomSourceMapBuilder sourceMapBuilder = new PomSourceMapBuilder();

    public PomContractPageObjectWriterAgent(DeterministicPomJavaWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }
        this.writer = writer;
    }

    @Override
    public String name() {
        return "pom-contract-page-object-writer-agent";
    }

    @Override
    public Set<WorkflowArtifact> requires() {
        return Set.of(WorkflowArtifact.POM_CONTRACT_SPECS);
    }

    @Override
    public Set<WorkflowArtifact> produces() {
        return Set.of(WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES, WorkflowArtifact.PAGE_OBJECT_FILES);
    }

    @Override
    public WorkflowArtifact input() {
        return WorkflowArtifact.POM_CONTRACT_SPECS;
    }

    @Override
    public WorkflowArtifact output() {
        return WorkflowArtifact.GENERATED_PAGE_OBJECT_SOURCES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PomContractSpec> inputFrom(PipelineArtifactStore store, WorkflowState state) {
        return (List<PomContractSpec>) store.get(WorkflowArtifact.POM_CONTRACT_SPECS).orElse(List.of());
    }

    @Override
    public boolean supports(List<PomContractSpec> input, WorkflowRunEnvelope run) {
        return input != null && !input.isEmpty();
    }

    @Override
    public List<GeneratedSourceFile> execute(List<PomContractSpec> input, WorkflowRunEnvelope run) {
        return writer.write(input);
    }

    @Override
    public void applyOutput(List<GeneratedSourceFile> output, WorkflowState state) {
        publisher.publishAiPageObjectFiles(output, state);
        artifactPublisher.writeJson(
                state,
                "validation",
                "pom-source-traceability.json",
                buildTraceability(state, output)
        );
        artifactPublisher.writeJson(
                state,
                "validation",
                "pom-source-map.json",
                sourceMapBuilder.build(state == null ? List.of() : state.getPomContractSpecs(), output)
        );
    }

    private GeneratedPomTraceabilityArtifact buildTraceability(
            WorkflowState state,
            List<GeneratedSourceFile> generatedFiles
    ) {
        List<PomContractSpec> contracts = state == null ? List.of() : state.getPomContractSpecs();
        List<GeneratedSourceFile> files = generatedFiles == null ? List.of() : generatedFiles;
        Map<String, GeneratedSourceFile> filesByClass = files.stream()
                .collect(Collectors.toMap(
                        GeneratedSourceFile::className,
                        file -> file,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        List<GeneratedPomTraceabilityEntry> entries = new ArrayList<>();
        for (PomContractSpec contract : contracts == null ? List.<PomContractSpec>of() : contracts) {
            String pageName = contract.page().name();
            GeneratedSourceFile file = filesByClass.get(pageName);
            entries.add(new GeneratedPomTraceabilityEntry(
                    pageName,
                    contract.page().route(),
                    contract.page().capability(),
                    file == null ? pageName : file.className(),
                    file == null ? "" : file.relativePath(),
                    requirementIdsFor(state, contract),
                    contract.actions().stream().map(PomActionSpec::methodName).toList(),
                    contract.assertions().stream().map(PomAssertionSpec::methodName).toList(),
                    contract.coverageGaps()
            ));
        }
        return new GeneratedPomTraceabilityArtifact(entries.size(), entries);
    }

    private List<String> requirementIdsFor(WorkflowState state, PomContractSpec contract) {
        if (state == null || state.getCanonicalTestCaseBundle() == null || contract == null) {
            return List.of();
        }
        return state.getCanonicalTestCaseBundle().testCases().stream()
                .filter(testCase -> ownsPage(contract, testCase))
                .map(CanonicalTestCase::id)
                .distinct()
                .toList();
    }

    private boolean ownsPage(PomContractSpec contract, CanonicalTestCase testCase) {
        String pageName = contract.page().name();
        String route = contract.page().route();
        return PageReferenceMatcher.matchesScenarioPage(testCase.sourcePageName(), testCase.sourceRoute(), pageName)
                || PageReferenceMatcher.matchesScenarioPage(testCase.pageName(), testCase.route(), pageName)
                || (!route.isBlank() && (route.equals(testCase.sourceRoute()) || route.equals(testCase.route())));
    }

    private record GeneratedPomTraceabilityArtifact(
            int pages,
            List<GeneratedPomTraceabilityEntry> entries
    ) {
    }

    private record GeneratedPomTraceabilityEntry(
            String pageName,
            String route,
            String capability,
            String className,
            String relativePath,
            List<String> requirementIds,
            List<String> actionMethods,
            List<String> assertionMethods,
            List<String> coverageGaps
    ) {
    }
}
