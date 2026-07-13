package ua.demo.agentlab.artifactreuse.registry.neo4j;

import java.util.List;
import java.util.Map;

public class Neo4jArtifactRegistrySchema {

    public List<Map<String, Object>> constraintStatements() {
        return List.of(
                statement("CREATE CONSTRAINT artifact_artifact_id IF NOT EXISTS "
                        + "FOR (a:Artifact) REQUIRE a.artifactId IS UNIQUE"),
                statement("CREATE CONSTRAINT artifact_target_key IF NOT EXISTS "
                        + "FOR (t:ArtifactTarget) REQUIRE t.targetKey IS UNIQUE"),
                statement("CREATE CONSTRAINT artifact_registry_run_id IF NOT EXISTS "
                        + "FOR (r:ArtifactRegistryRun) REQUIRE r.runId IS UNIQUE"),
                statement("CREATE CONSTRAINT artifact_validation_gate_id IF NOT EXISTS "
                        + "FOR (q:ArtifactValidationGate) REQUIRE q.gateId IS UNIQUE")
        );
    }

    private Map<String, Object> statement(String cypher) {
        return Map.of("statement", cypher, "parameters", Map.of());
    }
}
