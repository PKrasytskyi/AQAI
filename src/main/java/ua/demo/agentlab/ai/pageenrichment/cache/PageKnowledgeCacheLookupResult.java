package ua.demo.agentlab.ai.pageenrichment.cache;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record PageKnowledgeCacheLookupResult(
        List<PageKnowledgeCacheEntry> entries,
        String source,
        String note
) {
    public PageKnowledgeCacheLookupResult {
        entries = entries == null ? List.of() : List.copyOf(entries);
        source = source == null ? "" : source.trim();
        note = note == null ? "" : note.trim();
    }

    public List<PageModelEnrichmentRecord> cachedRecords() {
        return entries.stream()
                .filter(PageKnowledgeCacheEntry::hit)
                .map(PageKnowledgeCacheEntry::enrichmentRecord)
                .filter(record -> record != null)
                .toList();
    }

    public Optional<PageKnowledgeCacheEntry> hitFor(String pageId, String fingerprint) {
        String normalizedPageId = normalize(pageId);
        String normalizedFingerprint = normalize(fingerprint);
        return entries.stream()
                .filter(PageKnowledgeCacheEntry::hit)
                .filter(entry -> normalize(entry.pageId()).equals(normalizedPageId))
                .filter(entry -> normalize(entry.pageFingerprintHash()).equals(normalizedFingerprint))
                .findFirst();
    }

    public Map<String, String> fingerprintByPageId() {
        return entries.stream()
                .collect(Collectors.toMap(
                        entry -> normalize(entry.pageId()),
                        PageKnowledgeCacheEntry::pageFingerprintHash,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new
                ));
    }

    public long hitCount() {
        return entries.stream().filter(PageKnowledgeCacheEntry::hit).count();
    }

    public long missCount() {
        return entries.stream().filter(entry -> !entry.hit()).count();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
