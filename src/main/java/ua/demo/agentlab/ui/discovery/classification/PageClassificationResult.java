package ua.demo.agentlab.ui.discovery.classification;

import ua.demo.agentlab.ui.discovery.identity.CanonicalPageType;
import ua.demo.agentlab.ui.discovery.identity.PageIdentity;

import java.util.List;

public record PageClassificationResult(
        String pageName,
        List<String> inferredCapabilities,
        String classificationReason,
        CanonicalPageType canonicalPageType,
        PageIdentity pageIdentity
) {

    public PageClassificationResult(
            String pageName,
            List<String> inferredCapabilities,
            String classificationReason
    ) {
        this(
                pageName,
                inferredCapabilities,
                classificationReason,
                CanonicalPageType.fromLegacyPageName(pageName),
                PageIdentity.legacy(pageName, CanonicalPageType.fromLegacyPageName(pageName).mappedType(), "")
        );
    }
}
