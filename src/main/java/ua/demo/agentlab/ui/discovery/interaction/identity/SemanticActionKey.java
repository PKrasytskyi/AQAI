package ua.demo.agentlab.ui.discovery.interaction.identity;

/** Stable action identity. It deliberately does not contain a locator ID. */
public record SemanticActionKey(SemanticElementKey elementKey, String intent) {
    public SemanticActionKey {
        if (elementKey == null) throw new IllegalArgumentException("Semantic action requires an element key");
        intent = CanonicalIdentity.text(intent).toUpperCase(java.util.Locale.ROOT).replace('-', '_');
        if (intent.isBlank()) throw new IllegalArgumentException("Semantic action intent is required");
    }

    public String value() {
        return elementKey.value() + ":action:" + intent.toLowerCase(java.util.Locale.ROOT).replace('_', '-');
    }
}
