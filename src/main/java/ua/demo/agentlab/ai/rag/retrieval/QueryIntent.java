package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;

import java.util.List;
import java.util.Set;

public record QueryIntent(
        String originalRequest,
        Set<String> domainTerms,
        Set<String> qualifiers,
        List<ArtifactType> requestedArtifactTypes,
        TaskClassification taskClassification
) {
    public QueryIntent {
        if (originalRequest == null || originalRequest.isBlank()) {
            throw new IllegalArgumentException("originalRequest cannot be blank");
        }
        domainTerms = domainTerms == null ? Set.of() : Set.copyOf(domainTerms);
        qualifiers = qualifiers == null ? Set.of() : Set.copyOf(qualifiers);
        requestedArtifactTypes = requestedArtifactTypes == null ? List.of() : List.copyOf(requestedArtifactTypes);
        if (taskClassification == null) {
            throw new IllegalArgumentException("taskClassification cannot be null");
        }
    }
}
