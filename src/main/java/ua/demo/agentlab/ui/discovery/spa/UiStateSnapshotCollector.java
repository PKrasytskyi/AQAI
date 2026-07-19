package ua.demo.agentlab.ui.discovery.spa;

import org.openqa.selenium.WebDriver;
import ua.demo.agentlab.config.ProjectProfile;
import ua.demo.agentlab.ui.discovery.interaction.inventory.UiInteractionPage;
import ua.demo.agentlab.ui.discovery.spa.model.UiStateSnapshot;

import java.util.List;
import java.util.Objects;

/** Captures and de-duplicates state snapshots without deciding navigation. */
public final class UiStateSnapshotCollector {

    private final SpaStateSnapshotCaptureService captureService;

    public UiStateSnapshotCollector(SpaStateSnapshotCaptureService captureService) {
        this.captureService = Objects.requireNonNull(captureService, "captureService");
    }

    public UiStateSnapshot capture(WebDriver driver, ProjectProfile profile, UiInteractionPage page) {
        return captureService.capture(driver, profile, page);
    }

    public UiStateSnapshot capture(WebDriver driver, ProjectProfile profile, String pageId,
                                   ua.demo.agentlab.ui.discovery.persistence.knowledge.KnowledgeRunMetadata metadata) {
        return captureService.capture(driver, profile, pageId, metadata);
    }

    public void addDistinct(List<UiStateSnapshot> states, UiStateSnapshot state) {
        if (state == null) return;
        if (states.stream().noneMatch(existing -> existing.stateId().equals(state.stateId()))) {
            states.add(state);
        }
    }
}
