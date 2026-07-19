package ua.demo.agentlab.ui.discovery.component.strategy;

import ua.demo.agentlab.ui.discovery.component.model.ComponentType;

import java.util.List;

/** Detects semantic landmarks not dependent on framework-specific CSS classes. */
public final class AriaLandmarkComponentDetector implements ComponentDetectionStrategy {
    private final ComponentEvidenceMatcher matcher = new ComponentEvidenceMatcher();

    @Override
    public List<ComponentCandidate> detect(ComponentDetectionContext context) {
        List<String> ids = matcher.ids(context, "role=navigation", "role='navigation'", "role=banner", "role='banner'");
        return ids.isEmpty() ? List.of() : List.of(new ComponentCandidate("aria-landmark", "LandmarkComponent",
                ComponentType.NAVIGATION, ids, 0.84d, List.of(), List.of("component:aria-landmark")));
    }
}
