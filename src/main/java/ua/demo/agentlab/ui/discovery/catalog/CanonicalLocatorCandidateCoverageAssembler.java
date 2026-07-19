package ua.demo.agentlab.ui.discovery.catalog;

import ua.demo.agentlab.ui.discovery.interaction.model.InteractionEvidenceStatus;
import ua.demo.agentlab.ui.discovery.interaction.model.LocatorPromotionDecision;
import ua.demo.agentlab.ui.discovery.interaction.pipeline.CanonicalInteractionEvidenceBundle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Observability projection from canonical decisions; it does not select or promote locators. */
public final class CanonicalLocatorCandidateCoverageAssembler {

    public LocatorCandidateCoverageReport assemble(CanonicalInteractionEvidenceBundle canonical) {
        if (canonical == null) {
            return new LocatorCandidateCoverageReport(LocatorCandidateCoverageReport.SCHEMA_VERSION, "",
                    0, 0, 0, 0, false, false, false, List.of(),
                    List.of("canonical interaction evidence is missing"));
        }
        Map<String, List<LocatorPromotionDecision>> groups = new LinkedHashMap<>();
        canonical.promotionDecisions().forEach(item -> groups.computeIfAbsent(
                item.interaction().scopedInteraction().candidate().elementKey().value(),
                ignored -> new ArrayList<>()).add(item));
        List<LocatorElementCoverage> elements = groups.values().stream().map(this::element)
                .sorted(Comparator.comparing(LocatorElementCoverage::pageId)
                        .thenComparing(LocatorElementCoverage::componentId)
                        .thenComparing(LocatorElementCoverage::elementId))
                .toList();
        int candidates = (int) elements.stream().filter(LocatorElementCoverage::hasCandidate).count();
        int live = (int) elements.stream().filter(item -> item.liveVerifiedCount() > 0).count();
        int confirmed = (int) elements.stream().filter(LocatorElementCoverage::hasConfirmedPrimary).count();
        boolean candidatePassed = !elements.isEmpty() && candidates == elements.size();
        boolean confirmedPassed = !elements.isEmpty() && confirmed == elements.size();
        return new LocatorCandidateCoverageReport(LocatorCandidateCoverageReport.SCHEMA_VERSION, canonical.runId(),
                elements.size(), candidates, live, confirmed, candidatePassed, confirmedPassed,
                candidatePassed && confirmedPassed && canonical.invariants().passed(), elements,
                List.of("projection-source=canonical-interaction-evidence"));
    }

    private LocatorElementCoverage element(List<LocatorPromotionDecision> decisions) {
        var first = decisions.get(0).interaction().scopedInteraction().candidate();
        int live = (int) decisions.stream().filter(item -> item.interaction().verification().locatorVerified()).count();
        String primary = decisions.stream().filter(LocatorPromotionDecision::primary)
                .map(item -> item.interaction().scopedInteraction().candidate().locatorEvidenceId().value())
                .findFirst().orElse("");
        String standby = decisions.stream().filter(LocatorPromotionDecision::standby)
                .map(item -> item.interaction().scopedInteraction().candidate().locatorEvidenceId().value())
                .findFirst().orElse("");
        List<RejectedLocatorCandidate> rejected = decisions.stream()
                .filter(item -> item.status() == InteractionEvidenceStatus.REJECTED)
                .map(item -> new RejectedLocatorCandidate(
                        item.interaction().scopedInteraction().candidate().locatorEvidenceId().value(),
                        String.join(",", item.decisionCodes()))).toList();
        List<String> requirements = decisions.stream()
                .flatMap(item -> item.interaction().scopedInteraction().requirementIds().stream())
                .distinct().sorted().toList();
        String pageName = first.provenance().stream().filter(value -> value.startsWith("page-name:"))
                .map(value -> value.substring("page-name:".length())).findFirst().orElse("");
        String gap = primary.isBlank() ? "no confirmed primary locator survived promotion policy" : "";
        return new LocatorElementCoverage(first.pageId(), pageName, first.route(),
                first.elementKey().semanticRole(), first.componentId(), "SEMANTIC_COMPONENT",
                decisions.size(), live, primary, standby, requirements, rejected, gap);
    }
}
