package ua.demo.agentlab.ui.discovery.runtime;

import ua.demo.agentlab.ui.discovery.runtime.model.ConsoleLogEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NavigationEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkRequestEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.NetworkResponseEvent;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserLogEntry;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredTransition;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.ArrayList;
import java.util.List;

public class SeleniumLogRuntimeEvidenceCollector implements RuntimeEvidenceCollector {

    private final RuntimeEventNormalizer normalizer;
    private final NetworkSemanticEnricher networkSemanticEnricher;
    private final SpaStateTransitionDetector stateTransitionDetector;

    public SeleniumLogRuntimeEvidenceCollector() {
        this(new RuntimeEventNormalizer(), new NetworkSemanticEnricher(), new SpaStateTransitionDetector());
    }

    public SeleniumLogRuntimeEvidenceCollector(
            RuntimeEventNormalizer normalizer,
            NetworkSemanticEnricher networkSemanticEnricher,
            SpaStateTransitionDetector stateTransitionDetector
    ) {
        this.normalizer = normalizer == null ? new RuntimeEventNormalizer() : normalizer;
        this.networkSemanticEnricher = networkSemanticEnricher == null
                ? new NetworkSemanticEnricher()
                : networkSemanticEnricher;
        this.stateTransitionDetector = stateTransitionDetector == null
                ? new SpaStateTransitionDetector()
                : stateTransitionDetector;
    }

    @Override
    public RuntimeEvidenceBundle collect(SeleniumDiscoveryResult seleniumDiscoveryResult) {
        if (seleniumDiscoveryResult == null || seleniumDiscoveryResult.pages().isEmpty()) {
            return RuntimeEvidenceBundle.empty();
        }
        List<NetworkRequestEvent> requests = new ArrayList<>();
        List<NetworkResponseEvent> responses = new ArrayList<>();
        List<ConsoleLogEvent> consoleLogs = new ArrayList<>();
        List<NavigationEvent> navigationEvents = new ArrayList<>();

        int networkIndex = 1;
        int consoleIndex = 1;
        for (DiscoveredPageSnapshot page : seleniumDiscoveryResult.pages()) {
            RawPageSnapshot raw = page.rawPageSnapshot();
            if (raw == null) {
                continue;
            }
            String timestamp = raw.timestamp();
            for (BrowserNetworkCall call : raw.networkCalls()) {
                String eventId = "network-" + networkIndex++;
                requests.add(new NetworkRequestEvent(
                        eventId + "-request",
                        page.pageId(),
                        page.url(),
                        call.method(),
                        call.url(),
                        normalizer.path(call.url()),
                        call.resourceType(),
                        timestamp,
                        "selenium-performance-log"
                ));
                responses.add(new NetworkResponseEvent(
                        eventId + "-response",
                        page.pageId(),
                        page.url(),
                        call.method(),
                        call.url(),
                        normalizer.path(call.url()),
                        call.status(),
                        call.resourceType(),
                        timestamp,
                        "selenium-performance-log"
                ));
            }
            for (BrowserLogEntry log : raw.browserConsoleLogs()) {
                consoleLogs.add(new ConsoleLogEvent(
                        "console-" + consoleIndex++,
                        page.pageId(),
                        page.url(),
                        log.level(),
                        log.message(),
                        log.timestamp(),
                        "selenium-browser-log"
                ));
            }
        }

        int transitionIndex = 1;
        for (DiscoveredTransition transition : seleniumDiscoveryResult.transitions()) {
            navigationEvents.add(new NavigationEvent(
                    "navigation-" + transitionIndex++,
                    transition.fromPageId(),
                    "",
                    transition.toPageId(),
                    transition.toUrl(),
                    transition.actionType() + ":" + transition.actionLabel(),
                    transition.success(),
                    "selenium-transition"
            ));
        }

        return new RuntimeEvidenceBundle(
                requests,
                responses,
                consoleLogs,
                navigationEvents,
                List.of(),
                networkSemanticEnricher.enrich(responses),
                stateTransitionDetector.detect(navigationEvents),
                List.of(
                        "collector=selenium-log-runtime-evidence",
                        "pages=" + seleniumDiscoveryResult.pages().size(),
                        "networkResponses=" + responses.size(),
                        "consoleLogs=" + consoleLogs.size(),
                        "navigationEvents=" + navigationEvents.size()
                )
        );
    }
}
