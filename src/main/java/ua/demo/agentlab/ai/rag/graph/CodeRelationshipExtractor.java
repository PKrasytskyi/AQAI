package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.index.ArtifactMetadataResolver;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.IndexedArtifact;
import ua.demo.agentlab.ai.rag.model.SourceDocument;
import ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeRelationshipExtractor {

    private static final Pattern EXTENDS_PATTERN = Pattern.compile("\\bextends\\s+([A-Z][A-Za-z0-9_]*)");
    private static final Pattern IMPLEMENTS_PATTERN = Pattern.compile("\\bimplements\\s+([A-Z][A-Za-z0-9_,\\s]*)");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^\\s*(public|protected|private)?\\s*(static\\s+)?[A-Za-z0-9_<>\\[\\], ?]+\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\("
    );

    private final WorkspaceDocumentCollector collector;
    private final ArtifactMetadataResolver artifactMetadataResolver;

    public CodeRelationshipExtractor(WorkspaceDocumentCollector collector, ArtifactMetadataResolver artifactMetadataResolver) {
        this.collector = Objects.requireNonNull(collector, "collector cannot be null");
        this.artifactMetadataResolver = Objects.requireNonNull(artifactMetadataResolver, "artifactMetadataResolver cannot be null");
    }

    public ProjectCodeGraph extract(Path workspaceRoot) {
        List<SourceDocument> documents = new ArrayList<>(collector.collect(workspaceRoot));
        Map<String, IndexedArtifact> artifactsByPath = new LinkedHashMap<>();
        Map<String, IndexedArtifact> artifactsByName = new LinkedHashMap<>();
        Map<String, List<String>> methodNamesByArtifactPath = new LinkedHashMap<>();

        ProjectCodeGraph graph = new ProjectCodeGraph();

        for (SourceDocument document : documents) {
            IndexedArtifact artifact = artifactMetadataResolver.classify(document);
            artifactsByPath.put(artifact.relativePath(), artifact);
            artifactsByName.putIfAbsent(artifact.artifactName(), artifact);
            CodeGraphNode artifactNode = createArtifactNode(artifact, document.content());
            graph.addNode(artifactNode);

            if ("java".equalsIgnoreCase(document.language())) {
                List<String> declaredMethodNames = extractMethodNames(document.content());
                methodNamesByArtifactPath.put(artifact.relativePath(), declaredMethodNames);
                for (String methodName : declaredMethodNames) {
                    String methodNodeId = methodNodeId(artifact.relativePath(), methodName);
                    graph.addNode(new CodeGraphNode(
                            methodNodeId,
                            GraphNodeType.METHOD,
                            artifact.relativePath(),
                            methodName,
                            artifact.packageName(),
                            List.of("method"),
                            "Method declared in " + artifact.artifactName()
                    ));
                    graph.addEdge(new CodeGraphEdge(artifact.relativePath(), methodNodeId, GraphEdgeType.DECLARES));
                }
            }
        }

        for (SourceDocument document : documents) {
            IndexedArtifact sourceArtifact = artifactsByPath.get(document.relativePath());
            if (sourceArtifact == null) {
                continue;
            }
            createInheritanceEdges(graph, sourceArtifact, artifactsByName, document.content());
            createUsageEdges(graph, sourceArtifact, artifactsByName, document.content());
            createCallEdges(graph, sourceArtifact, artifactsByPath, methodNamesByArtifactPath, document.content());
        }

        return graph;
    }

    private CodeGraphNode createArtifactNode(IndexedArtifact artifact, String content) {
        return new CodeGraphNode(
                artifact.relativePath(),
                toGraphNodeType(artifact.artifactType()),
                artifact.relativePath(),
                artifact.artifactName(),
                artifact.packageName(),
                artifact.tags(),
                preview(content)
        );
    }

    private void createInheritanceEdges(
            ProjectCodeGraph graph,
            IndexedArtifact sourceArtifact,
            Map<String, IndexedArtifact> artifactsByName,
            String content
    ) {
        Matcher extendsMatcher = EXTENDS_PATTERN.matcher(content);
        while (extendsMatcher.find()) {
            IndexedArtifact target = artifactsByName.get(extendsMatcher.group(1));
            if (target != null) {
                graph.addEdge(new CodeGraphEdge(sourceArtifact.relativePath(), target.relativePath(), GraphEdgeType.EXTENDS));
            }
        }

        Matcher implementsMatcher = IMPLEMENTS_PATTERN.matcher(content);
        while (implementsMatcher.find()) {
            String[] interfaceNames = implementsMatcher.group(1).split(",");
            for (String interfaceName : interfaceNames) {
                IndexedArtifact target = artifactsByName.get(interfaceName.trim());
                if (target != null) {
                    graph.addEdge(new CodeGraphEdge(sourceArtifact.relativePath(), target.relativePath(), GraphEdgeType.IMPLEMENTS));
                }
            }
        }
    }

    private void createUsageEdges(
            ProjectCodeGraph graph,
            IndexedArtifact sourceArtifact,
            Map<String, IndexedArtifact> artifactsByName,
            String content
    ) {
        for (IndexedArtifact candidate : artifactsByName.values()) {
            if (candidate.relativePath().equals(sourceArtifact.relativePath())) {
                continue;
            }
            if (candidate.artifactName().isBlank()) {
                continue;
            }
            Pattern usagePattern = Pattern.compile("\\b" + Pattern.quote(candidate.artifactName()) + "\\b");
            if (usagePattern.matcher(content).find()) {
                graph.addEdge(new CodeGraphEdge(sourceArtifact.relativePath(), candidate.relativePath(), GraphEdgeType.USES));
            }
        }
    }

    private void createCallEdges(
            ProjectCodeGraph graph,
            IndexedArtifact sourceArtifact,
            Map<String, IndexedArtifact> artifactsByPath,
            Map<String, List<String>> methodNamesByArtifactPath,
            String content
    ) {
        for (Map.Entry<String, List<String>> entry : methodNamesByArtifactPath.entrySet()) {
            IndexedArtifact targetArtifact = artifactsByPath.get(entry.getKey());
            if (targetArtifact == null) {
                continue;
            }
            for (String methodName : entry.getValue()) {
                Pattern callPattern = Pattern.compile("\\b" + Pattern.quote(methodName) + "\\s*\\(");
                if (callPattern.matcher(content).find()) {
                    graph.addEdge(new CodeGraphEdge(
                            sourceArtifact.relativePath(),
                            methodNodeId(targetArtifact.relativePath(), methodName),
                            GraphEdgeType.CALLS
                    ));
                }
            }
        }
    }

    private List<String> extractMethodNames(String content) {
        List<String> methodNames = new ArrayList<>();
        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            String methodName = matcher.group(3);
            if (methodName != null && !methodName.isBlank()
                    && !"if".equals(methodName) && !"for".equals(methodName) && !"while".equals(methodName)
                    && !"switch".equals(methodName) && !"catch".equals(methodName)) {
                methodNames.add(methodName.trim());
            }
        }
        return List.copyOf(methodNames);
    }

    private String methodNodeId(String relativePath, String methodName) {
        return relativePath + "#method:" + methodName;
    }

    private GraphNodeType toGraphNodeType(ArtifactType artifactType) {
        return switch (artifactType) {
            case PAGE_OBJECT -> GraphNodeType.PAGE_OBJECT;
            case TEST_CLASS -> GraphNodeType.TEST_CLASS;
            case BASE_CLASS -> GraphNodeType.BASE_CLASS;
            case API_CLIENT -> GraphNodeType.API_CLIENT;
            case TEST_DATA -> GraphNodeType.TEST_DATA;
            case POLICY -> GraphNodeType.POLICY_DOC;
            case FEATURE_FILE -> GraphNodeType.FEATURE_FILE;
            case CONFIGURATION -> GraphNodeType.CONFIGURATION;
            case DOCUMENTATION, UTILITY -> GraphNodeType.UTILITY;
            case UNKNOWN -> GraphNodeType.UNKNOWN;
        };
    }

    private String preview(String content) {
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.length() <= 220) {
            return normalized;
        }
        return normalized.substring(0, 220);
    }
}
