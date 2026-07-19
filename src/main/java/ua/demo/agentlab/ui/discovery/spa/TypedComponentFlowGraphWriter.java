package ua.demo.agentlab.ui.discovery.spa;

import ua.demo.agentlab.ai.rag.http.JsonHttpClient;
import ua.demo.agentlab.ui.discovery.persistence.knowledge.config.Neo4jRuntimeConfig;
import ua.demo.agentlab.ui.discovery.spa.model.TypedComponentFlow;
import ua.demo.agentlab.ui.discovery.spa.model.TypedComponentFlowBundle;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persists candidate component flows separately from promoted locator/action evidence. */
public final class TypedComponentFlowGraphWriter {
    private final Neo4jRuntimeConfig config; private final JsonHttpClient http;
    public TypedComponentFlowGraphWriter(Neo4jRuntimeConfig config){this(config,new JsonHttpClient());}
    TypedComponentFlowGraphWriter(Neo4jRuntimeConfig config, JsonHttpClient http){this.config=config;this.http=http;}
    public void persist(TypedComponentFlowBundle bundle){
        if(bundle==null||bundle.flows().isEmpty()||config==null||!config.enabled()||config.password()==null||config.password().isBlank()) return;
        try {
            var metadata=bundle.runMetadata(); if(metadata==null) return;
            List<Map<String,Object>> flows=bundle.flows().stream().map(flow -> payload(metadata, flow)).toList();
            http.post(commitUrl(),Map.of("statements",List.of(Map.of("statement",query(),"parameters",Map.of("flows",flows)))),headers());
        } catch(RuntimeException ignored) { /* Candidate flow persistence must not break discovery. */ }
    }
    private Map<String,Object> payload(ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata metadata, TypedComponentFlow flow) {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("appId",metadata.appId()); value.put("baseUrlHash",metadata.baseUrlHash()); value.put("schemaVersion",metadata.schemaVersion());
        value.put("pageId",flow.pageId()); value.put("flowId",flow.flowId()); value.put("route",flow.route()); value.put("type",flow.type().name());
        value.put("componentIds",String.join("|",flow.componentIds())); value.put("actionIds",String.join("|",flow.actionIds()));
        value.put("requiredLocatorIds",String.join("|",flow.requiredLocatorIds())); value.put("targetRoute",flow.targetRoute()); value.put("confidence",flow.confidence()); value.put("status",flow.status().name());
        return value;
    }
    private String query(){return "UNWIND $flows AS f MERGE (flow:SpaTypedComponentFlow {appId:f.appId,baseUrlHash:f.baseUrlHash,schemaVersion:f.schemaVersion,pageId:f.pageId,flowId:f.flowId}) ON CREATE SET flow.status='CANDIDATE' SET flow += f";}
    private String commitUrl(){String root=config.httpUrl().endsWith("/")?config.httpUrl().substring(0,config.httpUrl().length()-1):config.httpUrl();return root+"/db/"+config.database()+"/tx/commit";}
    private Map<String,String> headers(){return Map.of("Authorization","Basic "+Base64.getEncoder().encodeToString((config.username()+":"+config.password()).getBytes(StandardCharsets.UTF_8)));}
}
