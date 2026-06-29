package ua.demo.agentlab.ai.rag.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProjectCodeGraph {

    private final Map<String, CodeGraphNode> nodesById = new LinkedHashMap<>();
    private final Map<String, String> artifactNodeIdByPath = new LinkedHashMap<>();
    private final Map<String, List<CodeGraphEdge>> outgoingEdges = new LinkedHashMap<>();
    private final Map<String, List<CodeGraphEdge>> incomingEdges = new LinkedHashMap<>();

    public void addNode(CodeGraphNode node) {
        if (node == null) {
            throw new IllegalArgumentException("node cannot be null");
        }
        nodesById.put(node.id(), node);
        if (node.nodeType() != GraphNodeType.METHOD && node.relativePath() != null && !node.relativePath().isBlank()) {
            artifactNodeIdByPath.putIfAbsent(node.relativePath(), node.id());
        }
    }

    public void addEdge(CodeGraphEdge edge) {
        if (edge == null) {
            throw new IllegalArgumentException("edge cannot be null");
        }
        if (!nodesById.containsKey(edge.fromNodeId()) || !nodesById.containsKey(edge.toNodeId())) {
            return;
        }
        if (containsEdge(edge)) {
            return;
        }
        outgoingEdges.computeIfAbsent(edge.fromNodeId(), key -> new ArrayList<>()).add(edge);
        incomingEdges.computeIfAbsent(edge.toNodeId(), key -> new ArrayList<>()).add(edge);
    }

    public CodeGraphNode node(String nodeId) {
        return nodesById.get(nodeId);
    }

    public CodeGraphNode artifactByPath(String relativePath) {
        String nodeId = artifactNodeIdByPath.get(relativePath);
        return nodeId == null ? null : nodesById.get(nodeId);
    }

    public Collection<CodeGraphNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    public List<CodeGraphEdge> outgoing(String nodeId) {
        return List.copyOf(outgoingEdges.getOrDefault(nodeId, List.of()));
    }

    public List<CodeGraphEdge> incoming(String nodeId) {
        return List.copyOf(incomingEdges.getOrDefault(nodeId, List.of()));
    }

    public List<CodeGraphEdge> edges() {
        List<CodeGraphEdge> edges = new ArrayList<>();
        for (List<CodeGraphEdge> values : outgoingEdges.values()) {
            edges.addAll(values);
        }
        return List.copyOf(edges);
    }

    public Set<CodeGraphNode> connectedArtifactNeighbors(String relativePath) {
        CodeGraphNode artifactNode = artifactByPath(relativePath);
        if (artifactNode == null) {
            return Set.of();
        }

        Set<CodeGraphNode> neighbors = new LinkedHashSet<>();
        collectArtifactNeighbors(artifactNode.id(), neighbors, true);
        collectArtifactNeighbors(artifactNode.id(), neighbors, false);
        neighbors.removeIf(node -> node.id().equals(artifactNode.id()));
        return Set.copyOf(neighbors);
    }

    public CodeGraphNode declaringArtifactOfMethod(String methodNodeId) {
        for (CodeGraphEdge edge : incoming(methodNodeId)) {
            if (edge.edgeType() == GraphEdgeType.DECLARES) {
                return node(edge.fromNodeId());
            }
        }
        return null;
    }

    private void collectArtifactNeighbors(String nodeId, Set<CodeGraphNode> neighbors, boolean outgoing) {
        List<CodeGraphEdge> edges = outgoing ? outgoing(nodeId) : incoming(nodeId);
        for (CodeGraphEdge edge : edges) {
            String otherNodeId = outgoing ? edge.toNodeId() : edge.fromNodeId();
            CodeGraphNode otherNode = node(otherNodeId);
            if (otherNode == null) {
                continue;
            }
            if (otherNode.nodeType() == GraphNodeType.METHOD) {
                CodeGraphNode declaringArtifact = declaringArtifactOfMethod(otherNode.id());
                if (declaringArtifact != null) {
                    neighbors.add(declaringArtifact);
                }
                continue;
            }
            neighbors.add(otherNode);
        }
    }

    private boolean containsEdge(CodeGraphEdge edge) {
        return outgoingEdges.getOrDefault(edge.fromNodeId(), List.of()).stream()
                .anyMatch(existing -> existing.toNodeId().equals(edge.toNodeId())
                        && existing.edgeType() == edge.edgeType());
    }
}
