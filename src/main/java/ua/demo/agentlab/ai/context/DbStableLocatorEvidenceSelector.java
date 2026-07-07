package ua.demo.agentlab.ai.context;

import ua.demo.agentlab.ui.discovery.evidence.LocatorEvidenceType;
import ua.demo.agentlab.ui.discovery.identity.RouteCanonicalizer;
import ua.demo.agentlab.ui.discovery.mapping.model.MappedPage;

import java.util.List;

public class DbStableLocatorEvidenceSelector {

    private final PageOwnershipSlicer ownershipSlicer;

    public DbStableLocatorEvidenceSelector() {
        this(new PageOwnershipSlicer());
    }

    public DbStableLocatorEvidenceSelector(PageOwnershipSlicer ownershipSlicer) {
        this.ownershipSlicer = ownershipSlicer == null ? new PageOwnershipSlicer() : ownershipSlicer;
    }

    public List<PromptLocatorEvidence> select(AiContextPackage context, MappedPage targetPage) {
        if (context == null || context.dbStableLocatorEvidence().isEmpty() || targetPage == null) {
            return List.of();
        }
        return context.dbStableLocatorEvidence().stream()
                .filter(locator -> locator.evidenceType() == LocatorEvidenceType.CONFIRMED_LOCATOR)
                .filter(locator -> matchesTarget(locator, targetPage))
                .toList();
    }

    private boolean matchesTarget(PromptLocatorEvidence locator, MappedPage targetPage) {
        String targetPageId = targetPage.pageId();
        String targetRoute = ownershipSlicer.route(targetPage);
        for (String trace : locator.sourceTrace()) {
            if (trace.equalsIgnoreCase("db-page-id:" + targetPageId)
                    || trace.equalsIgnoreCase("db-route:" + targetRoute)
                    || RouteCanonicalizer.routeEqualsOrSuffix(trace.replace("db-route:", ""), targetRoute)) {
                return true;
            }
        }
        return false;
    }
}
