package ua.demo.agentlab.artifactreuse.registry;

import ua.demo.agentlab.artifactreuse.model.ArtifactRecord;
import ua.demo.agentlab.artifactreuse.model.ArtifactRunRelation;
import ua.demo.agentlab.artifactreuse.model.ArtifactTarget;
import ua.demo.agentlab.artifactreuse.model.QualityGateRecord;
import ua.demo.agentlab.artifactreuse.model.RunRecord;

import java.util.List;

public record ArtifactRegistryWriteRequest(
        ArtifactRecord artifact,
        ArtifactTarget target,
        RunRecord run,
        ArtifactRunRelation runRelation,
        List<QualityGateRecord> qualityGates
) {
    public ArtifactRegistryWriteRequest {
        runRelation = runRelation == null ? ArtifactRunRelation.PRODUCED : runRelation;
        qualityGates = qualityGates == null ? List.of() : List.copyOf(qualityGates);
    }
}
