package ua.demo.agentlab.ai.ui.generation;

import ua.demo.agentlab.ai.context.AiContextPackage;
import ua.demo.agentlab.ai.ui.contract.PomContractEvidenceRehydrator;
import ua.demo.agentlab.ai.ui.contract.PomContractScopeGapReconciler;
import ua.demo.agentlab.ai.ui.contract.PomContractSpec;
import ua.demo.agentlab.ai.ui.contract.PomContractStructuralNormalizer;
import ua.demo.agentlab.ai.ui.parser.PomContractSpecParser;
import ua.demo.agentlab.ai.ui.prompt.scope.PromptReadyPomScope;

/** Parses untrusted model JSON, rehydrates evidence, and reconciles explicit scope gaps. */
public final class PomContractParsingStage {
    private final PomContractSpecParser parser;
    private final PomContractEvidenceRehydrator rehydrator;
    private final PomContractScopeGapReconciler gapReconciler;
    private final PomContractStructuralNormalizer structuralNormalizer;

    public PomContractParsingStage(PomContractSpecParser parser, PomContractEvidenceRehydrator rehydrator,
                                   PomContractScopeGapReconciler gapReconciler) {
        this(parser, rehydrator, gapReconciler, new PomContractStructuralNormalizer());
    }

    PomContractParsingStage(PomContractSpecParser parser, PomContractEvidenceRehydrator rehydrator,
                            PomContractScopeGapReconciler gapReconciler,
                            PomContractStructuralNormalizer structuralNormalizer) {
        this.parser = parser;
        this.rehydrator = rehydrator;
        this.gapReconciler = gapReconciler;
        this.structuralNormalizer = structuralNormalizer;
    }

    public PomContractSpec parse(String response, PromptReadyPomScope scope, AiContextPackage context) {
        return normalize(gapReconciler.reconcile(rehydrator.rehydrate(parser.parse(response), scope, context), scope));
    }

    public PomContractSpec rehydrate(PomContractSpec contract, PromptReadyPomScope scope, AiContextPackage context) {
        return normalize(gapReconciler.reconcile(rehydrator.rehydrate(contract, scope, context), scope));
    }

    private PomContractSpec normalize(PomContractSpec contract) {
        return structuralNormalizer.normalize(contract);
    }
}
