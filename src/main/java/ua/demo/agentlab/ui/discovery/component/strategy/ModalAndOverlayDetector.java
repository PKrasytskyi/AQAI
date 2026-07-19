package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;

public final class ModalAndOverlayDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();
    @Override public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<String> ids = matcher.ids(context, "modal", "dialog", "role=dialog", "role='dialog'", "popup", "overlay");
        return ids.isEmpty() ? List.of() : List.of(new ComponentCandidate("modal", "ModalComponent",
                ComponentType.MODAL, ids, 0.76d, List.of(), List.of("component:modal-evidence")));
    }
}
