package ua.demo.agentlab.ai.rag.intelligence.service;

import ua.demo.agentlab.ai.rag.intelligence.model.VectorSummary;

import java.nio.file.Path;
import java.util.List;

public class DisabledVectorSummaryIndexer implements VectorSummaryIndexer {

    @Override
    public int index(Path workspaceRoot, List<VectorSummary> vectorSummaries) {
        return 0;
    }
}
