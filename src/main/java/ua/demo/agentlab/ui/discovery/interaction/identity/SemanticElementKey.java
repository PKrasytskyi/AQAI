package ua.demo.agentlab.ui.discovery.interaction.identity;

/** Stable element identity independent from a DOM occurrence suffix or locator strategy. */
public record SemanticElementKey(
        String pageId,
        String stateId,
        String componentId,
        String semanticRole,
        String domAnchorHash
) {
    public SemanticElementKey {
        pageId = CanonicalIdentity.text(pageId);
        stateId = CanonicalIdentity.text(stateId);
        componentId = CanonicalIdentity.text(componentId);
        semanticRole = CanonicalIdentity.occurrenceText(semanticRole);
        domAnchorHash = CanonicalIdentity.text(domAnchorHash);
        if (pageId.isBlank() || componentId.isBlank() || semanticRole.isBlank()) {
            throw new IllegalArgumentException("SemanticElementKey requires page, component, and semantic role");
        }
    }

    public static SemanticElementKey of(
            String pageId, String stateId, String componentId, String semanticRole
    ) {
        String normalizedRole = CanonicalIdentity.occurrenceText(semanticRole);
        return new SemanticElementKey(pageId, stateId, componentId, normalizedRole,
                CanonicalIdentity.hash(CanonicalIdentity.text(pageId) + "|"
                        + CanonicalIdentity.text(componentId) + "|" + normalizedRole));
    }

    public String value() {
        return String.join(":", pageId, stateId.isBlank() ? "base" : stateId, componentId, semanticRole, domAnchorHash);
    }
}
