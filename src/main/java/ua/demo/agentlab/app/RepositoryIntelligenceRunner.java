package ua.demo.agentlab.app;

import ua.demo.agentlab.ai.rag.config.PropertiesRagRuntimeConfig;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.intelligence.model.RepositoryIntelligenceReport;
import ua.demo.agentlab.ai.rag.intelligence.service.KnowledgeEnrichmentService;
import ua.demo.agentlab.ai.rag.intelligence.service.OpenAiKnowledgeEnrichmentClient;
import ua.demo.agentlab.ai.rag.intelligence.service.QdrantVectorSummaryIndexer;
import ua.demo.agentlab.ai.rag.intelligence.service.RepositoryIntelligenceIndexer;
import ua.demo.agentlab.ai.rag.intelligence.service.VectorSummaryIndexer;
import ua.demo.agentlab.ai.rag.intelligence.store.LocalPersistentGraphStore;
import ua.demo.agentlab.ai.rag.intelligence.store.LocalRepositoryKnowledgeStore;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class RepositoryIntelligenceRunner {

    private RepositoryIntelligenceRunner() {
    }

    public static void main(String[] args) {
        Path workspaceRoot = args != null && args.length > 0 && args[0] != null && !args[0].isBlank()
                ? Paths.get(args[0]).toAbsolutePath().normalize()
                : Paths.get(".").toAbsolutePath().normalize();
        Path outputDirectory = args != null && args.length > 1 && args[1] != null && !args[1].isBlank()
                ? Paths.get(args[1]).toAbsolutePath().normalize()
                : workspaceRoot.resolve("target/repository-intelligence");
        RagRuntimeConfig ragRuntimeConfig = new PropertiesRagRuntimeConfig();
        VectorSummaryIndexer vectorSummaryIndexer = ragRuntimeConfig.enabled()
                && ragRuntimeConfig.openAiApiKey() != null
                && !ragRuntimeConfig.openAiApiKey().isBlank()
                ? new QdrantVectorSummaryIndexer(
                        new OpenAiEmbeddingClient(ragRuntimeConfig),
                        new QdrantVectorStore(ragRuntimeConfig)
                )
                : new ua.demo.agentlab.ai.rag.intelligence.service.DisabledVectorSummaryIndexer();
        KnowledgeEnrichmentService knowledgeEnrichmentService = ragRuntimeConfig.enabled()
                && ragRuntimeConfig.openAiApiKey() != null
                && !ragRuntimeConfig.openAiApiKey().isBlank()
                ? new KnowledgeEnrichmentService(new OpenAiKnowledgeEnrichmentClient(ragRuntimeConfig))
                : new KnowledgeEnrichmentService();

        RepositoryIntelligenceReport report = new RepositoryIntelligenceIndexer(
                ua.demo.agentlab.ai.rag.intelligence.service.RepositoryIntelligenceConfig.defaults(outputDirectory),
                new LocalRepositoryKnowledgeStore(),
                new LocalPersistentGraphStore(),
                vectorSummaryIndexer,
                knowledgeEnrichmentService
        ).index(workspaceRoot, outputDirectory);

        System.out.println("Repository intelligence indexing completed");
        System.out.println("Workspace: " + workspaceRoot);
        System.out.println("Output: " + report.outputDirectory());
        System.out.println("Scanned files: " + report.scannedFiles());
        System.out.println("Warnings: " + report.warnings());
        System.out.println("Changed documents: " + report.changedDocuments());
        System.out.println("Reused documents: " + report.reusedDocuments());
        System.out.println("Controller routes: " + report.controllerRoutes());
        System.out.println("DTO/models: " + report.dtoModels());
        System.out.println("Services/repositories: " + report.layerComponents());
        System.out.println("Existing tests: " + report.existingTests());
        System.out.println("OpenAPI endpoints: " + report.openApiEndpoints());
        System.out.println("Endpoint matches: " + report.endpointMatches());
        System.out.println("Graph entities: " + report.graphEntities());
        System.out.println("Graph relations: " + report.graphRelations());
        System.out.println("Knowledge enrichments: " + report.knowledgeEnrichments());
        System.out.println("Vector summaries: " + report.vectorSummaries());
        System.out.println("Vector summaries indexed: " + report.vectorSummariesIndexed());
        System.out.println("Initial test ideas: " + report.initialTestIdeas());
    }
}
