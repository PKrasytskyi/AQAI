package ua.demo.agentlab.ai.pageenrichment.cache;

import ua.demo.agentlab.ai.pageenrichment.model.PageModelEnrichmentRecord;

public record PageKnowledgeCacheEntry(
        String pageId,
        String pageName,
        String route,
        String pageFingerprintHash,
        PageModelEnrichmentRecord enrichmentRecord,
        String source,
        boolean hit,
        String note
) {
    public PageKnowledgeCacheEntry {
        pageId = safe(pageId);
        pageName = safe(pageName);
        route = safe(route);
        pageFingerprintHash = safe(pageFingerprintHash);
        source = safe(source);
        note = safe(note);
    }

    public static PageKnowledgeCacheEntry miss(String pageId, String pageName, String route, String fingerprint, String note) {
        return new PageKnowledgeCacheEntry(pageId, pageName, route, fingerprint, null, "MISS", false, note);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
