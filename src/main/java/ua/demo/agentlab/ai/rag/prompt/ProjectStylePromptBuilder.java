package ua.demo.agentlab.ai.rag.prompt;

import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalResult;
import ua.demo.agentlab.templates.ProjectContext;

import java.util.Objects;

public class ProjectStylePromptBuilder implements PromptBuilder {

    @Override
    public String build(String userRequest, ContextRetrievalResult retrievalResult, ProjectContext projectContext) {
        Objects.requireNonNull(userRequest, "userRequest must not be null");
        Objects.requireNonNull(retrievalResult, "retrievalResult must not be null");
        Objects.requireNonNull(projectContext, "projectContext must not be null");
        java.util.List<RetrievedChunk> matches = retrievalResult.contextChunks();

        StringBuilder builder = new StringBuilder();
        builder.append("You are generating automation test code for an existing Java project.")
                .append(System.lineSeparator())
                .append("Follow the project's style and reuse existing framework abstractions.")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("Resolved retrieval intent:")
                .append(System.lineSeparator())
                .append("- Task type: ").append(retrievalResult.intent().taskClassification().taskType())
                .append(System.lineSeparator())
                .append("- Task confidence: ").append(String.format("%.2f", retrievalResult.intent().taskClassification().confidenceScore()))
                .append(System.lineSeparator())
                .append("- Task signals: ").append(String.join(", ", retrievalResult.intent().taskClassification().signals()))
                .append(System.lineSeparator())
                .append("- Retrieval policy: desired=")
                .append(retrievalResult.retrievalPolicy().desiredArtifacts())
                .append(", semanticMultiplier=")
                .append(retrievalResult.retrievalPolicy().semanticRetrievalMultiplier())
                .append(", graphMultiplier=")
                .append(retrievalResult.retrievalPolicy().graphExpansionMultiplier())
                .append(System.lineSeparator())
                .append("- Domain terms: ").append(String.join(", ", retrievalResult.intent().domainTerms()))
                .append(System.lineSeparator())
                .append("- Qualifiers: ").append(String.join(", ", retrievalResult.intent().qualifiers()))
                .append(System.lineSeparator())
                .append("- Requested artifact types: ").append(retrievalResult.intent().requestedArtifactTypes())
                .append(System.lineSeparator())
                .append("- Semantic query: ").append(retrievalResult.ragQuery().semanticQuery())
                .append(System.lineSeparator())
                .append("- Raw retrieved matches: ").append(retrievalResult.rawMatchCount())
                .append(System.lineSeparator())
                .append("- Graph expanded artifacts: ").append(retrievalResult.graphExpandedCount())
                .append(System.lineSeparator())
                .append("- Filtered candidates: ").append(retrievalResult.filteredCandidateCount())
                .append(System.lineSeparator())
                .append("- Graph source: ").append(retrievalResult.graphSource())
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        if (!retrievalResult.rerankExplanations().isEmpty()) {
            builder.append("Top rerank explanations:")
                    .append(System.lineSeparator());
            retrievalResult.rerankExplanations().stream()
                    .limit(5)
                    .forEach(explanation -> builder.append("- ")
                            .append(explanation.relativePath())
                            .append(" | score=")
                            .append(String.format("%.4f", explanation.finalScore()))
                            .append(" | reasons=")
                            .append(String.join(", ", explanation.reasons()))
                            .append(System.lineSeparator()));
            builder.append(System.lineSeparator());
        }

        builder.append("Project support package: ")
                .append(projectContext.supportPackage())
                .append(System.lineSeparator());
        builder.append("Base page type: ")
                .append(projectContext.basePageType())
                .append(System.lineSeparator());
        builder.append("Base test type: ")
                .append(projectContext.baseTestType())
                .append(System.lineSeparator());
        builder.append("Known support types: ")
                .append(String.join(", ", projectContext.supportTypes()))
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("User request:")
                .append(System.lineSeparator())
                .append(userRequest.trim())
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        builder.append("Relevant project context chunks:")
                .append(System.lineSeparator());
        if (matches.isEmpty()) {
            builder.append("- No indexed chunks found. Fall back to framework conventions only.")
                    .append(System.lineSeparator());
        } else {
            for (RetrievedChunk match : matches) {
                builder.append("---")
                        .append(System.lineSeparator());
                builder.append("File: ").append(match.relativePath())
                        .append(" | artifactType=").append(match.metadata().artifactType())
                        .append(" | artifactName=").append(match.metadata().artifactName())
                        .append(" | language=").append(match.language())
                        .append(" | chunk=").append(match.chunkIndex())
                        .append(" | score=").append(String.format("%.4f", match.score()))
                        .append(System.lineSeparator());
                if (!match.metadata().packageName().isBlank()) {
                    builder.append("Package: ").append(match.metadata().packageName()).append(System.lineSeparator());
                }
                if (!match.metadata().tags().isEmpty()) {
                    builder.append("Tags: ").append(String.join(", ", match.metadata().tags()))
                            .append(System.lineSeparator());
                }
                builder.append(match.text().trim())
                        .append(System.lineSeparator());
            }
        }

        builder.append(System.lineSeparator())
                .append("Output requirements:")
                .append(System.lineSeparator())
                .append("1. Generate Java test code only.")
                .append(System.lineSeparator())
                .append("2. Reuse existing core/ui/template abstractions from the project.")
                .append(System.lineSeparator())
                .append("3. Keep style aligned with the retrieved code.")
                .append(System.lineSeparator())
                .append("4. Do not invent a new framework layer if an existing one already exists.")
                .append(System.lineSeparator())
                .append("5. Prefer page object and base test usage already present in the project.")
                .append(System.lineSeparator());

        return builder.toString();
    }
}
