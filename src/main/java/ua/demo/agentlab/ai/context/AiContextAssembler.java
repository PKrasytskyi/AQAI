package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.flow.FlowScopedKnowledgePackage;
import ua.demo.agentlab.orchestration.WorkflowState;

import java.util.ArrayList;
import java.util.List;

public class AiContextAssembler {

    private final CanonicalInteractionLayer canonicalInteractionLayer;
    private final UiKnowledgeRetrievalService retrievalService;
    private final PromptUiEvidenceBuilder promptUiEvidenceBuilder = new PromptUiEvidenceBuilder();

    public AiContextAssembler() {
        this(null, null);
    }

    public AiContextAssembler(
            CanonicalInteractionLayer canonicalInteractionLayer,
            UiKnowledgeRetrievalService retrievalService
    ) {
        this.canonicalInteractionLayer = canonicalInteractionLayer;
        this.retrievalService = retrievalService;
    }

    public AiContextPackage assemble(WorkflowState state) {
        if (state == null) {
            throw new IllegalArgumentException("state cannot be null");
        }
        AiContextAssemblyInput input = AiContextAssemblyInput.from(state);

        FlowScopedKnowledgePackage flowScopedKnowledgePackage = input.flowScopedKnowledgePackage();
        CanonicalUiInteractionModel canonicalInteractionModel;
        UiKnowledgeRetrievalContext retrievalContext;
        if (flowScopedKnowledgePackage != null) {
            canonicalInteractionModel = flowScopedKnowledgePackage.canonicalInteractionModel();
            retrievalContext = flowScopedKnowledgePackage.retrievalContext();
        } else {
            canonicalInteractionModel = canonicalInteractionLayer == null
                    ? CanonicalUiInteractionModel.empty()
                    : canonicalInteractionLayer.build(input.mappedUiKnowledge());
            retrievalContext = retrievalService == null
                    ? UiKnowledgeRetrievalContext.empty("DB-backed retrieval is not configured")
                    : retrievalService.retrieve(UiKnowledgeRetrievalRequest.from(input, canonicalInteractionModel));
        }

        return packageFrom(
                input,
                canonicalInteractionModel,
                retrievalContext
        );
    }

    public AiContextPackage assemble(AiContextAssemblyInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input cannot be null");
        }

        FlowScopedKnowledgePackage flowScopedKnowledgePackage = input.flowScopedKnowledgePackage();
        CanonicalUiInteractionModel canonicalInteractionModel;
        UiKnowledgeRetrievalContext retrievalContext;
        if (flowScopedKnowledgePackage != null) {
            canonicalInteractionModel = flowScopedKnowledgePackage.canonicalInteractionModel();
            retrievalContext = flowScopedKnowledgePackage.retrievalContext();
        } else {
            canonicalInteractionModel = canonicalInteractionLayer == null
                    ? CanonicalUiInteractionModel.empty()
                    : canonicalInteractionLayer.build(input.mappedUiKnowledge());
            retrievalContext = retrievalService == null
                    ? UiKnowledgeRetrievalContext.empty("DB-backed retrieval is not configured")
                    : retrievalService.retrieve(UiKnowledgeRetrievalRequest.from(input, canonicalInteractionModel));
        }

        return packageFrom(input, canonicalInteractionModel, retrievalContext);
    }

    private AiContextPackage packageFrom(
            AiContextAssemblyInput input,
            CanonicalUiInteractionModel canonicalInteractionModel,
            UiKnowledgeRetrievalContext retrievalContext
    ) {
        FlowScopedKnowledgePackage flowScopedKnowledgePackage = input.flowScopedKnowledgePackage();
        AiContextPackage contextPackage = new AiContextPackage(
                input.objective(),
                input.normalizedRequirementBundle(),
                input.generationPolicy(),
                input.projectProfile(),
                input.testPlan(),
                input.canonicalTestCaseBundle(),
                input.uiTestPlan(),
                input.canonicalPageFlowModel(),
                flowScopedKnowledgePackage == null
                        ? input.mappedUiKnowledge()
                        : flowScopedKnowledgePackage.mappedUiKnowledge(),
                input.mappedUiKnowledgeCurated(),
                input.pageModelBundle(),
                canonicalInteractionModel,
                retrievalContext,
                input.assertionContracts(),
                input.pageModelEnrichments(),
                buildTemplateCapabilities(),
                PromptUiEvidence.empty("prompt-evidence:assembly-bootstrap")
        );
        return withPromptEvidence(contextPackage, promptUiEvidenceBuilder.build(contextPackage));
    }

    private AiContextPackage withPromptEvidence(AiContextPackage contextPackage, PromptUiEvidence promptUiEvidence) {
        return new AiContextPackage(
                contextPackage.objective(),
                contextPackage.normalizedRequirementBundle(),
                contextPackage.generationPolicy(),
                contextPackage.projectProfile(),
                contextPackage.testPlan(),
                contextPackage.canonicalTestCaseBundle(),
                contextPackage.uiTestPlan(),
                contextPackage.canonicalPageFlowModel(),
                contextPackage.mappedUiKnowledge(),
                contextPackage.mappedUiKnowledgeCurated(),
                contextPackage.pageModelBundle(),
                contextPackage.canonicalInteractionModel(),
                contextPackage.retrievalContext(),
                contextPackage.assertionContracts(),
                contextPackage.pageModelEnrichments(),
                contextPackage.templateCapabilities(),
                promptUiEvidence
        );
    }

    private List<String> buildTemplateCapabilities() {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("BasePage supports open(relativePath), openAbsolute(url), getCurrentUrl(), getTitle(), getPageSource().");
        capabilities.add("BasePage exposes helpers: elements, waits, frames, dropdowns, alerts, windows, navigation, scripts, interactions.");
        capabilities.add("Element actions support visibility checks, click, typing, lists of elements, and text extraction.");
        capabilities.add("Generated UI tests extend BaseTest and should use UiAssertions, ScenarioData, and project runtimeConfig.");
        capabilities.add("Page Objects should follow OOP/POM and prefer reusable interaction methods over inline Selenium steps.");
        return List.copyOf(capabilities);
    }
}
