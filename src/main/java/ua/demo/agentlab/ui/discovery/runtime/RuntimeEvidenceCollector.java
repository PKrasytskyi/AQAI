package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

public interface RuntimeEvidenceCollector {

    RuntimeEvidenceBundle collect(SeleniumDiscoveryResult seleniumDiscoveryResult);
}
