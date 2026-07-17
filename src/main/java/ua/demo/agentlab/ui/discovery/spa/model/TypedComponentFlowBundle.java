package ua.demo.agentlab.ui.discovery.spa.model;

import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import java.util.List;

public record TypedComponentFlowBundle(KnowledgeRunMetadata runMetadata, List<TypedComponentFlow> flows, List<String> notes) {
    public TypedComponentFlowBundle { flows=flows==null?List.of():List.copyOf(flows); notes=notes==null?List.of():List.copyOf(notes); }
}
