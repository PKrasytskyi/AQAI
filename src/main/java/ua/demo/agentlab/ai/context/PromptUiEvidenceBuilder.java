package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.component.ComponentBoundaryDetector;
import ua.demo.agentlab.ui.discovery.evidence.EvidenceRankingService;
import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceClassifier;
import ua.demo.agentlab.ui.discovery.semantic.SemanticActionModelBuilder;

import java.util.List;

public class PromptUiEvidenceBuilder {

    private final PageOwnershipSlicer pageOwnershipSlicer;
    private final SemanticActionEvidenceSelector semanticActionEvidenceSelector;
    private final PromptAssertionEvidenceSelector assertionEvidenceSelector;
    private final LocatorEvidenceSelector locatorEvidenceSelector;
    private final PromptEvidencePrioritizer promptEvidencePrioritizer;
    private final PromptUiEvidenceAssembler promptUiEvidenceAssembler;
    private final PromptEvidenceQualityGate promptEvidenceQualityGate;

    public PromptUiEvidenceBuilder() {
        this(
                new PromptLocatorSelector(),
                new SemanticActionModelBuilder(),
                new ComponentBoundaryDetector(),
                new EvidenceRankingService(),
                new LocatorEvidenceClassifier()
        );
    }

    PromptUiEvidenceBuilder(PromptLocatorSelector locatorSelector) {
        this(
                locatorSelector,
                new SemanticActionModelBuilder(),
                new ComponentBoundaryDetector(),
                new EvidenceRankingService(),
                new LocatorEvidenceClassifier()
        );
    }

    PromptUiEvidenceBuilder(
            PromptLocatorSelector locatorSelector,
            SemanticActionModelBuilder semanticActionModelBuilder
    ) {
        this(
                locatorSelector,
                semanticActionModelBuilder,
                new ComponentBoundaryDetector(),
                new EvidenceRankingService(),
                new LocatorEvidenceClassifier()
        );
    }

    PromptUiEvidenceBuilder(
            PromptLocatorSelector locatorSelector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            ComponentBoundaryDetector componentBoundaryDetector
    ) {
        this(
                locatorSelector,
                semanticActionModelBuilder,
                componentBoundaryDetector,
                new EvidenceRankingService(),
                new LocatorEvidenceClassifier()
        );
    }

    PromptUiEvidenceBuilder(
            PromptLocatorSelector locatorSelector,
            SemanticActionModelBuilder semanticActionModelBuilder,
            ComponentBoundaryDetector componentBoundaryDetector,
            EvidenceRankingService evidenceRankingService,
            LocatorEvidenceClassifier locatorEvidenceClassifier
    ) {
        this.pageOwnershipSlicer = new PageOwnershipSlicer();
        this.semanticActionEvidenceSelector = new SemanticActionEvidenceSelector(
                semanticActionModelBuilder,
                pageOwnershipSlicer
        );
        this.assertionEvidenceSelector = new PromptAssertionEvidenceSelector(pageOwnershipSlicer);
        this.locatorEvidenceSelector = new LocatorEvidenceSelector(
                locatorSelector,
                semanticActionModelBuilder,
                componentBoundaryDetector,
                evidenceRankingService,
                locatorEvidenceClassifier,
                pageOwnershipSlicer
        );
        this.promptEvidencePrioritizer = new PromptEvidencePrioritizer(pageOwnershipSlicer);
        this.promptUiEvidenceAssembler = new PromptUiEvidenceAssembler();
        this.promptEvidenceQualityGate = new PromptEvidenceQualityGate();
    }

    public PromptUiEvidence build(AiContextPackage context) {
        if (context == null || context.mappedUiKnowledge() == null || context.mappedUiKnowledge().pages().isEmpty()) {
            return PromptUiEvidence.empty("prompt-evidence:no-mapped-page-scope");
        }
        PromptPageScope scope = pageOwnershipSlicer.slice(context);
        if (scope == null) {
            return PromptUiEvidence.empty("prompt-evidence:no-page-scope");
        }
        List<PromptActionEvidence> actions = semanticActionEvidenceSelector.select(context, scope);
        List<PromptAssertionEvidence> assertions = assertionEvidenceSelector.select(context, scope);
        List<PromptLocatorEvidence> locatorCandidates = locatorEvidenceSelector.select(context, scope);
        List<PromptLocatorEvidence> prioritizedLocators = promptEvidencePrioritizer
                .prioritize(locatorCandidates, context, scope)
                .stream()
                .limit(16)
                .toList();
        PromptUiEvidence evidence = promptUiEvidenceAssembler.assemble(
                context,
                scope,
                actions,
                assertions,
                prioritizedLocators
        );
        promptEvidenceQualityGate.validate(evidence);
        return evidence;
    }
}
