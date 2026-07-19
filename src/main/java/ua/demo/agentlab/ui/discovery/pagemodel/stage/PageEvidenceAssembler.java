package ua.demo.agentlab.ui.discovery.pagemodel.stage;

import ua.demo.agentlab.ui.discovery.evidence.model.DiscoveredPageEvidence;
import ua.demo.agentlab.ui.discovery.pagemodel.model.PageEvidenceModel;

/** Keeps evidence path mapping separate from page semantics. */
public final class PageEvidenceAssembler {

    public PageEvidenceModel assemble(DiscoveredPageEvidence evidence) {
        return evidence == null ? new PageEvidenceModel("", "")
                : new PageEvidenceModel(evidence.screenshotPath(), evidence.htmlPath());
    }
}
