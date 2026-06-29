package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ContextReranker {

    public RerankResult rerank(QueryIntent intent, List<RetrievedChunk> candidates, int limit) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(candidates, "candidates cannot be null");
        int effectiveLimit = limit <= 0 ? 6 : limit;

        Map<String, RetrievedChunk> bestByPath = new LinkedHashMap<>();
        candidates.stream()
                .sorted(Comparator.comparingDouble((RetrievedChunk candidate) -> score(intent, candidate)).reversed())
                .forEach(candidate -> bestByPath.merge(candidate.relativePath(), candidate, (left, right) ->
                        score(intent, left) >= score(intent, right) ? left : right));

        List<RetrievedChunk> reranked = new ArrayList<>(bestByPath.values());
        reranked.sort(Comparator.comparingDouble((RetrievedChunk candidate) -> score(intent, candidate)).reversed());
        List<RetrievedChunk> limited = reranked.size() > effectiveLimit
                ? List.copyOf(reranked.subList(0, effectiveLimit))
                : List.copyOf(reranked);
        List<ContextRerankExplanation> explanations = limited.stream()
                .map(candidate -> new ContextRerankExplanation(
                        candidate.relativePath(),
                        score(intent, candidate),
                        explain(intent, candidate)
                ))
                .toList();
        return new RerankResult(limited, explanations);
    }

    private double score(QueryIntent intent, RetrievedChunk candidate) {
        double score = candidate.score();
        ArtifactType artifactType = candidate.metadata().artifactType();
        if (intent.requestedArtifactTypes().contains(artifactType)) {
            score += 1.1d;
        }
        if (artifactType == ArtifactType.PAGE_OBJECT || artifactType == ArtifactType.TEST_CLASS) {
            score += 0.3d;
        }
        if (artifactType == ArtifactType.BASE_CLASS || artifactType == ArtifactType.POLICY) {
            score += 0.2d;
        }
        String searchable = (candidate.relativePath() + " "
                + candidate.metadata().artifactName() + " "
                + candidate.metadata().packageName() + " "
                + String.join(" ", candidate.metadata().tags()) + " "
                + candidate.text()).toLowerCase(Locale.ROOT);
        for (String domainTerm : intent.domainTerms()) {
            if (searchable.contains(domainTerm.toLowerCase(Locale.ROOT))) {
                score += 0.55d;
            }
        }
        for (String qualifier : intent.qualifiers()) {
            if (searchable.contains(qualifier.toLowerCase(Locale.ROOT))) {
                score += 0.25d;
            }
        }
        if (candidate.chunkIndex() < 0) {
            score -= 0.08d;
        }
        return score;
    }

    private List<String> explain(QueryIntent intent, RetrievedChunk candidate) {
        List<String> reasons = new ArrayList<>();
        ArtifactType artifactType = candidate.metadata().artifactType();
        if (intent.requestedArtifactTypes().contains(artifactType)) {
            reasons.add("artifact-type-match:" + artifactType.name());
        }
        String searchable = (candidate.relativePath() + " "
                + candidate.metadata().artifactName() + " "
                + candidate.metadata().packageName() + " "
                + String.join(" ", candidate.metadata().tags()) + " "
                + candidate.text()).toLowerCase(Locale.ROOT);
        for (String domainTerm : intent.domainTerms()) {
            if (searchable.contains(domainTerm.toLowerCase(Locale.ROOT))) {
                reasons.add("domain-term:" + domainTerm);
            }
        }
        for (String qualifier : intent.qualifiers()) {
            if (searchable.contains(qualifier.toLowerCase(Locale.ROOT))) {
                reasons.add("qualifier:" + qualifier);
            }
        }
        if (candidate.chunkIndex() < 0) {
            reasons.add("synthetic-graph-artifact");
        }
        if (reasons.isEmpty()) {
            reasons.add("semantic-score");
        }
        return reasons;
    }
}
