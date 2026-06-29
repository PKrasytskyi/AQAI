package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.context.CanonicalOperationClassifier.CanonicalOperationClassification;
import ua.demo.agentlab.ai.context.CanonicalSubjectClassifier.CanonicalSubjectClassification;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;

public class CanonicalConfidenceScorer {

    public double score(
            MappedAction action,
            MappedElement sourceElement,
            CanonicalInteractionEvidence evidence,
            CanonicalOperationClassification operation,
            CanonicalSubjectClassification subject
    ) {
        double score = Math.max(action.confidenceScore(), operation.confidence());
        if (sourceElement != null) {
            score = Math.max(score, sourceElement.confidenceScore() * 0.9d);
        }
        if (!subject.domainHints().isEmpty()) {
            score += 0.05d;
        }
        if (!evidence.extractedRoutes().isEmpty()) {
            score += 0.04d;
        }
        if (!evidence.locatorValues().isEmpty()) {
            score += 0.03d;
        }
        return Math.max(0.0d, Math.min(1.0d, score));
    }
}
