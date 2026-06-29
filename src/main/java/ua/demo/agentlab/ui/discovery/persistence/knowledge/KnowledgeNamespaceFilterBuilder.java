package ua.demo.agentlab.ui.discovery.persistence.knowledge;

import java.util.LinkedHashMap;
import java.util.Map;

public class KnowledgeNamespaceFilterBuilder {

    public Map<String, String> currentRunOnly(KnowledgeRunMetadata runMetadata) {
        Map<String, String> filter = new LinkedHashMap<>();
        if (runMetadata == null) {
            return Map.copyOf(filter);
        }
        putIfPresent(filter, "runId", runMetadata.runId());
        putIfPresent(filter, "appId", runMetadata.appId());
        putIfPresent(filter, "baseUrlHash", runMetadata.baseUrlHash());
        putIfPresent(filter, "requirementSetHash", runMetadata.requirementSetHash());
        putIfPresent(filter, "discoverySessionId", runMetadata.discoverySessionId());
        putIfPresent(filter, "schemaVersion", runMetadata.schemaVersion());
        return Map.copyOf(filter);
    }

    public Map<String, String> stablePageCache(
            KnowledgeRunMetadata runMetadata,
            String pageId,
            String pageFingerprintHash,
            String enrichmentCacheVersion
    ) {
        Map<String, String> filter = stableCacheBaseFilter(runMetadata);
        putIfPresent(filter, "pageId", pageId);
        putIfPresent(filter, "pageFingerprintHash", pageFingerprintHash);
        putIfPresent(filter, "enrichmentCacheVersion", enrichmentCacheVersion);
        return Map.copyOf(filter);
    }

    private Map<String, String> stableCacheBaseFilter(KnowledgeRunMetadata runMetadata) {
        Map<String, String> filter = new LinkedHashMap<>();
        if (runMetadata == null) {
            return filter;
        }
        putIfPresent(filter, "appId", runMetadata.appId());
        putIfPresent(filter, "baseUrlHash", runMetadata.baseUrlHash());
        putIfPresent(filter, "schemaVersion", runMetadata.schemaVersion());
        return filter;
    }

    private void putIfPresent(Map<String, String> target, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return;
        }
        target.put(key, value.trim());
    }
}
