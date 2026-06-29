package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GraphContextExpander {

    private final ProjectCodeGraph projectCodeGraph;

    public GraphContextExpander(ProjectCodeGraph projectCodeGraph) {
        this.projectCodeGraph = Objects.requireNonNull(projectCodeGraph, "projectCodeGraph cannot be null");
    }

    public List<RetrievedChunk> expand(List<RetrievedChunk> semanticSeeds, int maxArtifacts) {
        Objects.requireNonNull(semanticSeeds, "semanticSeeds cannot be null");
        int effectiveLimit = maxArtifacts <= 0 ? semanticSeeds.size() : maxArtifacts;

        Map<String, RetrievedChunk> resultsByPath = new LinkedHashMap<>();
        for (RetrievedChunk seed : semanticSeeds) {
            resultsByPath.putIfAbsent(seed.relativePath(), seed);
        }

        for (RetrievedChunk seed : semanticSeeds) {
            if (resultsByPath.size() >= effectiveLimit) {
                break;
            }
            Set<CodeGraphNode> neighbors = projectCodeGraph.connectedArtifactNeighbors(seed.relativePath());
            for (CodeGraphNode neighbor : neighbors) {
                if (resultsByPath.size() >= effectiveLimit) {
                    break;
                }
                resultsByPath.computeIfAbsent(neighbor.relativePath(), path -> synthesizeChunk(seed, neighbor));
            }
        }

        return new ArrayList<>(resultsByPath.values());
    }

    private RetrievedChunk synthesizeChunk(RetrievedChunk seed, CodeGraphNode neighbor) {
        return new RetrievedChunk(
                "graph:" + neighbor.id(),
                neighbor.relativePath(),
                inferLanguage(neighbor.relativePath()),
                -1,
                Math.max(seed.score() - 0.15d, 0.05d),
                "Graph-related artifact: " + neighbor.displayName()
                        + System.lineSeparator()
                        + "Path: " + neighbor.relativePath()
                        + System.lineSeparator()
                        + neighbor.previewText(),
                new ChunkMetadata(
                        mapNodeType(neighbor.nodeType()),
                        neighbor.displayName(),
                        neighbor.packageName(),
                        neighbor.tags()
                )
        );
    }

    private ua.demo.agentlab.ai.rag.model.ArtifactType mapNodeType(GraphNodeType nodeType) {
        return switch (nodeType) {
            case PAGE_OBJECT -> ua.demo.agentlab.ai.rag.model.ArtifactType.PAGE_OBJECT;
            case TEST_CLASS -> ua.demo.agentlab.ai.rag.model.ArtifactType.TEST_CLASS;
            case BASE_CLASS -> ua.demo.agentlab.ai.rag.model.ArtifactType.BASE_CLASS;
            case API_CLIENT -> ua.demo.agentlab.ai.rag.model.ArtifactType.API_CLIENT;
            case TEST_DATA -> ua.demo.agentlab.ai.rag.model.ArtifactType.TEST_DATA;
            case POLICY_DOC -> ua.demo.agentlab.ai.rag.model.ArtifactType.POLICY;
            case FEATURE_FILE -> ua.demo.agentlab.ai.rag.model.ArtifactType.FEATURE_FILE;
            case CONFIGURATION -> ua.demo.agentlab.ai.rag.model.ArtifactType.CONFIGURATION;
            case UTILITY, METHOD -> ua.demo.agentlab.ai.rag.model.ArtifactType.UTILITY;
            case UNKNOWN -> ua.demo.agentlab.ai.rag.model.ArtifactType.UNKNOWN;
        };
    }

    private String inferLanguage(String relativePath) {
        String lowerPath = relativePath.toLowerCase(java.util.Locale.ROOT);
        if (lowerPath.endsWith(".java")) {
            return "java";
        }
        if (lowerPath.endsWith(".feature")) {
            return "gherkin";
        }
        return "markdown";
    }
}
