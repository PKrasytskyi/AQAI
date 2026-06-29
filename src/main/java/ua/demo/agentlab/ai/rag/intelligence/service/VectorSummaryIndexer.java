package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.VectorSummary;

import java.nio.file.Path;
import java.util.List;

public interface VectorSummaryIndexer {

    int index(Path workspaceRoot, List<VectorSummary> vectorSummaries);
}
