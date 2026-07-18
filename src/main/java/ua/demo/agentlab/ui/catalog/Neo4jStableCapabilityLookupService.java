package ua.demo.agentlab.ui.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.PropertiesNeo4jRuntimeConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Cross-run cache lookup for stable page/component capabilities. It never returns candidate evidence. */
public final class Neo4jStableCapabilityLookupService {
    private final Neo4jRuntimeConfig config;
    private final JsonHttpClient http;

    public Neo4jStableCapabilityLookupService() { this(new PropertiesNeo4jRuntimeConfig(), new JsonHttpClient()); }
    Neo4jStableCapabilityLookupService(Neo4jRuntimeConfig config, JsonHttpClient http) { this.config=config; this.http=http; }

    public List<ConfirmedPageCandidate> findStablePages(ProjectProfile profile) {
        if (profile == null || config == null || !config.enabled() || blank(config.password())) return List.of();
        try {
            JsonNode response = http.post(commitUrl(), Map.of("statements", List.of(Map.of("statement", query(), "parameters", Map.of(
                    "appId", profile.profileId(), "baseUrlHash", sha256(profile.baseUrl()),
                    "schemaVersion", KnowledgeRunMetadata.CURRENT_SCHEMA_VERSION)))), headers());
            List<ConfirmedPageCandidate> result = new ArrayList<>();
            for (JsonNode data : response.path("results").path(0).path("data")) {
                JsonNode row=data.path("row"); if (!row.isArray() || row.size()<4) continue;
                String name=row.get(0).asText(""); String route=row.get(1).asText(""); String capability=row.get(2).asText("GENERIC");
                double confidence=row.get(3).asDouble(0d);
                try { result.add(new ConfirmedPageCandidate(name, route, PageCapability.valueOf(capability), PageSource.DB_STABLE_CACHE, confidence,
                        List.of("neo4j-stable-capability", "confirmed-component-or-locator"))); }
                catch (IllegalArgumentException ignored) { /* unknown capability is not stable evidence */ }
            }
            return List.copyOf(result);
        } catch (RuntimeException ignored) { return List.of(); }
    }
    private String query() {
        return "MATCH (s:UiState)-[:HAS_COMPONENT]->(:UiComponent)-[:HAS_ELEMENT]->"
                + "(:UiSemanticElement)-[:SUPPORTS_ACTION]->(:UiSemanticAction)-[r:USES_LOCATOR]->(l:UiLocatorEvidence) "
                + "WHERE s.appId=$appId AND s.baseUrlHash=$baseUrlHash AND s.schemaVersion=$schemaVersion "
                + "AND l.schemaVersion=$schemaVersion AND l.status='CONFIRMED' "
                + "AND l.evidenceType='CONFIRMED_LOCATOR' AND coalesce(l.validationStatus,'')='PASSED' "
                + "AND coalesce(l.sameOrigin,false)=true AND coalesce(toFloat(l.runtimePassRate),0.0)>=0.90 "
                + "AND coalesce(toFloat(l.flakyRate),1.0)<=0.10 AND coalesce(r.primary,false)=true "
                + "RETURN s.pageName,s.route,s.capability,max(coalesce(toFloat(l.qualityScore),0.0)) ORDER BY s.route";
    }
    private String commitUrl(){ String root=config.httpUrl().endsWith("/")?config.httpUrl().substring(0,config.httpUrl().length()-1):config.httpUrl(); return root+"/db/"+config.database()+"/tx/commit"; }
    private Map<String,String> headers(){return Map.of("Authorization","Basic "+Base64.getEncoder().encodeToString((config.username()+":"+config.password()).getBytes(StandardCharsets.UTF_8)));}
    private boolean blank(String v){return v==null||v.isBlank();}
    private String sha256(String value){ try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest((value==null?"":value.trim()).getBytes(StandardCharsets.UTF_8)); StringBuilder out=new StringBuilder(); for(byte b:bytes)out.append(String.format("%02x",b)); return out.toString(); } catch(Exception e){return "";} }
}
