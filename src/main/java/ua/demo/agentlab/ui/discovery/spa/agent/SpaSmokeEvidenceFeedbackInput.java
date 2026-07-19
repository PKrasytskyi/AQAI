package ua.demo.agentlab.ui.discovery.spa.agent;

import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionInventory;
import ua.demo.agentlab.ui.discovery.spa.model.SpaLiveTargetedVerificationResult;
import ua.demo.agentlab.ui.writer.GeneratedSourceFile;

import java.util.List;

public record SpaSmokeEvidenceFeedbackInput(List<GeneratedSourceFile> sources, UiInteractionInventory inventory,
                                            SpaLiveTargetedVerificationResult verification,
                                            ua.demo.agentlab.validation.GeneratedCodeValidationResult compileResult,
                                            ua.demo.agentlab.review.GeneratedCodeReviewReport reviewReport,
                                            ua.demo.agentlab.validation.smoke.GeneratedUiSmokeResult generatedSmoke,
                                            ua.demo.agentlab.validation.smoke.LiveUiSmokeResult liveSmoke) {
    public SpaSmokeEvidenceFeedbackInput { sources = sources == null ? List.of() : List.copyOf(sources); }
}
