package ua.demo.agentlab.ai.rag.graph;

import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntity;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphEntityType;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelation;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryGraphRelationType;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryKnowledgeSnapshot;
import ua.demo.agentlab.ai.rag.model.ArtifactType;
import ua.demo.agentlab.ai.rag.model.ChunkMetadata;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.retrieval.QueryIntent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PersistentGraphQueryService implements GraphQueryService {

    private final RepositoryKnowledgeSnapshot snapshot;
    private final Map<String, List<RepositoryGraphEntity>> entitiesByPath;
    private final Map<String, RepositoryGraphEntity> entitiesById;
    private final Map<String, List<RepositoryGraphRelation>> outgoingRelations;
    private final Map<String, List<RepositoryGraphRelation>> incomingRelations;

    public PersistentGraphQueryService(RepositoryKnowledgeSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot cannot be null");
        this.entitiesByPath = indexEntitiesByPath(snapshot.graphEntities());
        this.entitiesById = indexEntitiesById(snapshot.graphEntities());
        this.outgoingRelations = indexOutgoingRelations(snapshot.graphRelations());
        this.incomingRelations = indexIncomingRelations(snapshot.graphRelations());
    }

    @Override
    public GraphQueryResult expand(QueryIntent intent, List<RetrievedChunk> semanticSeeds, int maxArtifacts) {
        Objects.requireNonNull(intent, "intent cannot be null");
        Objects.requireNonNull(semanticSeeds, "semanticSeeds cannot be null");
        int effectiveLimit = maxArtifacts <= 0 ? Math.max(semanticSeeds.size() + 3, 6) : maxArtifacts;
        Map<String, RetrievedChunk> expandedByPath = new LinkedHashMap<>();
        Set<String> matchedSeedPaths = new HashSet<>();

        for (RetrievedChunk seed : semanticSeeds) {
            List<RepositoryGraphEntity> seedEntities = findSeedEntities(seed);
            if (!seedEntities.isEmpty()) {
                matchedSeedPaths.add(seed.relativePath());
            }
            for (RepositoryGraphEntity seedEntity : seedEntities) {
                traverseNeighborhood(intent, seed, seedEntity, expandedByPath, effectiveLimit);
                if (expandedByPath.size() >= effectiveLimit) {
                    break;
                }
            }
            if (expandedByPath.size() >= effectiveLimit) {
                break;
            }
        }

        List<RetrievedChunk> ranked = expandedByPath.values().stream()
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(effectiveLimit)
                .toList();
        return new GraphQueryResult(
                ranked,
                matchedSeedPaths.size(),
                ranked.size(),
                "PERSISTENT_REPOSITORY_GRAPH"
        );
    }

    private void traverseNeighborhood(
            QueryIntent intent,
            RetrievedChunk seed,
            RepositoryGraphEntity root,
            Map<String, RetrievedChunk> expandedByPath,
            int effectiveLimit
    ) {
        record GraphHop(String entityId, double score, int depth, RepositoryGraphRelationType viaRelation) {}

        ArrayDeque<GraphHop> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(new GraphHop(root.id(), seed.score(), 0, null));
        visited.add(root.id());

        while (!queue.isEmpty() && expandedByPath.size() < effectiveLimit) {
            GraphHop hop = queue.removeFirst();
            if (hop.depth() > 2) {
                continue;
            }
            for (RepositoryGraphRelation relation : adjacentRelations(hop.entityId())) {
                String neighborId = relation.fromId().equals(hop.entityId())
                        ? relation.toId()
                        : relation.fromId();
                if (!visited.add(neighborId)) {
                    continue;
                }
                RepositoryGraphEntity neighbor = entitiesById.get(neighborId);
                if (neighbor == null || neighbor.relativePath().isBlank() || neighbor.relativePath().equals(seed.relativePath())) {
                    continue;
                }
                double score = scoreNeighbor(intent, seed, neighbor, relation.type(), hop.depth() + 1);
                RetrievedChunk synthesized = synthesizeChunk(seed, neighbor, relation.type(), hop.depth() + 1, score);
                expandedByPath.merge(neighbor.relativePath(), synthesized, (left, right) ->
                        left.score() >= right.score() ? left : right);
                queue.addLast(new GraphHop(neighborId, score, hop.depth() + 1, relation.type()));
            }
        }
    }

    private List<RepositoryGraphRelation> adjacentRelations(String entityId) {
        List<RepositoryGraphRelation> relations = new ArrayList<>();
        relations.addAll(outgoingRelations.getOrDefault(entityId, List.of()));
        relations.addAll(incomingRelations.getOrDefault(entityId, List.of()));
        return relations;
    }

    private List<RepositoryGraphEntity> findSeedEntities(RetrievedChunk seed) {
        List<RepositoryGraphEntity> byPath = entitiesByPath.getOrDefault(seed.relativePath(), List.of());
        if (!byPath.isEmpty()) {
            return byPath;
        }
        String artifactName = seed.metadata().artifactName();
        if (artifactName == null || artifactName.isBlank()) {
            return List.of();
        }
        return snapshot.graphEntities().stream()
                .filter(entity -> entity.name().equalsIgnoreCase(artifactName))
                .toList();
    }

    private RetrievedChunk synthesizeChunk(
            RetrievedChunk seed,
            RepositoryGraphEntity entity,
            RepositoryGraphRelationType relationType,
            int depth,
            double score
    ) {
        return new RetrievedChunk(
                "repo-graph:" + entity.id(),
                entity.relativePath(),
                inferLanguage(entity.relativePath()),
                -1,
                score,
                """
                Graph-related repository artifact
                Source artifact: %s
                Related artifact: %s
                Relation: %s
                Depth: %d
                Tags: %s
                """.formatted(
                        seed.relativePath(),
                        entity.name(),
                        relationType.name(),
                        depth,
                        String.join(", ", entity.tags())
                ),
                new ChunkMetadata(
                        mapArtifactType(entity.type()),
                        entity.name(),
                        "",
                        entity.tags()
                )
        );
    }

    private double scoreNeighbor(
            QueryIntent intent,
            RetrievedChunk seed,
            RepositoryGraphEntity entity,
            RepositoryGraphRelationType relationType,
            int depth
    ) {
        double score = Math.max(seed.score() - (depth * 0.18d), 0.05d);
        score += relationWeight(relationType);
        ArtifactType artifactType = mapArtifactType(entity.type());
        if (intent.requestedArtifactTypes().contains(artifactType)) {
            score += 0.75d;
        }
        String searchable = (entity.name() + " " + entity.relativePath() + " " + String.join(" ", entity.tags()))
                .toLowerCase(Locale.ROOT);
        for (String domainTerm : intent.domainTerms()) {
            if (searchable.contains(domainTerm.toLowerCase(Locale.ROOT))) {
                score += 0.35d;
            }
        }
        for (String qualifier : intent.qualifiers()) {
            if (searchable.contains(qualifier.toLowerCase(Locale.ROOT))) {
                score += 0.2d;
            }
        }
        return score;
    }

    private double relationWeight(RepositoryGraphRelationType relationType) {
        return switch (relationType) {
            case USES -> 0.50d;
            case EXTENDS -> 0.45d;
            case IMPLEMENTS -> 0.42d;
            case CALLS -> 0.38d;
            case DECLARES, DECLARES_ROUTE -> 0.34d;
            case MATCHES_OPENAPI -> 0.32d;
            case ENRICHES, TRACES_TO -> 0.36d;
            case DETECTED_AS, BELONGS_TO -> 0.20d;
        };
    }

    private ArtifactType mapArtifactType(RepositoryGraphEntityType entityType) {
        return switch (entityType) {
            case TEST_CLASS -> ArtifactType.TEST_CLASS;
            case CONTROLLER -> ArtifactType.API_CLIENT;
            case ROUTE, OPENAPI_ENDPOINT -> ArtifactType.API_CLIENT;
            case DTO_MODEL -> ArtifactType.TEST_DATA;
            case SERVICE_COMPONENT, CODE_ARTIFACT -> ArtifactType.UTILITY;
            case KNOWLEDGE_ENRICHMENT -> ArtifactType.DOCUMENTATION;
            case FRAMEWORK -> ArtifactType.DOCUMENTATION;
        };
    }

    private String inferLanguage(String relativePath) {
        String normalized = relativePath.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".java")) {
            return "java";
        }
        if (normalized.endsWith(".feature")) {
            return "gherkin";
        }
        if (normalized.endsWith(".yaml") || normalized.endsWith(".yml")) {
            return "yaml";
        }
        if (normalized.endsWith(".properties")) {
            return "properties";
        }
        return "markdown";
    }

    private Map<String, List<RepositoryGraphEntity>> indexEntitiesByPath(List<RepositoryGraphEntity> entities) {
        Map<String, List<RepositoryGraphEntity>> index = new HashMap<>();
        for (RepositoryGraphEntity entity : entities) {
            index.computeIfAbsent(entity.relativePath(), ignored -> new ArrayList<>()).add(entity);
        }
        return index;
    }

    private Map<String, RepositoryGraphEntity> indexEntitiesById(List<RepositoryGraphEntity> entities) {
        Map<String, RepositoryGraphEntity> index = new HashMap<>();
        for (RepositoryGraphEntity entity : entities) {
            index.put(entity.id(), entity);
        }
        return index;
    }

    private Map<String, List<RepositoryGraphRelation>> indexOutgoingRelations(List<RepositoryGraphRelation> relations) {
        Map<String, List<RepositoryGraphRelation>> index = new HashMap<>();
        for (RepositoryGraphRelation relation : relations) {
            index.computeIfAbsent(relation.fromId(), ignored -> new ArrayList<>()).add(relation);
        }
        return index;
    }

    private Map<String, List<RepositoryGraphRelation>> indexIncomingRelations(List<RepositoryGraphRelation> relations) {
        Map<String, List<RepositoryGraphRelation>> index = new HashMap<>();
        for (RepositoryGraphRelation relation : relations) {
            index.computeIfAbsent(relation.toId(), ignored -> new ArrayList<>()).add(relation);
        }
        return index;
    }
}
