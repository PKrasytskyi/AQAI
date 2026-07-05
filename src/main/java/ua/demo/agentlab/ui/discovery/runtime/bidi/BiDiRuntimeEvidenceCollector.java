package ua.demo.agentlab.ui.discovery.runtime.bidi;

import ua.demo.agentlab.ui.discovery.runtime.NetworkSemanticEnricher;
import ua.demo.agentlab.ui.discovery.runtime.RuntimeEvidenceCollector;
import ua.demo.agentlab.ui.discovery.runtime.model.RuntimeEvidenceBundle;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserLogEntry;
import ua.demo.agentlab.ui.discovery.selenium.model.BrowserNetworkCall;
import ua.demo.agentlab.ui.discovery.selenium.model.DiscoveredPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.RawPageSnapshot;
import ua.demo.agentlab.ui.discovery.selenium.model.SeleniumDiscoveryResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BiDiRuntimeEvidenceCollector implements RuntimeEvidenceCollector {

    private final BiDiEventNormalizer normalizer;
    private final NetworkSemanticEnricher networkSemanticEnricher;
    private final BiDiEventBuffer eventBuffer;

    public BiDiRuntimeEvidenceCollector() {
        this(new BiDiEventNormalizer(), new NetworkSemanticEnricher(), null);
    }

    public BiDiRuntimeEvidenceCollector(BiDiEventBuffer eventBuffer) {
        this(new BiDiEventNormalizer(), new NetworkSemanticEnricher(), eventBuffer);
    }

    BiDiRuntimeEvidenceCollector(
            BiDiEventNormalizer normalizer,
            NetworkSemanticEnricher networkSemanticEnricher,
            BiDiEventBuffer eventBuffer
    ) {
        this.normalizer = normalizer == null ? new BiDiEventNormalizer() : normalizer;
        this.networkSemanticEnricher = networkSemanticEnricher == null
                ? new NetworkSemanticEnricher()
                : networkSemanticEnricher;
        this.eventBuffer = eventBuffer;
    }

    @Override
    public RuntimeEvidenceBundle collect(SeleniumDiscoveryResult seleniumDiscoveryResult) {
        List<BiDiRuntimeEvent> events = new ArrayList<>();
        if (eventBuffer != null) {
            events.addAll(eventBuffer.snapshot());
        }
        int bufferedEvents = events.size();
        List<BiDiRuntimeEvent> replayedEvents = replayFromSeleniumSnapshot(seleniumDiscoveryResult);
        events.addAll(replayedEvents);
        RuntimeEvidenceBundle normalized = normalizer.normalize(events);
        return new RuntimeEvidenceBundle(
                normalized.networkRequests(),
                normalized.networkResponses(),
                normalized.consoleLogs(),
                normalized.navigationEvents(),
                normalized.domMutations(),
                networkSemanticEnricher.enrich(normalized.networkResponses()),
                normalized.stateTransitions(),
                List.of(
                        "collector=bidi-runtime-evidence",
                        "mode=" + (eventBuffer == null ? "selenium-snapshot-adapter" : "bidi-buffer"),
                        "bufferedEvents=" + bufferedEvents,
                        "replayedSeleniumEvents=" + replayedEvents.size(),
                        "rawEvents=" + events.size(),
                        "networkResponses=" + normalized.networkResponses().size(),
                        "consoleLogs=" + normalized.consoleLogs().size()
                )
        );
    }

    private List<BiDiRuntimeEvent> replayFromSeleniumSnapshot(SeleniumDiscoveryResult seleniumDiscoveryResult) {
        if (seleniumDiscoveryResult == null || seleniumDiscoveryResult.pages().isEmpty()) {
            return List.of();
        }
        List<BiDiRuntimeEvent> events = new ArrayList<>();
        for (DiscoveredPageSnapshot page : seleniumDiscoveryResult.pages()) {
            RawPageSnapshot raw = page.rawPageSnapshot();
            if (raw == null) {
                continue;
            }
            long timestamp = parseLong(raw.timestamp());
            for (BrowserNetworkCall call : raw.networkCalls()) {
                Map<String, String> attributes = new LinkedHashMap<>();
                attributes.put("method", call.method());
                attributes.put("url", call.url());
                attributes.put("status", String.valueOf(call.status()));
                attributes.put("resourceType", call.resourceType());
                events.add(new BiDiRuntimeEvent(
                        "network.responseCompleted",
                        page.pageId(),
                        page.url(),
                        attributes,
                        timestamp
                ));
            }
            for (BrowserLogEntry log : raw.browserConsoleLogs()) {
                Map<String, String> attributes = new LinkedHashMap<>();
                attributes.put("level", log.level());
                attributes.put("message", log.message());
                events.add(new BiDiRuntimeEvent(
                        "log.entryAdded",
                        page.pageId(),
                        page.url(),
                        attributes,
                        log.timestamp()
                ));
            }
        }
        return events;
    }

    private long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? System.currentTimeMillis() : Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return System.currentTimeMillis();
        }
    }
}
