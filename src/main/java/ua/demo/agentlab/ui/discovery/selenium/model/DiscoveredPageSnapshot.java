package ua.demo.agentlab.ui.discovery.selenium.model;

import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.LocatorHint;

import java.util.List;

public record DiscoveredPageSnapshot(
        String pageId,
        String url,
        String title,
        List<String> headings,
        List<DiscoveredInteractiveElement> links,
        List<DiscoveredInteractiveElement> buttons,
        List<DiscoveredForm> forms,
        boolean modalVisible,
        boolean authenticatedArea,
        List<String> capabilities,
        List<LocatorHint> locatorHints,
        String fingerprint,
        DiscoveredPageEvidence evidence,
        RawPageSnapshot rawPageSnapshot,
        List<RawElement> rawElements
) {
    public DiscoveredPageSnapshot(
            String pageId,
            String url,
            String title,
            List<String> headings,
            List<DiscoveredInteractiveElement> links,
            List<DiscoveredInteractiveElement> buttons,
            List<DiscoveredForm> forms,
            boolean modalVisible,
            boolean authenticatedArea,
            List<String> capabilities,
            List<LocatorHint> locatorHints,
            String fingerprint,
            DiscoveredPageEvidence evidence
    ) {
        this(
                pageId,
                url,
                title,
                headings,
                links,
                buttons,
                forms,
                modalVisible,
                authenticatedArea,
                capabilities,
                locatorHints,
                fingerprint,
                evidence,
                null,
                List.of()
        );
    }

    public DiscoveredPageSnapshot {
        headings = headings == null ? List.of() : List.copyOf(headings);
        links = links == null ? List.of() : List.copyOf(links);
        buttons = buttons == null ? List.of() : List.copyOf(buttons);
        forms = forms == null ? List.of() : List.copyOf(forms);
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        locatorHints = locatorHints == null ? List.of() : List.copyOf(locatorHints);
        rawElements = rawElements == null ? List.of() : List.copyOf(rawElements);
    }
}
