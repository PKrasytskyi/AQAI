package ua.demo.agentlab.ai.rag.index;

import ua.demo.agentlab.ai.rag.chunking.ProjectDocumentChunker;
import ua.demo.agentlab.ai.rag.model.CodeChunk;
import ua.demo.agentlab.ai.rag.model.RagChunk;
import ua.demo.agentlab.ai.rag.model.SourceDocument;
import ua.demo.agentlab.ai.rag.source.WorkspaceDocumentCollector;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CodebaseIndexer {

    private final WorkspaceDocumentCollector collector;
    private final ProjectDocumentChunker chunker;
    private final ArtifactMetadataResolver artifactMetadataResolver;

    public CodebaseIndexer(WorkspaceDocumentCollector collector, ProjectDocumentChunker chunker) {
        this(collector, chunker, new ArtifactMetadataResolver());
    }

    public CodebaseIndexer(
            WorkspaceDocumentCollector collector,
            ProjectDocumentChunker chunker,
            ArtifactMetadataResolver artifactMetadataResolver
    ) {
        if (collector == null || chunker == null) {
            throw new IllegalArgumentException("CodebaseIndexer dependencies cannot be null");
        }
        if (artifactMetadataResolver == null) {
            throw new IllegalArgumentException("artifactMetadataResolver cannot be null");
        }
        this.collector = collector;
        this.chunker = chunker;
        this.artifactMetadataResolver = artifactMetadataResolver;
    }

    public List<CodeChunk> index(Path workspaceRoot) {
        List<SourceDocument> documents = new ArrayList<>(collector.collect(workspaceRoot));
        documents.sort(Comparator.comparing(SourceDocument::relativePath));

        List<CodeChunk> chunks = new ArrayList<>();
        for (SourceDocument document : documents) {
            ua.demo.agentlab.ai.rag.model.IndexedArtifact indexedArtifact = artifactMetadataResolver.classify(document);
            List<RagChunk> documentChunks = chunker.chunk(document);
            for (RagChunk documentChunk : documentChunks) {
                chunks.add(CodeChunk.fromLegacy(documentChunk, indexedArtifact.toChunkMetadata()));
            }
        }
        return chunks;
    }
}
