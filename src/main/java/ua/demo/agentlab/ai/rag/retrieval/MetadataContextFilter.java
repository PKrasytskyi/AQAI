package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MetadataContextFilter {

    public MetadataFilterResult filter(QueryIntent intent, List<RetrievedChunk> candidates) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(candidates, "candidates cannot be null");

        Map<String, RetrievedChunk> bestByPath = new LinkedHashMap<>();
        int rejected = 0;
        for (RetrievedChunk candidate : candidates) {
            if (!shouldKeep(intent, candidate)) {
                rejected++;
                continue;
            }
            bestByPath.merge(candidate.relativePath(), candidate, (left, right) ->
                    left.score() >= right.score() ? left : right);
        }
        return new MetadataFilterResult(new ArrayList<>(bestByPath.values()), rejected);
    }

    private boolean shouldKeep(QueryIntent intent, RetrievedChunk candidate) {
        ArtifactType artifactType = candidate.metadata().artifactType();
        if (intent.requestedArtifactTypes().contains(artifactType)) {
            return true;
        }
        if (artifactType == ArtifactType.BASE_CLASS
                || artifactType == ArtifactType.POLICY
                || artifactType == ArtifactType.TEST_DATA
                || artifactType == ArtifactType.CONFIGURATION) {
            return true;
        }
        String searchable = (candidate.relativePath() + " "
                + candidate.metadata().artifactName() + " "
                + String.join(" ", candidate.metadata().tags()) + " "
                + candidate.text()).toLowerCase(Locale.ROOT);
        for (String domainTerm : intent.domainTerms()) {
            if (searchable.contains(domainTerm.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        for (String qualifier : intent.qualifiers()) {
            if (searchable.contains(qualifier.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return candidate.score() >= 0.55d;
    }
}
