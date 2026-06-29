package ua.demo.agentlab.ai.rag.chunking;

import ua.demo.agentlab.ai.rag.model.RagChunk;
import ua.demo.agentlab.ai.rag.model.SourceDocument;

import java.util.ArrayList;
import java.util.List;

public class ProjectDocumentChunker {

    private final int maxChars;
    private final int overlapChars;

    public ProjectDocumentChunker(int maxChars, int overlapChars) {
        if (maxChars < 200) {
            throw new IllegalArgumentException("maxChars must be at least 200");
        }
        if (overlapChars < 0 || overlapChars >= maxChars) {
            throw new IllegalArgumentException("overlapChars must be >= 0 and < maxChars");
        }
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    public List<RagChunk> chunk(SourceDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("document cannot be null");
        }

        String normalized = normalize(document.content());
        List<RagChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + maxChars);
            if (end < normalized.length()) {
                int preferredBoundary = findPreferredBoundary(normalized, start, end);
                if (preferredBoundary > start + 100) {
                    end = preferredBoundary;
                }
            }

            String text = normalized.substring(start, end).trim();
            if (!text.isBlank()) {
                chunks.add(new RagChunk(
                        null,
                        document.path(),
                        document.relativePath(),
                        document.language(),
                        index,
                        start,
                        end,
                        text
                ));
                index++;
            }

            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - overlapChars);
        }

        return chunks;
    }

    private int findPreferredBoundary(String text, int start, int end) {
        int paragraph = text.lastIndexOf("\n\n", end - 1);
        if (paragraph > start) {
            return paragraph;
        }
        int line = text.lastIndexOf('\n', end - 1);
        if (line > start) {
            return line;
        }
        int sentence = text.lastIndexOf(". ", end - 1);
        if (sentence > start) {
            return sentence + 1;
        }
        return end;
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }
}
