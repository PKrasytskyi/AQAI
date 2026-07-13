package ua.demo.agentlab.artifactreuse.model;

public final class ArtifactReuseTerminology {

    private ArtifactReuseTerminology() {
    }

    public static final String ARTIFACT =
            "Generated or persisted output such as a POM contract, API spec, prompt context, quality summary, or test data plan.";
    public static final String KNOWLEDGE =
            "Structured application facts such as page, route, locator, component, capability, state, assertion, or flow.";
    public static final String ARTIFACT_REGISTRY =
            "Metadata store for artifact fingerprints, quality status, file paths, lifecycle state, and reuse counters.";
    public static final String QA_KNOWLEDGE_GRAPH =
            "Graph of pages, flows, components, states, capabilities, assertions, locators, and their relationships.";
    public static final String REUSE_POLICY =
            "Deterministic decision layer that returns REUSE_STABLE, CALL_LLM, or FORCE_REFRESH.";
    public static final String REUSE_PLANNER =
            "Future semantic planner that decides which known pages, paths, components, or artifacts can support a new requirement.";

    public static boolean pomContractReuseBelongsToArtifactLayer() {
        return true;
    }

    public static boolean flowPathReuseBelongsToKnowledgeGraph() {
        return true;
    }
}
