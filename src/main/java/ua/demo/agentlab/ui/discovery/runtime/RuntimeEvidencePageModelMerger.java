package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.pagemodel.model.PageApiRelationModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModel;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageModelBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.runtime.model.SemanticNetworkEvidence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RuntimeEvidencePageModelMerger {

    public PageModelBundle merge(PageModelBundle pageModelBundle, RuntimeEvidenceBundle runtimeEvidenceBundle) {
        if (pageModelBundle == null || runtimeEvidenceBundle == null
                || runtimeEvidenceBundle.semanticNetworkEvidence().isEmpty()) {
            return pageModelBundle;
        }
        List<PageModel> pages = pageModelBundle.pages().stream()
                .map(page -> withRuntimeRelations(page, runtimeEvidenceBundle.semanticNetworkEvidence()))
                .toList();
        return new PageModelBundle(pages);
    }

    private PageModel withRuntimeRelations(PageModel page, List<SemanticNetworkEvidence> evidence) {
        List<PageApiRelationModel> runtimeRelations = evidence.stream()
                .filter(item -> belongsToPage(page, item))
                .sorted(Comparator.comparing(SemanticNetworkEvidence::confidence).reversed())
                .map(this::toRelation)
                .toList();
        if (runtimeRelations.isEmpty()) {
            return page;
        }
        List<PageApiRelationModel> merged = new ArrayList<>(page.apiRelations());
        for (PageApiRelationModel relation : runtimeRelations) {
            boolean exists = merged.stream()
                    .anyMatch(existing -> existing.endpoint().equalsIgnoreCase(relation.endpoint())
                            && existing.relationType().equalsIgnoreCase(relation.relationType()));
            if (!exists) {
                merged.add(relation);
            }
        }
        return new PageModel(
                page.pageId(),
                page.url(),
                page.route(),
                page.title(),
                page.visibleText(),
                page.featureGuess(),
                page.evidence(),
                page.elements(),
                page.forms(),
                merged,
                page.flows()
        );
    }

    private boolean belongsToPage(PageModel page, SemanticNetworkEvidence evidence) {
        if (page.pageId().equalsIgnoreCase(evidence.pageId())) {
            return true;
        }
        String pageUrl = normalize(page.url());
        String evidenceUrl = normalize(evidence.pageUrl());
        return !pageUrl.isBlank() && pageUrl.equals(evidenceUrl);
    }

    private PageApiRelationModel toRelation(SemanticNetworkEvidence evidence) {
        return new PageApiRelationModel(
                evidence.evidenceId(),
                "",
                evidence.endpoint(),
                evidence.operation().toLowerCase(Locale.ROOT),
                evidence.confidence(),
                "runtime evidence: " + evidence.businessIntent() + " via " + evidence.sourceTrace()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
