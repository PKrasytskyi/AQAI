package ua.demo.agentlab.ui.discovery.runtime.bidi;

import org.openqa.selenium.WebDriver;

public class BiDiSessionManager {

    private final BiDiDiscoveryConfig config;
    private final BiDiEventBuffer eventBuffer;
    private final SeleniumBiDiSessionAdapter sessionAdapter;

    public BiDiSessionManager(BiDiDiscoveryConfig config) {
        this(config, null);
    }

    public BiDiSessionManager(BiDiDiscoveryConfig config, BiDiEventBuffer eventBuffer) {
        this.config = config == null ? BiDiDiscoveryConfig.disabled() : config;
        this.eventBuffer = eventBuffer == null
                ? new BiDiEventBuffer(this.config.maxBufferedEvents())
                : eventBuffer;
        this.sessionAdapter = new SeleniumBiDiSessionAdapter(this.eventBuffer);
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    public BiDiEventBuffer eventBuffer() {
        return eventBuffer;
    }

    public void start(Object driver) {
        start(driver, "", "");
    }

    public void start(Object driver, String pageId, String pageUrl) {
        if (!config.enabled()) {
            return;
        }
        if (driver instanceof WebDriver webDriver) {
            sessionAdapter.start(webDriver, pageId, pageUrl);
        }
    }

    public void drain(Object driver) {
        if (!config.enabled()) {
            return;
        }
        if (driver instanceof WebDriver webDriver) {
            sessionAdapter.drain(webDriver);
        }
    }

    public void stop(Object driver) {
        if (!config.enabled()) {
            return;
        }
        if (driver instanceof WebDriver webDriver) {
            sessionAdapter.stop(webDriver);
        }
    }

    public void stop() {
        // Keep collected events available for RuntimeEvidenceCollector.
    }
}
