package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ai.context.CanonicalOperationClassifier.CanonicalOperationClassification;
import ua.demo.agentlab.ai.context.CanonicalSubjectClassifier.CanonicalSubjectClassification;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedAction;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleBasedCanonicalInteractionLayer implements CanonicalInteractionLayer {

    private final InteractionEvidenceExtractor evidenceExtractor;
    private final CanonicalAliasDictionary aliasDictionary;
    private final CanonicalOperationClassifier operationClassifier;
    private final CanonicalSubjectClassifier subjectClassifier;
    private final CanonicalConfidenceScorer confidenceScorer;
    private final CanonicalInteractionMerger interactionMerger;

    public RuleBasedCanonicalInteractionLayer() {
        this(new InteractionEvidenceExtractor(),
                new CanonicalAliasDictionary(),
                new CanonicalInteractionMerger(),
                new CanonicalConfidenceScorer());
    }

    RuleBasedCanonicalInteractionLayer(
            InteractionEvidenceExtractor evidenceExtractor,
            CanonicalAliasDictionary aliasDictionary,
            CanonicalInteractionMerger interactionMerger,
            CanonicalConfidenceScorer confidenceScorer
    ) {
        this.evidenceExtractor = evidenceExtractor;
        this.aliasDictionary = aliasDictionary;
        this.operationClassifier = new CanonicalOperationClassifier(aliasDictionary);
        this.subjectClassifier = new CanonicalSubjectClassifier(aliasDictionary);
        this.interactionMerger = interactionMerger;
        this.confidenceScorer = confidenceScorer;
    }

    @Override
    public CanonicalUiInteractionModel build(MappedUiKnowledge knowledge) {
        if (knowledge == null || knowledge.pages().isEmpty()) {
            return CanonicalUiInteractionModel.empty();
        }

        List<CanonicalUiInteraction> interactions = new ArrayList<>();
        for (MappedPage page : knowledge.pages()) {
            Map<String, MappedElement> elementsById = page.elements().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            MappedElement::elementId,
                            element -> element,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            for (MappedAction action : page.actions()) {
                interactions.add(toCanonicalInteraction(page, action, elementsById.get(action.sourceElementId())));
            }
        }
        return new CanonicalUiInteractionModel(interactionMerger.deduplicate(interactions));
    }

    private CanonicalUiInteraction toCanonicalInteraction(
            MappedPage page,
            MappedAction action,
            MappedElement sourceElement
    ) {
        CanonicalInteractionEvidence evidence = evidenceExtractor.extract(page, action, sourceElement);
        CanonicalOperationClassification operation = operationClassifier.classify(evidence);
        CanonicalSubjectClassification subject = subjectClassifier.classify(evidence, operation);

        Set<String> keywords = new LinkedHashSet<>();
        addKeywords(keywords, page.pageName());
        addKeywords(keywords, operation.canonicalName());
        addKeywords(keywords, operation.operationKind().name());
        addKeywords(keywords, subject.subjectType());
        addKeywords(keywords, subject.targetType());
        addKeywords(keywords, action.actionName());
        addKeywords(keywords, action.description());
        keywords.addAll(aliasDictionary.normalizedTokens(evidence));

        return new CanonicalUiInteraction(
                action.actionId(),
                page.pageId(),
                page.pageName(),
                operation.canonicalName(),
                operation.operationKind().name(),
                subject.subjectType(),
                subject.targetType(),
                action.sourceElementId(),
                sourceElement == null ? "" : sourceElement.semanticName(),
                action.targetPageId(),
                subject.targetRoute(),
                List.copyOf(keywords),
                subject.domainHints(),
                operation.rationale(),
                confidenceScorer.score(action, sourceElement, evidence, operation, subject)
        );
    }

    private void addKeywords(Set<String> keywords, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String token : value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/_\\- ]", " ").split("\\s+")) {
            if (!token.isBlank() && (token.length() >= 3 || token.startsWith("/"))) {
                keywords.add(token);
            }
        }
    }
}
