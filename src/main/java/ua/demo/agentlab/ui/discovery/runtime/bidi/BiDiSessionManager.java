package ua.demo.agentlab.ui.discovery.runtime.bidi;

public class BiDiSessionManager {

    private final BiDiDiscoveryConfig config;
    private final BiDiEventBuffer eventBuffer;

    public BiDiSessionManager(BiDiDiscoveryConfig config) {
        this.config = config == null ? BiDiDiscoveryConfig.disabled() : config;
        this.eventBuffer = new BiDiEventBuffer(this.config.maxBufferedEvents());
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public BiDiEventBuffer eventBuffer() {
        return eventBuffer;
    }

    public void start(Object driver) {
        if (!config.enabled()) {
            return;
        }
        // Placeholder boundary for Selenium WebDriver BiDi/CDP integration.
        // The rest of the pipeline consumes BiDiRuntimeEvent from eventBuffer().
    }

    public void stop() {
        eventBuffer.clear();
    }
}
