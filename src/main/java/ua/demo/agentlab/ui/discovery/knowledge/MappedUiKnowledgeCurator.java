package ua.demo.agentlab.ui.discovery.knowledge;

import ua.demo.agentlab.ui.discovery.knowledge.model.ExcludedEvidence;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeCurated;
import ua.demo.agentlab.ui.discovery.knowledge.model.MappedUiKnowledgeRaw;
import ua.demo.agentlab.ui.discovery.mapping.MappedKnowledgeCurationFilter;
import ua.demo.agentlab.ui.discovery.mapping.model.LocatorCandidate;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedElement;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedField;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedUiKnowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MappedUiKnowledgeCurator {

    private final MappedKnowledgeCurationFilter curationFilter;

    public MappedUiKnowledgeCurator() {
        this(new MappedKnowledgeCurationFilter());
    }

    MappedUiKnowledgeCurator(MappedKnowledgeCurationFilter curationFilter) {
        this.curationFilter = curationFilter == null
                ? new MappedKnowledgeCurationFilter()
                : curationFilter;
    }

    public MappedUiKnowledgeCurated curate(MappedUiKnowledgeRaw rawKnowledge) {
        if (rawKnowledge == null) {
            return new MappedUiKnowledgeCurated(
                    MappedUiKnowledge.empty(),
                    List.of(),
                    List.of("curator:empty-raw-knowledge"),
                    0.0d
            );
        }
        MappedUiKnowledge raw = rawKnowledge.knowledge();
        MappedUiKnowledge curated = curationFilter.filterForCuration(raw);
        if (curated == null) {
            curated = MappedUiKnowledge.empty();
        }
        List<ExcludedEvidence> excludedEvidence = excludedLocatorEvidence(raw, curated);
        List<String> trace = new ArrayList<>(rawKnowledge.sourceTrace());
        trace.add("curator:safe-candidate-filter");
        trace.add("curator:excluded-evidence-count=" + excludedEvidence.size());
        return new MappedUiKnowledgeCurated(
                curated,
                excludedEvidence,
                trace,
                confidence(raw, curated)
        );
    }

    private List<ExcludedEvidence> excludedLocatorEvidence(MappedUiKnowledge raw, MappedUiKnowledge curated) {
        List<ExcludedEvidence> excluded = new ArrayList<>();
        for (MappedPage rawPage : raw.pages()) {
            MappedPage curatedPage = curated.pages().stream()
                    .filter(page -> page.pageId().equals(rawPage.pageId()))
                    .findFirst()
                    .orElse(null);
            for (MappedElement rawElement : rawPage.elements()) {
                List<LocatorCandidate> promoted = curatedPage == null
                        ? List.of()
                        : curatedPage.elements().stream()
                        .filter(element -> element.elementId().equals(rawElement.elementId()))
                        .findFirst()
                        .map(MappedElement::locatorCandidates)
                        .orElse(List.of());
                addExcludedLocators(excluded, rawPage.pageId(), "element", rawElement.locatorCandidates(), promoted);
            }
            rawPage.forms().forEach(form -> form.fields().forEach(field -> {
                List<LocatorCandidate> promoted = curatedPage == null
                        ? List.of()
                        : curatedPage.forms().stream()
                        .filter(candidate -> candidate.formId().equals(form.formId()))
                        .findFirst()
                        .flatMap(candidate -> candidate.fields().stream()
                                .filter(curatedField -> curatedField.fieldId().equals(field.fieldId()))
                                .findFirst())
                        .map(MappedField::locatorCandidates)
                        .orElse(List.of());
                addExcludedLocators(excluded, rawPage.pageId(), "form-field", field.locatorCandidates(), promoted);
            }));
        }
        return excluded;
    }

    private void addExcludedLocators(
            List<ExcludedEvidence> excluded,
            String pageId,
            String source,
            List<LocatorCandidate> rawLocators,
            List<LocatorCandidate> promotedLocators
    ) {
        for (LocatorCandidate locator : rawLocators) {
            boolean promoted = promotedLocators.stream()
                    .anyMatch(candidate -> candidate.strategy() == locator.strategy()
                            && candidate.value().equals(locator.value()));
            if (!promoted) {
                excluded.add(new ExcludedEvidence(
                        "MappedUiKnowledgeRaw",
                        "locator:" + source,
                        pageId,
                        exclusionReason(locator),
                        locator.strategy().wireName() + "=" + locator.value()
                ));
            }
        }
    }

    private String exclusionReason(LocatorCandidate locator) {
        if (locator == null) {
            return "null-locator";
        }
        List<String> reasons = new ArrayList<>();
        if (locator.stabilityScore() < 0.75d) {
            reasons.add("low-stability-score=" + String.format(Locale.ROOT, "%.2f", locator.stabilityScore()));
        }
        if (!locator.sameOrigin()) {
            reasons.add("external-origin");
        }
        if (!locator.uniqueOnPage()) {
            reasons.add("not-unique-on-page");
        }
        if (!locator.stableAcrossRuns()) {
            reasons.add("unstable-across-runs");
        }
        if (!locator.risks().isEmpty()) {
            reasons.add("risks=" + locator.risks());
        }
        return reasons.isEmpty() ? "not-promoted" : String.join("; ", reasons);
    }

    private double confidence(MappedUiKnowledge raw, MappedUiKnowledge curated) {
        long rawLocatorCount = locatorCount(raw);
        if (rawLocatorCount == 0) {
            return curated.pages().isEmpty() ? 0.0d : 0.70d;
        }
        return Math.max(0.0d, Math.min(1.0d, (double) locatorCount(curated) / rawLocatorCount));
    }

    private long locatorCount(MappedUiKnowledge knowledge) {
        return knowledge.pages().stream()
                .flatMap(page -> java.util.stream.Stream.concat(
                        page.elements().stream().flatMap(element -> element.locatorCandidates().stream()),
                        page.forms().stream().flatMap(form -> form.fields().stream())
                                .flatMap(field -> field.locatorCandidates().stream())
                ))
                .count();
    }
}
