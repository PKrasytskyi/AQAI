package ua.demo.agentlab.ui.discovery.runtime.bidi;

public record BiDiDiscoveryConfig(
        boolean enabled,
        boolean collectNetwork,
        boolean collectConsole,
        boolean collectDomMutations,
        int maxBufferedEvents
) {
    public BiDiDiscoveryConfig {
        maxBufferedEvents = Math.max(100, maxBufferedEvents);
    }

    public static BiDiDiscoveryConfig disabled() {
        return new BiDiDiscoveryConfig(false, true, true, false, 2_000);
    }
}
