package ua.demo.agentlab.ai.rag.service;

import ua.demo.agentlab.ai.rag.embedding.EmbeddingService;
import ua.demo.agentlab.ai.rag.graph.CodeRelationshipExtractor;
import ua.demo.agentlab.ai.rag.graph.FallbackGraphQueryService;
import ua.demo.agentlab.ai.rag.graph.GraphContextExpander;
import ua.demo.agentlab.ai.rag.graph.GraphQueryService;
import ua.demo.agentlab.ai.rag.graph.PersistentGraphQueryService;
import ua.demo.agentlab.ai.rag.graph.ProjectCodeGraph;
import ua.demo.agentlab.ai.rag.index.ArtifactMetadataResolver;
import ua.demo.agentlab.ai.rag.intelligence.store.LocalRepositoryKnowledgeStore;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.retrieval.ContextAssembler;
import ua.demo.agentlab.ai.rag.retrieval.ContextReranker;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalRequest;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalResult;
import ua.demo.agentlab.ai.rag.retrieval.HybridContextRetriever;
import ua.demo.agentlab.ai.rag.retrieval.MetadataContextFilter;
import ua.demo.agentlab.ai.rag.retrieval.ProjectContextRetriever;
import ua.demo.agentlab.ai.rag.retrieval.RagQueryBuilder;
import ua.demo.agentlab.ai.rag.retrieval.QueryIntentResolver;
import ua.demo.agentlab.ai.rag.retrieval.RetrievalPolicyResolver;
import ua.demo.agentlab.ai.rag.retrieval.Retriever;
import ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector;
import ua.demo.agentlab.ai.rag.store.VectorStore;

import java.nio.file.Path;
import java.util.List;

public class RagRetrievalService {

    private final Retriever retriever;
    private final ProjectContextRetriever projectContextRetriever;
    private final HybridContextRetriever hybridContextRetriever;

    public RagRetrievalService(
            Path workspaceRoot,
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            int retrievalLimit
    ) {
        this.retriever = new Retriever(embeddingService, vectorStore, retrievalLimit);
        this.projectContextRetriever = new ProjectContextRetriever(
                retriever,
                new QueryIntentResolver(),
                new RagQueryBuilder(),
                new RetrievalPolicyResolver(),
                new ContextAssembler()
        );
        GraphQueryService graphQueryService = buildGraphQueryService(workspaceRoot);
        this.hybridContextRetriever = new HybridContextRetriever(
                projectContextRetriever,
                graphQueryService,
                new MetadataContextFilter(),
                new ContextReranker()
        );
    }

    public List<RetrievedChunk> search(String request) {
        return retriever.retrieve(request);
    }

    public Retriever retriever() {
        return retriever;
    }

    public ContextRetrievalResult retrieveContext(ContextRetrievalRequest request) {
        return hybridContextRetriever.retrieve(request);
    }

    public ProjectContextRetriever projectContextRetriever() {
        return projectContextRetriever;
    }

    public HybridContextRetriever hybridContextRetriever() {
        return hybridContextRetriever;
    }

    private GraphQueryService buildGraphQueryService(Path workspaceRoot) {
        return new LocalRepositoryKnowledgeStore()
                .load(workspaceRoot.resolve("target/repository-intelligence"))
                .<GraphQueryService>map(PersistentGraphQueryService::new)
                .orElseGet(() -> {
                    ProjectCodeGraph projectCodeGraph = new CodeRelationshipExtractor(
                            new WorkspaceDocumentCollector(),
                            new ArtifactMetadataResolver()
                    ).extract(workspaceRoot);
                    return new FallbackGraphQueryService(new GraphContextExpander(projectCodeGraph));
                });
    }
}
