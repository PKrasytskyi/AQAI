package ua.demo.agentlab.ai.rag.retrieval;

import ua.demo.agentlab.ai.rag.model.ArtifactType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RagQueryBuilder {

    public RagQuery build(QueryIntent intent) {
        if (intent == null) {
            throw new IllegalArgumentException("intent cannot be null");
        }
        StringBuilder semanticQuery = new StringBuilder(intent.originalRequest());
        semanticQuery.append(System.lineSeparator())
                .append("Task type: ")
                .append(intent.taskClassification().taskType().name());

        if (!intent.requestedArtifactTypes().isEmpty()) {
            semanticQuery.append(System.lineSeparator()).append("Relevant artifact types: ");
            for (int index = 0; index < intent.requestedArtifactTypes().size(); index++) {
                if (index > 0) {
                    semanticQuery.append(", ");
                }
                semanticQuery.append(intent.requestedArtifactTypes().get(index).name());
            }
        }
        if (!intent.domainTerms().isEmpty()) {
            semanticQuery.append(System.lineSeparator())
                    .append("Domain terms: ")
                    .append(String.join(", ", intent.domainTerms()));
        }
        if (!intent.qualifiers().isEmpty()) {
            semanticQuery.append(System.lineSeparator())
                    .append("Qualifiers: ")
                    .append(String.join(", ", intent.qualifiers()));
        }

        List<String> graphTerms = new ArrayList<>(intent.domainTerms());
        graphTerms.addAll(intent.qualifiers());
        intent.requestedArtifactTypes().stream()
                .map(ArtifactType::name)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(graphTerms::add);
        graphTerms.add(intent.taskClassification().taskType().name().toLowerCase(Locale.ROOT));

        List<String> metadataHints = new ArrayList<>();
        metadataHints.add("task:" + intent.taskClassification().taskType().name().toLowerCase(Locale.ROOT));
        for (ArtifactType artifactType : intent.requestedArtifactTypes()) {
            metadataHints.add("artifactType:" + artifactType.name());
        }
        for (String signal : intent.taskClassification().signals()) {
            metadataHints.add("signal:" + signal);
        }
        return new RagQuery(
                semanticQuery.toString(),
                graphTerms.stream().distinct().toList(),
                metadataHints.stream().distinct().toList()
        );
    }
}
