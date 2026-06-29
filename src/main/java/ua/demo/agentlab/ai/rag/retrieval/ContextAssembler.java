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

public class ContextAssembler {

    public List<RetrievedChunk> assemble(QueryIntent intent, List<RetrievedChunk> retrievedChunks, int maxArtifacts) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(retrievedChunks, "retrievedChunks cannot be null");
        int effectiveLimit = maxArtifacts <= 0 ? 6 : maxArtifacts;

        Map<String, RetrievedChunk> bestChunkPerFile = new LinkedHashMap<>();
        retrievedChunks.stream()
                .sorted(Comparator.comparingDouble((RetrievedChunk chunk) -> score(intent, chunk)).reversed())
                .forEach(chunk -> bestChunkPerFile.putIfAbsent(chunk.relativePath(), chunk));

        List<RetrievedChunk> candidates = new ArrayList<>(bestChunkPerFile.values());
        candidates.sort(Comparator.comparingDouble((RetrievedChunk chunk) -> score(intent, chunk)).reversed());

        List<RetrievedChunk> selected = new ArrayList<>();
        for (ArtifactType artifactType : intent.requestedArtifactTypes()) {
            RetrievedChunk bestForType = candidates.stream()
                    .filter(chunk -> chunk.metadata().artifactType() == artifactType)
                    .filter(chunk -> !selected.contains(chunk))
                    .findFirst()
                    .orElse(null);
            if (bestForType != null) {
                selected.add(bestForType);
                if (selected.size() >= effectiveLimit) {
                    return selected;
                }
            }
        }

        for (RetrievedChunk candidate : candidates) {
            if (selected.contains(candidate)) {
                continue;
            }
            selected.add(candidate);
            if (selected.size() >= effectiveLimit) {
                break;
            }
        }
        return selected;
    }

    private double score(QueryIntent intent, RetrievedChunk chunk) {
        double score = chunk.score();
        if (intent.requestedArtifactTypes().contains(chunk.metadata().artifactType())) {
            score += 1.5d;
        }
        if (chunk.metadata().artifactType() == ArtifactType.BASE_CLASS || chunk.metadata().artifactType() == ArtifactType.POLICY) {
            score += 0.25d;
        }
        String searchableText = (chunk.relativePath() + " "
                + chunk.metadata().artifactName() + " "
                + String.join(" ", chunk.metadata().tags()) + " "
                + chunk.text()).toLowerCase(Locale.ROOT);
        for (String domainTerm : intent.domainTerms()) {
            if (searchableText.contains(domainTerm.toLowerCase(Locale.ROOT))) {
                score += 0.75d;
            }
        }
        for (String qualifier : intent.qualifiers()) {
            if (searchableText.contains(qualifier.toLowerCase(Locale.ROOT))) {
                score += 0.35d;
            }
        }
        return score;
    }
}
