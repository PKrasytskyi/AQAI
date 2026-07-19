package ua.demo.agentlab.ui.discovery.interaction.identity;

/** Stable identity for one selector alternative belonging to a semantic element. */
public record LocatorEvidenceId(SemanticElementKey elementKey, String selectorFamily, String fingerprint) {
    public LocatorEvidenceId {
        if (elementKey == null) throw new IllegalArgumentException("Locator evidence requires an element key");
        selectorFamily = CanonicalIdentity.text(selectorFamily);
        fingerprint = CanonicalIdentity.text(fingerprint);
        if (selectorFamily.isBlank() || fingerprint.isBlank()) {
            throw new IllegalArgumentException("Locator strategy family and fingerprint are required");
        }
    }

    public static LocatorEvidenceId of(SemanticElementKey elementKey, String strategy, String value) {
        String family = CanonicalIdentity.text(strategy);
        return new LocatorEvidenceId(elementKey, family,
                CanonicalIdentity.hash(family + "|" + (value == null ? "" : value.trim())));
    }

    public String value() {
        return elementKey.value() + ":locator:" + selectorFamily + ":" + fingerprint;
    }
}
