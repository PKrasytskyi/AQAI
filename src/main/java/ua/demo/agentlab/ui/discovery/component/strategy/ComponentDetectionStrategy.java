package ua.demo.agentlab.ui.discovery.component.strategy;

import java.util.List;

public interface ComponentDetectionStrategy {
    List<ComponentCandidate> detect(ComponentDetectionContext context);
}
