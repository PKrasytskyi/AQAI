package ua.demo.agentlab.api.agent;

import ua.demo.agentlab.api.model.ApiEndpointBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public record ApiGenerationInput(
        ApiEndpointBundle seededEndpoints,
        SeleniumDiscoveryResult seleniumDiscoveryResult
) {
}
