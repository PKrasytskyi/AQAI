package ua.demo.agentlab.core.config;

import java.time.Duration;

public interface UiRuntimeConfig {

    String getBaseUrl();

    String getBrowser();

    boolean isHeadless();

    Duration getDefaultTimeout();
}
