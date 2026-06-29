package ua.demo.agentlab.app;

import ua.demo.agentlab.ai.rag.command.IndexProjectCommand;
import ua.demo.agentlab.ai.rag.chunking.ProjectDocumentChunker;
import ua.demo.agentlab.ai.rag.config.PropertiesRagRuntimeConfig;
import ua.demo.agentlab.ai.rag.config.RagRuntimeConfig;
import ua.demo.agentlab.ai.rag.index.ChunkJsonlWriter;
import ua.demo.agentlab.ai.rag.index.CodebaseIndexer;
import ua.demo.agentlab.ai.rag.model.RagGenerationResult;
import ua.demo.agentlab.ai.rag.model.RagIndexingReport;
import ua.demo.agentlab.ai.rag.model.RetrievedChunk;
import ua.demo.agentlab.ai.rag.openai.OpenAiEmbeddingClient;
import ua.demo.agentlab.ai.rag.openai.OpenAiResponseGenerationClient;
import ua.demo.agentlab.ai.rag.prompt.ProjectStylePromptBuilder;
import ua.demo.agentlab.ai.rag.qdrant.QdrantVectorStore;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalRequest;
import ua.demo.agentlab.ai.rag.retrieval.ContextRetrievalResult;
import ua.demo.agentlab.ai.rag.service.RagRetrievalService;
import ua.demo.agentlab.ai.rag.service.RagTestGenerationService;
import ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector;
import ua.demo.agentlab.templates.DefaultProjectContextScanner;
import ua.demo.agentlab.templates.ProjectContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class RagConsoleRunner {

    private RagConsoleRunner() {
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return;
        }

        RagRuntimeConfig config = new PropertiesRagRuntimeConfig();
        ensureEnabled(config);

        String command = args[0].trim().toLowerCase();
        Path workspaceRoot = resolveWorkspace(args);

        OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(config);
        QdrantVectorStore vectorStore = new QdrantVectorStore(config);

        switch (command) {
            case "index" -> runIndexing(workspaceRoot, config, embeddingClient, vectorStore);
            case "search" -> runSearch(workspaceRoot, extractRequest(command, args), config, embeddingClient, vectorStore);
            case "generate" -> runGeneration(workspaceRoot, extractRequest(command, args), config, embeddingClient, vectorStore);
            case "index-and-generate" -> {
                runIndexing(workspaceRoot, config, embeddingClient, vectorStore);
                runGeneration(workspaceRoot, extractRequest(command, args), config, embeddingClient, vectorStore);
            }
            default -> throw new IllegalArgumentException("Unsupported RAG command: " + command);
        }
    }

    private static void runIndexing(
            Path workspaceRoot,
            RagRuntimeConfig config,
            OpenAiEmbeddingClient embeddingClient,
            QdrantVectorStore vectorStore
    ) {
        IndexProjectCommand indexProjectCommand = new IndexProjectCommand(
                new CodebaseIndexer(
                        new WorkspaceDocumentCollector(),
                        new ProjectDocumentChunker(config.chunkMaxChars(), config.chunkOverlapChars())
                ),
                new ChunkJsonlWriter(),
                embeddingClient,
                vectorStore,
                config.collectionName(),
                config.chunksJsonlPath()
        );

        RagIndexingReport report = indexProjectCommand.execute(workspaceRoot);
        System.out.println("RAG indexing completed");
        System.out.println("Workspace: " + workspaceRoot.toAbsolutePath().normalize());
        System.out.println("Collection: " + report.collectionName());
        System.out.println("Documents: " + report.documentsIndexed());
        System.out.println("Chunks: " + report.chunksIndexed());
        System.out.println("Vector size: " + report.vectorSize());
        System.out.println("chunks.jsonl: " + report.chunksJsonlPath());
    }

    private static void runSearch(
            Path workspaceRoot,
            String request,
            RagRuntimeConfig config,
            OpenAiEmbeddingClient embeddingClient,
            QdrantVectorStore vectorStore
    ) {
        RagRetrievalService retrievalService = new RagRetrievalService(
                workspaceRoot,
                embeddingClient,
                vectorStore,
                config.retrievalLimit()
        );
        ContextRetrievalResult result = retrievalService.retrieveContext(new ContextRetrievalRequest(request, 6));
        List<RetrievedChunk> matches = result.contextChunks();
        System.out.println("RAG context matches: " + matches.size());
        System.out.println("Task type: " + result.intent().taskClassification().taskType());
        System.out.println("Task confidence: " + String.format("%.2f", result.intent().taskClassification().confidenceScore()));
        System.out.println("Task signals: " + result.intent().taskClassification().signals());
        System.out.println("Retrieval policy: desired=" + result.retrievalPolicy().desiredArtifacts()
                + ", semanticMultiplier=" + result.retrievalPolicy().semanticRetrievalMultiplier()
                + ", graphMultiplier=" + result.retrievalPolicy().graphExpansionMultiplier());
        System.out.println("Intent domain terms: " + String.join(", ", result.intent().domainTerms()));
        System.out.println("Intent qualifiers: " + String.join(", ", result.intent().qualifiers()));
        System.out.println("Requested artifact types: " + result.intent().requestedArtifactTypes());
        System.out.println("Semantic query: " + result.ragQuery().semanticQuery());
        System.out.println("Raw retrieved matches: " + result.rawMatchCount());
        System.out.println("Graph expanded artifacts: " + result.graphExpandedCount());
        System.out.println("Filtered candidates: " + result.filteredCandidateCount());
        System.out.println("Graph source: " + result.graphSource());
        result.rerankExplanations().stream().limit(5).forEach(explanation ->
                System.out.println("Rerank: " + explanation.relativePath()
                        + " | score=" + String.format("%.4f", explanation.finalScore())
                        + " | reasons=" + String.join(", ", explanation.reasons()))
        );
        for (RetrievedChunk match : matches) {
            System.out.println(match.relativePath()
                    + " | artifactType=" + match.metadata().artifactType()
                    + " | artifactName=" + match.metadata().artifactName()
                    + " | " + match.language()
                    + " | chunk=" + match.chunkIndex()
                    + " | score=" + String.format("%.4f", match.score()));
        }
    }

    private static void runGeneration(
            Path workspaceRoot,
            String request,
            RagRuntimeConfig config,
            OpenAiEmbeddingClient embeddingClient,
            QdrantVectorStore vectorStore
    ) {
        ProjectContext projectContext = new DefaultProjectContextScanner(workspaceRoot).scan();
        RagRetrievalService retrievalService = new RagRetrievalService(
                workspaceRoot,
                embeddingClient,
                vectorStore,
                config.retrievalLimit()
        );
        RagTestGenerationService generationService = new RagTestGenerationService(
                retrievalService,
                new ProjectStylePromptBuilder(),
                new OpenAiResponseGenerationClient(config),
                projectContext
        );

        RagGenerationResult result = generationService.generate(request);
        System.out.println("RAG generation completed");
        System.out.println("Retrieved chunks: " + result.matches().size());
        System.out.println("Task type: " + result.retrievalTrace().taskType());
        System.out.println("Task confidence: " + String.format("%.2f", result.retrievalTrace().taskConfidence()));
        System.out.println("Semantic query: " + result.retrievalTrace().semanticQuery());
        System.out.println("Graph source: " + result.retrievalTrace().graphSource());
        System.out.println();
        System.out.println(result.generatedText());
    }

    private static void ensureEnabled(RagRuntimeConfig config) {
        if (!config.enabled()) {
            throw new IllegalStateException(
                    "RAG layer is disabled. Set rag.enabled=true in framework.properties or via system property."
            );
        }
    }

    private static Path resolveWorkspace(String[] args) {
        if (args.length < 2 || args[1] == null || args[1].isBlank()) {
            return Paths.get(".");
        }
        return Paths.get(args[1].trim()).toAbsolutePath().normalize();
    }

    private static String extractRequest(String command, String[] args) {
        if (args.length < 3 || args[2] == null || args[2].isBlank()) {
            throw new IllegalArgumentException("Command '" + command + "' requires a quoted user request argument");
        }
        return args[2].trim();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  index [workspacePath]");
        System.out.println("  search [workspacePath] \"user request\"");
        System.out.println("  generate [workspacePath] \"user request\"");
        System.out.println("  index-and-generate [workspacePath] \"user request\"");
    }
}
